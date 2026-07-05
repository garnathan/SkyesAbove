package com.ganathan.skyesabove.ui.screens.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganathan.skyesabove.data.model.WeatherResponse
import com.ganathan.skyesabove.data.model.SunData as ModelSunData
import com.ganathan.skyesabove.data.repository.WeatherRepository
import com.ganathan.skyesabove.ui.screens.weather.components.DailyForecast
import com.ganathan.skyesabove.ui.screens.weather.components.HourlyForecast
import com.ganathan.skyesabove.ui.util.FeelsLikeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * UI state for current weather conditions
 */
data class CurrentWeatherState(
    val temperature: Double = 0.0,
    val feelsLikeTemperature: Double = 0.0,
    val weatherSymbol: String? = null,
    val weatherDescription: String? = null,
    val windSpeed: Double = 0.0,
    val windDirection: Double? = null,
    val humidity: Double = 0.0,
    val cloudCover: Double = 0.0,
    val pressure: Double? = null,
    val visibility: Double? = null,
    val uvIndex: Double? = null
)

/**
 * UI state for sun/twilight times
 */
data class SunTimesState(
    val sunrise: LocalTime? = null,
    val sunset: LocalTime? = null,
    val civilTwilightStart: LocalTime? = null,
    val civilTwilightEnd: LocalTime? = null,
    val nauticalTwilightStart: LocalTime? = null,
    val nauticalTwilightEnd: LocalTime? = null,
    val astronomicalTwilightStart: LocalTime? = null,
    val astronomicalTwilightEnd: LocalTime? = null
)

/**
 * Combined UI state for the weather screen
 */
data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentWeather: CurrentWeatherState = CurrentWeatherState(),
    val sunTimes: SunTimesState = SunTimesState(),
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList(),
    val lastUpdated: LocalDateTime? = null,
    val locationName: String = "Loading..."
)

