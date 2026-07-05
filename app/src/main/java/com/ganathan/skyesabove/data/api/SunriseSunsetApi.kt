package com.ganathan.skyesabove.data.api

import com.ganathan.skyesabove.data.model.SunriseSunsetResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for sunrise-sunset.org API.
 * Base URL: https://api.sunrise-sunset.org/
 */
interface SunriseSunsetApi {

    companion object {
        const val BASE_URL = "https://api.sunrise-sunset.org/"
    }

    /**
     * Get sun times for specified coordinates.
     *
     * @param lat Latitude in decimal degrees
     * @param lng Longitude in decimal degrees
     * @param formatted 0 for ISO 8601 format, 1 for human-readable
     * @return SunriseSunsetResponse with timing data
     */
    @GET("json")
    suspend fun getSunTimes(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("formatted") formatted: Int = 0
    ): Response<SunriseSunsetResponse>
}
