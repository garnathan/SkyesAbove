package com.ganathan.skyesabove.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response from Open-Meteo API.
 */
data class OpenMeteoResponse(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("timezone")
    val timezone: String?,
    @SerializedName("current")
    val current: OpenMeteoCurrent?,
    @SerializedName("hourly")
    val hourly: OpenMeteoHourly?
)

/**
 * Current weather data from Open-Meteo.
 */
data class OpenMeteoCurrent(
    @SerializedName("time")
    val time: String?,
    @SerializedName("temperature_2m")
    val temperature: Double?,
    @SerializedName("relative_humidity_2m")
    val humidity: Int?,
    @SerializedName("precipitation")
    val precipitation: Double?,
    @SerializedName("weather_code")
    val weatherCode: Int?,
    @SerializedName("wind_speed_10m")
    val windSpeed: Double?,
    @SerializedName("wind_direction_10m")
    val windDirection: Int?,
    @SerializedName("cloud_cover")
    val cloudCover: Int?,
    @SerializedName("pressure_msl")
    val pressure: Double?
)

/**
 * Hourly forecast data from Open-Meteo.
 * Arrays are parallel - index i in each array corresponds to the same time point.
 */
data class OpenMeteoHourly(
    @SerializedName("time")
    val time: List<String>?,
    @SerializedName("temperature_2m")
    val temperature: List<Double>?,
    @SerializedName("relative_humidity_2m")
    val humidity: List<Int>?,
    @SerializedName("precipitation")
    val precipitation: List<Double>?,
    @SerializedName("weather_code")
    val weatherCode: List<Int>?,
    @SerializedName("wind_speed_10m")
    val windSpeed: List<Double>?,
    @SerializedName("wind_direction_10m")
    val windDirection: List<Int>?,
    @SerializedName("cloud_cover")
    val cloudCover: List<Int>?,
    @SerializedName("pressure_msl")
    val pressure: List<Double>?
)
