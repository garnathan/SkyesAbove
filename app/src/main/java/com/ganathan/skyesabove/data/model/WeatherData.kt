package com.ganathan.skyesabove.data.model

/**
 * Represents weather data for a specific point in time.
 */
data class WeatherData(
    val time: String,
    val temperature: Double,
    val windSpeed: Double,  // km/h
    val windDirection: Int,  // degrees
    val humidity: Int,
    val pressure: Double,
    val cloudCover: Int,
    val precipitation: Double,
    val symbol: String,
    val description: String
)

/**
 * Response containing current weather and hourly forecast.
 */
data class WeatherResponse(
    val current: WeatherData,
    val hourly: List<WeatherData>
)

/**
 * Daily forecast summary.
 */
data class DailyForecast(
    val date: String,
    val highTemp: Double,
    val lowTemp: Double,
    val symbol: String,
    val precipitation: Double
)
