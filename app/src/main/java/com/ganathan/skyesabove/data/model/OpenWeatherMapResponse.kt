package com.ganathan.skyesabove.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response from OpenWeatherMap 5-day/3-hour Forecast API.
 */
data class OpenWeatherMapResponse(
    @SerializedName("cod")
    val cod: String?,
    @SerializedName("message")
    val message: Int?,
    @SerializedName("cnt")
    val count: Int?,
    @SerializedName("list")
    val list: List<ForecastItem>?,
    @SerializedName("city")
    val city: City?
)

/**
 * Individual forecast item (3-hour interval).
 */
data class ForecastItem(
    @SerializedName("dt")
    val dt: Long?,
    @SerializedName("main")
    val main: MainData?,
    @SerializedName("weather")
    val weather: List<WeatherCondition>?,
    @SerializedName("clouds")
    val clouds: CloudData?,
    @SerializedName("wind")
    val wind: WindData?,
    @SerializedName("visibility")
    val visibility: Int?,
    @SerializedName("pop")
    val pop: Double?, // Probability of precipitation
    @SerializedName("rain")
    val rain: RainData?,
    @SerializedName("snow")
    val snow: SnowData?,
    @SerializedName("dt_txt")
    val dtTxt: String?
)

/**
 * Main weather data.
 */
data class MainData(
    @SerializedName("temp")
    val temp: Double?,
    @SerializedName("feels_like")
    val feelsLike: Double?,
    @SerializedName("temp_min")
    val tempMin: Double?,
    @SerializedName("temp_max")
    val tempMax: Double?,
    @SerializedName("pressure")
    val pressure: Int?,
    @SerializedName("sea_level")
    val seaLevel: Int?,
    @SerializedName("grnd_level")
    val groundLevel: Int?,
    @SerializedName("humidity")
    val humidity: Int?
)

/**
 * Weather condition description.
 */
data class WeatherCondition(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("main")
    val main: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("icon")
    val icon: String?
)

/**
 * Cloud data.
 */
data class CloudData(
    @SerializedName("all")
    val all: Int?
)

/**
 * Wind data.
 */
data class WindData(
    @SerializedName("speed")
    val speed: Double?,
    @SerializedName("deg")
    val deg: Int?,
    @SerializedName("gust")
    val gust: Double?
)

/**
 * Rain data (3-hour volume).
 */
data class RainData(
    @SerializedName("3h")
    val threeHour: Double?
)

/**
 * Snow data (3-hour volume).
 */
data class SnowData(
    @SerializedName("3h")
    val threeHour: Double?
)

/**
 * City information.
 */
data class City(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("coord")
    val coord: CityCoord?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("population")
    val population: Int?,
    @SerializedName("timezone")
    val timezone: Int?,
    @SerializedName("sunrise")
    val sunrise: Long?,
    @SerializedName("sunset")
    val sunset: Long?
)

/**
 * City coordinates.
 */
data class CityCoord(
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lon")
    val lon: Double?
)
