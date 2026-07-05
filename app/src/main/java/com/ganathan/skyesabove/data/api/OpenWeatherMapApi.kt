package com.ganathan.skyesabove.data.api

import com.ganathan.skyesabove.data.model.OpenWeatherMapResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for OpenWeatherMap API.
 * Requires an API key - get one free at https://openweathermap.org/api
 * Base URL: https://api.openweathermap.org/
 */
interface OpenWeatherMapApi {

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
        // Free tier API key - users can override in settings
        const val DEFAULT_API_KEY = "demo"
    }

    /**
     * Get current weather and forecast using One Call API 3.0.
     * Note: Free tier has limited calls, use sparingly.
     *
     * @param lat Latitude in decimal degrees
     * @param lon Longitude in decimal degrees
     * @param apiKey OpenWeatherMap API key
     * @param units Units of measurement (metric, imperial, standard)
     * @param exclude Parts to exclude from response
     * @return OpenWeatherMapResponse containing current and forecast data
     */
    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 40 // 5-day forecast, 3-hour intervals
    ): Response<OpenWeatherMapResponse>

    /**
     * Get current weather conditions.
     */
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<OpenWeatherMapCurrentResponse>
}

/**
 * Current weather response from OpenWeatherMap.
 */
data class OpenWeatherMapCurrentResponse(
    val coord: Coord?,
    val weather: List<Weather>?,
    val main: Main?,
    val wind: Wind?,
    val clouds: Clouds?,
    val dt: Long?,
    val name: String?
) {
    data class Coord(val lon: Double?, val lat: Double?)
    data class Weather(val id: Int?, val main: String?, val description: String?, val icon: String?)
    data class Main(
        val temp: Double?,
        val feels_like: Double?,
        val temp_min: Double?,
        val temp_max: Double?,
        val pressure: Int?,
        val humidity: Int?
    )
    data class Wind(val speed: Double?, val deg: Int?, val gust: Double?)
    data class Clouds(val all: Int?)
}
