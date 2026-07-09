package com.ganathan.skyesabove.data.repository

import com.ganathan.skyesabove.data.api.MetEireannApi
import com.ganathan.skyesabove.data.api.MetEireannObservationsApi
import com.ganathan.skyesabove.data.api.OpenMeteoApi
import com.ganathan.skyesabove.data.api.OpenWeatherMapApi
import com.ganathan.skyesabove.data.api.SunriseSunsetApi
import com.ganathan.skyesabove.data.api.XmlParser
import com.ganathan.skyesabove.data.model.CurrentObservation
import com.ganathan.skyesabove.data.model.DailyForecast
import com.ganathan.skyesabove.data.model.OpenMeteoResponse
import com.ganathan.skyesabove.data.model.OpenWeatherMapResponse
import com.ganathan.skyesabove.data.model.SunData
import com.ganathan.skyesabove.data.model.WeatherData
import com.ganathan.skyesabove.data.model.WeatherResponse
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching weather and sun data from multiple external APIs.
 * Combines data from Met Éireann, Open-Meteo, and OpenWeatherMap for best accuracy through averaging.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val metEireannApi: MetEireannApi,
    private val metEireannObservationsApi: MetEireannObservationsApi,
    private val openMeteoApi: OpenMeteoApi,
    private val openWeatherMapApi: OpenWeatherMapApi,
    private val sunriseSunsetApi: SunriseSunsetApi,
    private val settingsDataStore: SettingsDataStore
) {

    /**
     * Fetch weather data for the specified coordinates.
     * Combines data from multiple sources (Met Éireann + Open-Meteo + OpenWeatherMap) for best accuracy.
     *
     * @param lat Latitude in decimal degrees
     * @param long Longitude in decimal degrees
     * @return Result containing WeatherResponse or error
     */
    suspend fun getWeather(lat: Double, long: Double): Result<WeatherResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Get OWM API key from settings
                val owmApiKey = settingsDataStore.openWeatherMapApiKey.first()

                // Met Éireann forecast + the Dublin observations are Ireland-only; gate them by
                // location so the app/widget report correctly anywhere in the world. The global
                // sources (Open-Meteo always, OpenWeatherMap if keyed) cover everywhere else.
                val inIreland = lat in 51.2..55.6 && long in -11.0..-5.0

                // Fetch all sources in parallel
                val metEireannDeferred = async {
                    if (inIreland) fetchMetEireannForecast(lat, long)
                    else Result.failure(Exception("Met Éireann skipped: outside Ireland"))
                }
                val openMeteoDeferred = async { fetchOpenMeteoForecast(lat, long) }
                val openWeatherMapDeferred = async {
                    if (owmApiKey.isNotEmpty()) {
                        fetchOpenWeatherMapForecast(lat, long, owmApiKey)
                    } else {
                        Result.failure(Exception("No OpenWeatherMap API key configured"))
                    }
                }
                val observationsDeferred = async {
                    if (inIreland) fetchObservations("dublin")
                    else Result.failure(Exception("Observations skipped: outside Ireland"))
                }

                val metEireannResult = metEireannDeferred.await()
                val openMeteoResult = openMeteoDeferred.await()
                val openWeatherMapResult = openWeatherMapDeferred.await()
                val observationsResult = observationsDeferred.await()

                // Merge forecasts from all sources
                val mergedForecast = mergeAllForecasts(
                    metEireannResult.getOrNull(),
                    openMeteoResult.getOrNull(),
                    openWeatherMapResult.getOrNull()
                )

                if (mergedForecast != null) {
                    // Enhance current with real-time observations
                    val enhancedCurrent = observationsResult.getOrNull()?.let { obs ->
                        mergeCurrentWeather(mergedForecast.current, obs)
                    } ?: mergedForecast.current

                    Result.success(WeatherResponse(enhancedCurrent, mergedForecast.hourly))
                } else {
                    // Every source failed. Carry WHY per source (e.g. "UnknownHostException" from a
                    // dead-Wi-Fi DNS, or a rate-limit HTTP code) so the widget's diagnostics log
                    // explains the outage without needing logcat. Open-Meteo is the always-on
                    // global source, so lead with it.
                    fun reason(r: Result<*>): String = r.exceptionOrNull()
                        ?.let { it.message?.take(80) ?: it.javaClass.simpleName } ?: "?"
                    Result.failure(
                        Exception(
                            "all forecast sources failed — " +
                                "openMeteo: ${reason(openMeteoResult)}; " +
                                "metEireann: ${reason(metEireannResult)}; " +
                                "owm: ${reason(openWeatherMapResult)}"
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Merge forecasts from all three sources.
     * Averages numeric values when multiple sources have data.
     */
    private fun mergeAllForecasts(
        metEireann: WeatherResponse?,
        openMeteo: WeatherResponse?,
        openWeatherMap: WeatherResponse?
    ): WeatherResponse? {
        val sources = listOfNotNull(metEireann, openMeteo, openWeatherMap)
        if (sources.isEmpty()) return null
        if (sources.size == 1) return sources.first()

        // Merge current weather from all available sources
        val mergedCurrent = WeatherData(
            time = sources.first().current.time,
            // Temperature/pressure/humidity: a source that lacks the field reports 0.0/0 as a
            // "missing" sentinel (same convention as mergeCurrentWeather). Averaging that in
            // halves the real value (e.g. (24+0)/2=12), so drop zeros when a real value exists.
            temperature = averageMeaningful(sources.map { it.current.temperature }),
            windSpeed = averageMultiple(sources.map { it.current.windSpeed }),
            windDirection = averageMultipleInt(sources.map { it.current.windDirection }),
            humidity = averageMeaningfulInt(sources.map { it.current.humidity }),
            cloudCover = averageMultipleInt(sources.map { it.current.cloudCover }),
            pressure = averageMeaningful(sources.map { it.current.pressure }),
            precipitation = averageMultiple(sources.map { it.current.precipitation }),
            symbol = metEireann?.current?.symbol ?: openMeteo?.current?.symbol ?: openWeatherMap?.current?.symbol ?: "",
            description = metEireann?.current?.description?.ifEmpty { null }
                ?: openMeteo?.current?.description?.ifEmpty { null }
                ?: openWeatherMap?.current?.description ?: ""
        )

        // Build maps for hourly data from each source
        val hourlyMaps = sources.map { source ->
            source.hourly.associateBy { it.time.substringBefore(":00") }
        }

        // Get all unique time keys
        val allTimes = hourlyMaps.flatMap { it.keys }.distinct().sorted()

        // Merge hourly forecasts
        val mergedHourly = allTimes.take(168).mapNotNull { timeKey -> // 7 days * 24 hours
            val dataPoints = hourlyMaps.mapNotNull { it[timeKey] }
            if (dataPoints.isEmpty()) return@mapNotNull null

            WeatherData(
                time = dataPoints.first().time,
                // Drop 0.0/0 "missing" sentinels so one source's gap can't halve the reading
                // (this is what produced a phantom "Today 12°" low). Precip/cloud/wind-dir keep
                // 0 — there it's a real value, not a sentinel.
                temperature = averageMeaningful(dataPoints.map { it.temperature }),
                windSpeed = averageMultiple(dataPoints.map { it.windSpeed }),
                windDirection = averageMultipleInt(dataPoints.map { it.windDirection }),
                humidity = averageMeaningfulInt(dataPoints.map { it.humidity }),
                cloudCover = averageMultipleInt(dataPoints.map { it.cloudCover }),
                pressure = averageMeaningful(dataPoints.map { it.pressure }),
                precipitation = averageMultiple(dataPoints.map { it.precipitation }),
                symbol = dataPoints.firstOrNull { it.symbol.isNotEmpty() }?.symbol ?: "",
                description = dataPoints.firstOrNull { it.description.isNotEmpty() }?.description ?: ""
            )
        }

        return WeatherResponse(mergedCurrent, mergedHourly)
    }

    private fun averageMultiple(values: List<Double>): Double {
        return if (values.isEmpty()) 0.0 else values.sum() / values.size
    }

    private fun averageMultipleInt(values: List<Int>): Int {
        return if (values.isEmpty()) 0 else values.sum() / values.size
    }

    /**
     * Average, treating 0.0 as a "missing" sentinel: average only the non-zero values when any
     * exist (so a source without this field can't drag the mean down). Falls back to 0.0 only if
     * every source is missing it. Use for temperature/pressure/humidity — NOT precipitation,
     * cloud cover or wind direction, where 0 is a genuine reading.
     */
    private fun averageMeaningful(values: List<Double>): Double {
        val real = values.filter { it != 0.0 }
        return when {
            real.isNotEmpty() -> real.sum() / real.size
            else -> 0.0
        }
    }

    private fun averageMeaningfulInt(values: List<Int>): Int {
        val real = values.filter { it != 0 }
        return if (real.isNotEmpty()) real.sum() / real.size else 0
    }

    /**
     * Fetch forecast data from Met Éireann WDB API.
     */
    private suspend fun fetchMetEireannForecast(lat: Double, long: Double): Result<WeatherResponse> {
        return try {
            val response = metEireannApi.getLocationForecast(lat, long)

            if (response.isSuccessful) {
                val xmlString = response.body()?.string()
                if (xmlString != null) {
                    val rawWeatherData = XmlParser.parseWeatherXml(xmlString)
                    val mergedWeatherData = XmlParser.mergeWeatherData(rawWeatherData)

                    if (mergedWeatherData.isNotEmpty()) {
                        val current = mergedWeatherData.first()
                        val hourly = mergedWeatherData.take(168) // 7 days
                        Result.success(WeatherResponse(current, hourly))
                    } else {
                        Result.failure(Exception("No Met Éireann data available"))
                    }
                } else {
                    Result.failure(Exception("Empty Met Éireann response"))
                }
            } else {
                Result.failure(Exception("Met Éireann API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch forecast data from Open-Meteo API.
     */
    private suspend fun fetchOpenMeteoForecast(lat: Double, long: Double): Result<WeatherResponse> {
        return try {
            val response = openMeteoApi.getForecast(lat, long)

            if (response.isSuccessful) {
                val openMeteo = response.body()
                if (openMeteo != null) {
                    val weatherResponse = convertOpenMeteoToWeatherResponse(openMeteo)
                    if (weatherResponse != null) {
                        Result.success(weatherResponse)
                    } else {
                        Result.failure(Exception("Failed to parse Open-Meteo data"))
                    }
                } else {
                    Result.failure(Exception("Empty Open-Meteo response"))
                }
            } else {
                Result.failure(Exception("Open-Meteo API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch forecast data from OpenWeatherMap API.
     */
    private suspend fun fetchOpenWeatherMapForecast(
        lat: Double,
        long: Double,
        apiKey: String
    ): Result<WeatherResponse> {
        return try {
            val response = openWeatherMapApi.getForecast(lat, long, apiKey)

            if (response.isSuccessful) {
                val owmResponse = response.body()
                if (owmResponse != null) {
                    val weatherResponse = convertOpenWeatherMapToWeatherResponse(owmResponse)
                    if (weatherResponse != null) {
                        Result.success(weatherResponse)
                    } else {
                        Result.failure(Exception("Failed to parse OpenWeatherMap data"))
                    }
                } else {
                    Result.failure(Exception("Empty OpenWeatherMap response"))
                }
            } else {
                Result.failure(Exception("OpenWeatherMap API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert Open-Meteo response to our WeatherResponse format.
     */
    private fun convertOpenMeteoToWeatherResponse(openMeteo: OpenMeteoResponse): WeatherResponse? {
        val hourly = openMeteo.hourly ?: return null
        val times = hourly.time ?: return null

        val hourlyData = times.mapIndexedNotNull { index, time ->
            WeatherData(
                time = time,
                temperature = hourly.temperature?.getOrNull(index) ?: 0.0,
                windSpeed = hourly.windSpeed?.getOrNull(index) ?: 0.0,
                windDirection = hourly.windDirection?.getOrNull(index) ?: 0,
                humidity = hourly.humidity?.getOrNull(index) ?: 0,
                cloudCover = hourly.cloudCover?.getOrNull(index) ?: 0,
                pressure = hourly.pressure?.getOrNull(index) ?: 0.0,
                precipitation = hourly.precipitation?.getOrNull(index) ?: 0.0,
                symbol = weatherCodeToSymbol(hourly.weatherCode?.getOrNull(index) ?: 0),
                description = weatherCodeToDescription(hourly.weatherCode?.getOrNull(index) ?: 0)
            )
        }

        if (hourlyData.isEmpty()) return null

        val current = openMeteo.current?.let { curr ->
            WeatherData(
                time = curr.time ?: hourlyData.first().time,
                temperature = curr.temperature ?: hourlyData.first().temperature,
                windSpeed = curr.windSpeed ?: hourlyData.first().windSpeed,
                windDirection = curr.windDirection ?: hourlyData.first().windDirection,
                humidity = curr.humidity ?: hourlyData.first().humidity,
                cloudCover = curr.cloudCover ?: hourlyData.first().cloudCover,
                pressure = curr.pressure ?: hourlyData.first().pressure,
                precipitation = curr.precipitation ?: hourlyData.first().precipitation,
                symbol = weatherCodeToSymbol(curr.weatherCode ?: 0),
                description = weatherCodeToDescription(curr.weatherCode ?: 0)
            )
        } ?: hourlyData.first()

        return WeatherResponse(current, hourlyData.take(168))
    }

    /**
     * Convert OpenWeatherMap response to our WeatherResponse format.
     */
    private fun convertOpenWeatherMapToWeatherResponse(owm: OpenWeatherMapResponse): WeatherResponse? {
        val list = owm.list ?: return null
        if (list.isEmpty()) return null

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val zoneId = ZoneId.systemDefault()

        val hourlyData = list.mapNotNull { item ->
            val time = item.dt?.let {
                Instant.ofEpochSecond(it).atZone(zoneId).format(formatter)
            } ?: return@mapNotNull null

            WeatherData(
                time = time,
                temperature = item.main?.temp ?: 0.0,
                windSpeed = (item.wind?.speed ?: 0.0) * 3.6, // Convert m/s to km/h
                windDirection = item.wind?.deg ?: 0,
                humidity = item.main?.humidity ?: 0,
                cloudCover = item.clouds?.all ?: 0,
                pressure = (item.main?.pressure ?: 0).toDouble(),
                precipitation = (item.rain?.threeHour ?: 0.0) + (item.snow?.threeHour ?: 0.0),
                symbol = item.weather?.firstOrNull()?.icon ?: "",
                description = item.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
            )
        }

        if (hourlyData.isEmpty()) return null

        return WeatherResponse(hourlyData.first(), hourlyData)
    }

    /**
     * Convert WMO weather code to symbol code.
     */
    private fun weatherCodeToSymbol(code: Int): String {
        return when (code) {
            0 -> "01d"
            1, 2 -> "02d"
            3 -> "03d"
            45, 48 -> "15d"
            51, 53, 55 -> "40d"
            56, 57 -> "47d"
            61, 63, 65 -> "09d"
            66, 67 -> "12d"
            71, 73, 75 -> "13d"
            77 -> "49d"
            80, 81, 82 -> "05d"
            85, 86 -> "08d"
            95 -> "22d"
            96, 99 -> "25d"
            else -> "03d"
        }
    }

    /**
     * Convert WMO weather code to description.
     */
    private fun weatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing rime fog"
            51 -> "Light drizzle"
            53 -> "Moderate drizzle"
            55 -> "Dense drizzle"
            56 -> "Light freezing drizzle"
            57 -> "Dense freezing drizzle"
            61 -> "Slight rain"
            63 -> "Moderate rain"
            65 -> "Heavy rain"
            66 -> "Light freezing rain"
            67 -> "Heavy freezing rain"
            71 -> "Slight snow"
            73 -> "Moderate snow"
            75 -> "Heavy snow"
            77 -> "Snow grains"
            80 -> "Slight rain showers"
            81 -> "Moderate rain showers"
            82 -> "Violent rain showers"
            85 -> "Slight snow showers"
            86 -> "Heavy snow showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with slight hail"
            99 -> "Thunderstorm with heavy hail"
            else -> "Unknown"
        }
    }

    /**
     * Fetch current observations from Met Éireann.
     */
    private suspend fun fetchObservations(station: String): Result<CurrentObservation> {
        return try {
            val response = metEireannObservationsApi.getObservations(station)

            if (response.isSuccessful) {
                val observationsList = response.body()
                val obsResponse = observationsList?.lastOrNull()
                if (obsResponse != null) {
                    Result.success(
                        CurrentObservation(
                            temperature = obsResponse.temperature?.toDoubleOrNull() ?: 0.0,
                            humidity = obsResponse.humidity?.trim()?.toIntOrNull() ?: 0,
                            windSpeed = obsResponse.windSpeed?.toDoubleOrNull() ?: 0.0,
                            windGust = obsResponse.windGust?.toDoubleOrNull(),
                            windDirection = obsResponse.windDirection ?: 0,
                            pressure = obsResponse.pressure?.toDoubleOrNull() ?: 0.0,
                            rainfall = obsResponse.rainfall?.trim()?.toDoubleOrNull() ?: 0.0,
                            condition = obsResponse.symbol ?: "",
                            description = obsResponse.weatherDescription ?: ""
                        )
                    )
                } else {
                    Result.failure(Exception("No observations available"))
                }
            } else {
                Result.failure(Exception("Observations API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Merge forecast current with real observations for better accuracy.
     */
    private fun mergeCurrentWeather(forecast: WeatherData, obs: CurrentObservation): WeatherData {
        return forecast.copy(
            temperature = if (obs.temperature != 0.0) obs.temperature else forecast.temperature,
            windSpeed = if (obs.windSpeed != 0.0) obs.windSpeed else forecast.windSpeed,
            windDirection = if (obs.windDirection != 0) obs.windDirection else forecast.windDirection,
            humidity = if (obs.humidity != 0) obs.humidity else forecast.humidity,
            pressure = if (obs.pressure != 0.0) obs.pressure else forecast.pressure,
            precipitation = if (obs.rainfall != 0.0) obs.rainfall else forecast.precipitation,
            description = obs.description.ifEmpty { forecast.description }
        )
    }

    /**
     * Fetch sun timing data for the specified coordinates.
     */
    suspend fun getSunTimes(lat: Double, long: Double): Result<SunData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = sunriseSunsetApi.getSunTimes(lat, long, formatted = 0)

                if (response.isSuccessful) {
                    val sunResponse = response.body()
                    if (sunResponse != null && sunResponse.status == "OK") {
                        val results = sunResponse.results
                        val dayLengthHours = results.dayLength / 3600.0

                        val sunData = SunData(
                            sunrise = formatTime(results.sunrise),
                            sunset = formatTime(results.sunset),
                            civilTwilightEnd = formatTime(results.civilTwilightEnd),
                            dayLengthHours = dayLengthHours
                        )
                        // Local-zone times for display. Logged so the fix can be verified on a
                        // real device (UTC API sunset -> local): raw "${results.sunset}".
                        Log.i(
                            "SkyesSun",
                            "sun times (${ZoneId.systemDefault()}): sunrise=${sunData.sunrise} " +
                                "sunset=${sunData.sunset} civilTwilightEnd=${sunData.civilTwilightEnd} " +
                                "[raw sunset=${results.sunset}]"
                        )
                        Result.success(sunData)
                    } else {
                        Result.failure(Exception("Invalid sun data response"))
                    }
                } else {
                    Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Group hourly weather data into daily forecasts.
     */
    fun getDailyForecasts(hourlyData: List<WeatherData>): List<DailyForecast> {
        return hourlyData
            .groupBy { it.time.substringBefore("T") }
            .map { (date, dayData) ->
                val temps = dayData.map { it.temperature }
                val totalPrecip = dayData.sumOf { it.precipitation }
                val middayData = dayData.find { it.time.contains("T12:") }
                    ?: dayData[dayData.size / 2]

                DailyForecast(
                    date = date,
                    highTemp = temps.maxOrNull() ?: 0.0,
                    lowTemp = temps.minOrNull() ?: 0.0,
                    symbol = middayData.symbol,
                    precipitation = totalPrecip
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Convert wind speed from m/s to km/h.
     */
    fun convertWindSpeed(metersPerSecond: Double): Double {
        return metersPerSecond * 3.6
    }

    /**
     * Format an ISO-8601 sun time to a local-zone "HH:mm" for display.
     *
     * Delegates to the pure [isoToLocalHhMm] using the device's zone. See that function for why
     * the old string-chopping approach showed sunset an hour early during Irish Summer Time.
     */
    private fun formatTime(isoTime: String): String = isoToLocalHhMm(isoTime, ZoneId.systemDefault())
}

/**
 * Convert an ISO-8601 timestamp that carries an explicit offset (what sunrise-sunset.org returns
 * with formatted=0, e.g. "2026-07-09T20:52:45+00:00" in UTC) to a local-zone "HH:mm" string.
 *
 * The previous implementation string-chopped the clock field out of the ISO text
 * (`substringAfter("T").substringBefore("+")`) and displayed it verbatim, so it printed the UTC
 * clock time and ignored the offset entirely. During Irish Summer Time (UTC+1) that showed sunset
 * as 20:52 instead of the correct 21:52 — the ~1-hour discrepancy against SkyeJS, which hands the
 * same ISO string to a date library that localises it. We parse the absolute instant from the
 * offset and render it in [zone], so the displayed clock time matches the wall clock.
 *
 * Pure and Android-free so it can be unit-tested against a fixed zone (see SunTimeFormatTest).
 * Falls back to the raw input if the string doesn't parse, matching the old fail-safe behaviour.
 */
internal fun isoToLocalHhMm(isoTime: String, zone: ZoneId): String = try {
    OffsetDateTime.parse(isoTime).atZoneSameInstant(zone).format(HHMM_FORMAT)
} catch (e: Exception) {
    isoTime
}

private val HHMM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
