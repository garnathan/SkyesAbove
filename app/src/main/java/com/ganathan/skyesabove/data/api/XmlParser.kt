package com.ganathan.skyesabove.data.api

import com.ganathan.skyesabove.data.model.WeatherData
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Parser for Met Eireann XML weather data.
 *
 * The XML structure contains nested time elements with weather parameters:
 * - temperature (celsius)
 * - windSpeed (m/s - needs conversion to km/h)
 * - windDirection (degrees)
 * - humidity (percent)
 * - pressure (hPa)
 * - cloudiness (percent)
 * - precipitation (mm)
 * - symbol (weather code)
 */
object XmlParser {

    private const val MS_TO_KMH = 3.6

    /**
     * Parse Met Eireann XML response into list of WeatherData objects.
     *
     * @param xmlString Raw XML string from API
     * @return List of WeatherData parsed from XML
     */
    fun parseWeatherXml(xmlString: String): List<WeatherData> {
        val weatherDataList = mutableListOf<WeatherData>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlString))

        var currentTime: String? = null
        var temperature: Double? = null
        var windSpeedMs: Double? = null
        var windDirection: Int? = null
        var humidity: Int? = null
        var pressure: Double? = null
        var cloudCover: Int? = null
        var precipitation: Double? = null
        var symbol: String? = null

        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "time" -> {
                            currentTime = parser.getAttributeValue(null, "from")
                                ?: parser.getAttributeValue(null, "datatype")
                        }
                        "temperature" -> {
                            temperature = parser.getAttributeValue(null, "value")?.toDoubleOrNull()
                        }
                        "windSpeed" -> {
                            windSpeedMs = parser.getAttributeValue(null, "mps")?.toDoubleOrNull()
                        }
                        "windDirection" -> {
                            windDirection = parser.getAttributeValue(null, "deg")?.toDoubleOrNull()?.toInt()
                        }
                        "humidity" -> {
                            humidity = parser.getAttributeValue(null, "value")?.toDoubleOrNull()?.toInt()
                        }
                        "pressure" -> {
                            pressure = parser.getAttributeValue(null, "value")?.toDoubleOrNull()
                        }
                        "cloudiness" -> {
                            cloudCover = parser.getAttributeValue(null, "percent")?.toDoubleOrNull()?.toInt()
                        }
                        "precipitation" -> {
                            precipitation = parser.getAttributeValue(null, "value")?.toDoubleOrNull()
                        }
                        "symbol" -> {
                            symbol = parser.getAttributeValue(null, "id")
                                ?: parser.getAttributeValue(null, "number")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "time" && currentTime != null) {
                        // Only create WeatherData if we have essential data
                        if (temperature != null || symbol != null) {
                            weatherDataList.add(
                                WeatherData(
                                    time = currentTime,
                                    temperature = temperature ?: 0.0,
                                    windSpeed = (windSpeedMs ?: 0.0) * MS_TO_KMH,
                                    windDirection = windDirection ?: 0,
                                    humidity = humidity ?: 0,
                                    pressure = pressure ?: 0.0,
                                    cloudCover = cloudCover ?: 0,
                                    precipitation = precipitation ?: 0.0,
                                    symbol = symbol ?: "",
                                    description = getWeatherDescription(symbol)
                                )
                            )
                        }
                        // Reset for next time element
                        currentTime = null
                        temperature = null
                        windSpeedMs = null
                        windDirection = null
                        humidity = null
                        pressure = null
                        cloudCover = null
                        precipitation = null
                        symbol = null
                    }
                }
            }
            eventType = parser.next()
        }

        return weatherDataList
    }

    /**
     * Merge point forecast data (temperature, wind, etc.) with period forecast data (symbol, precipitation).
     * Met Eireann provides these in separate time elements that need to be combined.
     *
     * @param weatherDataList Raw parsed data with potentially incomplete entries
     * @return Merged list with complete weather data
     */
    fun mergeWeatherData(weatherDataList: List<WeatherData>): List<WeatherData> {
        if (weatherDataList.isEmpty()) return emptyList()

        val mergedMap = mutableMapOf<String, WeatherData>()

        for (data in weatherDataList) {
            val timeKey = data.time.substringBefore("T").plus("T").plus(
                data.time.substringAfter("T").substringBefore(":").padStart(2, '0') + ":00"
            )

            val existing = mergedMap[timeKey]
            if (existing != null) {
                // Merge: prefer non-default values
                mergedMap[timeKey] = existing.copy(
                    temperature = if (data.temperature != 0.0) data.temperature else existing.temperature,
                    windSpeed = if (data.windSpeed != 0.0) data.windSpeed else existing.windSpeed,
                    windDirection = if (data.windDirection != 0) data.windDirection else existing.windDirection,
                    humidity = if (data.humidity != 0) data.humidity else existing.humidity,
                    pressure = if (data.pressure != 0.0) data.pressure else existing.pressure,
                    cloudCover = if (data.cloudCover != 0) data.cloudCover else existing.cloudCover,
                    precipitation = if (data.precipitation != 0.0) data.precipitation else existing.precipitation,
                    symbol = if (data.symbol.isNotEmpty()) data.symbol else existing.symbol,
                    description = if (data.description.isNotEmpty()) data.description else existing.description
                )
            } else {
                mergedMap[timeKey] = data.copy(time = timeKey)
            }
        }

        return mergedMap.values.sortedBy { it.time }
    }

    /**
     * Get human-readable description for weather symbol code.
     *
     * @param symbol Weather symbol code from API
     * @return Human-readable weather description
     */
    private fun getWeatherDescription(symbol: String?): String {
        return when {
            symbol == null -> ""
            symbol.contains("Sun", ignoreCase = true) || symbol == "1" -> "Sunny"
            symbol.contains("LightCloud", ignoreCase = true) || symbol == "2" -> "Partly Cloudy"
            symbol.contains("PartlyCloud", ignoreCase = true) || symbol == "3" -> "Partly Cloudy"
            symbol.contains("Cloud", ignoreCase = true) || symbol == "4" -> "Cloudy"
            symbol.contains("LightRainSun", ignoreCase = true) || symbol == "5" -> "Light Rain, Some Sun"
            symbol.contains("LightRainThunderSun", ignoreCase = true) || symbol == "6" -> "Light Rain, Thunder"
            symbol.contains("SleetSun", ignoreCase = true) || symbol == "7" -> "Sleet, Some Sun"
            symbol.contains("SnowSun", ignoreCase = true) || symbol == "8" -> "Snow, Some Sun"
            symbol.contains("LightRain", ignoreCase = true) || symbol == "9" -> "Light Rain"
            symbol.contains("Rain", ignoreCase = true) || symbol == "10" -> "Rain"
            symbol.contains("RainThunder", ignoreCase = true) || symbol == "11" -> "Rain and Thunder"
            symbol.contains("Sleet", ignoreCase = true) || symbol == "12" -> "Sleet"
            symbol.contains("Snow", ignoreCase = true) || symbol == "13" -> "Snow"
            symbol.contains("SnowThunder", ignoreCase = true) || symbol == "14" -> "Snow and Thunder"
            symbol.contains("Fog", ignoreCase = true) || symbol == "15" -> "Fog"
            symbol.contains("DrizzleSun", ignoreCase = true) -> "Drizzle, Some Sun"
            symbol.contains("Drizzle", ignoreCase = true) -> "Drizzle"
            symbol.contains("Thunder", ignoreCase = true) -> "Thunderstorm"
            else -> "Unknown"
        }
    }
}