/**
 * ViewModel for the Weather Screen.
 *
 * Manages weather data fetching, sun times, and auto-refresh functionality.
 * Uses Hilt for dependency injection.
 */
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    // Default location (can be overridden by settings)
    private var latitude: Double = 53.3498  // Dublin, Ireland
    private var longitude: Double = -6.2603

    // Auto-refresh interval: 10 minutes
    private val autoRefreshIntervalMs = 10 * 60 * 1000L

    init {
        fetchWeatherData()
        startAutoRefresh()
    }

    /**
     * Fetches all weather data including current conditions, hourly and daily forecasts.
     */
    fun fetchWeatherData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Fetch weather data from repository
                val weatherResult = weatherRepository.getWeather(latitude, longitude)

                weatherResult.fold(
                    onSuccess = { weatherResponse ->
                        val currentWeather = parseCurrentWeather(weatherResponse)
                        val hourlyForecast = parseHourlyForecast(weatherResponse)
                        val dailyForecast = parseDailyForecast(weatherResponse)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                currentWeather = currentWeather,
                                hourlyForecast = hourlyForecast,
                                dailyForecast = dailyForecast,
                                lastUpdated = LocalDateTime.now(),
                                locationName = "Dublin, Ireland"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = exception.message ?: "Failed to fetch weather data"
                            )
                        }
                    }
                )

                // Fetch sun times separately
                fetchSunTimes()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    /**
     * Fetches sunrise, sunset, and twilight times.
     */
    fun fetchSunTimes() {
        viewModelScope.launch {
            try {
                val sunResult = weatherRepository.getSunTimes(latitude, longitude)

                sunResult.fold(
                    onSuccess = { sunData ->
                        _uiState.update {
                            it.copy(
                                sunTimes = SunTimesState(
                                    sunrise = parseTimeString(sunData.sunrise),
                                    sunset = parseTimeString(sunData.sunset),
                                    civilTwilightEnd = parseTimeString(sunData.civilTwilightEnd)
                                )
                            )
                        }
                    },
                    onFailure = { /* Sun times are optional, don't fail the whole screen */ }
                )
            } catch (e: Exception) {
                // Sun times are supplementary, don't propagate error
            }
        }
    }

    /**
     * Triggers a manual refresh of weather data.
     * Shows refresh indicator instead of loading spinner.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetchWeatherData()
        }
    }

    /**
     * Updates the location for weather data.
     */
    fun updateLocation(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
        fetchWeatherData()
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Starts auto-refresh timer to fetch new data every 10 minutes.
     */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(autoRefreshIntervalMs)
                if (!_uiState.value.isLoading && !_uiState.value.isRefreshing) {
                    fetchWeatherData()
                }
            }
        }
    }

    /**
     * Parses current weather from repository response.
     */
    private fun parseCurrentWeather(weatherResponse: WeatherResponse): CurrentWeatherState {
        val current = weatherResponse.current
        val temp = current.temperature
        val wind = current.windSpeed
        val humidity = current.humidity.toDouble()

        val feelsLike = FeelsLikeCalculator.calculateFeelsLike(
            temperatureCelsius = temp,
            windSpeedMs = wind / 3.6, // Convert km/h to m/s for feels like calculation
            humidityPercent = humidity
        )

        return CurrentWeatherState(
            temperature = temp,
            feelsLikeTemperature = feelsLike,
            weatherSymbol = current.symbol,
            weatherDescription = current.description,
            windSpeed = wind,
            windDirection = current.windDirection.toDouble(),
            humidity = humidity,
            cloudCover = current.cloudCover.toDouble(),
            pressure = current.pressure
        )
    }

    /**
     * Parses hourly forecast from repository response.
     */
    private fun parseHourlyForecast(weatherResponse: WeatherResponse): List<HourlyForecast> {
        return weatherResponse.hourly.map { hourly ->
            HourlyForecast(
                time = parseDateTime(hourly.time),
                temperature = hourly.temperature,
                symbolCode = hourly.symbol,
                // Met Éireann provides precipitation amount in mm, not probability
                // Scale: 1mm = 10%, capped at 100%
                precipitationProbability = if (hourly.precipitation > 0.1) (hourly.precipitation / 10.0).coerceAtMost(1.0) else null,
                windSpeed = hourly.windSpeed,
                windDirection = hourly.windDirection.toDouble()
            )
        }
    }

    /**
     * Parses daily forecast from repository response.
     */
    private fun parseDailyForecast(weatherResponse: WeatherResponse): List<DailyForecast> {
        // Group hourly data by date
        return weatherResponse.hourly
            .groupBy { it.time.substringBefore("T") }
            .map { (date, dayData) ->
                val temps = dayData.map { it.temperature }
                val maxPrecip = dayData.maxOfOrNull { it.precipitation } ?: 0.0
                val middayData = dayData.find { it.time.contains("T12:") } ?: dayData[dayData.size / 2]
                val avgWindSpeed = dayData.map { it.windSpeed }.average()
                val avgWindDirection = dayData.map { it.windDirection }.average()

                DailyForecast(
                    date = parseDate(date),
                    highTemp = temps.maxOrNull() ?: 0.0,
                    lowTemp = temps.minOrNull() ?: 0.0,
                    symbolCode = middayData.symbol,
                    // Use max hourly precipitation, scale: 1mm = 10%, capped at 100%
                    precipitationProbability = if (maxPrecip > 0.1) (maxPrecip / 10.0).coerceAtMost(1.0) else null,
                    precipitationAmount = dayData.sumOf { it.precipitation },
                    windSpeed = avgWindSpeed,
                    windDirection = avgWindDirection
                )
            }
            .sortedBy { it.date }
            .take(7)
    }

    /**
     * Parse time string to LocalTime.
     */
    private fun parseTimeString(timeStr: String): LocalTime? {
        return try {
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse ISO datetime string to LocalDateTime.
     */
    private fun parseDateTime(isoString: String): LocalDateTime {
        return try {
            LocalDateTime.parse(isoString.substringBefore("+").substringBefore("Z"))
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }

    /**
     * Parse date string to LocalDate.
     */
    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
