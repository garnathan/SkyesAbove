package com.ganathan.skyesabove.data.api

import com.ganathan.skyesabove.data.model.OpenMeteoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Open-Meteo API.
 * Free weather API with no key required.
 * Base URL: https://api.open-meteo.com/
 */
interface OpenMeteoApi {

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }

    /**
     * Get weather forecast for a location.
     *
     * @param lat Latitude in decimal degrees
     * @param lon Longitude in decimal degrees
     * @param hourly Comma-separated list of hourly variables
     * @param current Comma-separated list of current weather variables
     * @param timezone Timezone for the response (auto = detect from coordinates)
     * @return OpenMeteoResponse containing forecast data
     */
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,cloud_cover,pressure_msl",
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,cloud_cover,pressure_msl",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): Response<OpenMeteoResponse>
}
