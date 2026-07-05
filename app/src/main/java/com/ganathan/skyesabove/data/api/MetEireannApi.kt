package com.ganathan.skyesabove.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Met Eireann weather API.
 * Base URL: http://openaccess.pf.api.met.ie/metno-wdb2ts/
 *
 * Returns XML data that needs to be parsed.
 */
interface MetEireannApi {

    companion object {
        const val BASE_URL = "http://openaccess.pf.api.met.ie/metno-wdb2ts/"
    }

    /**
     * Get location forecast for specified coordinates.
     *
     * @param lat Latitude in decimal degrees
     * @param long Longitude in decimal degrees
     * @return ResponseBody containing XML weather data
     */
    @GET("locationforecast")
    suspend fun getLocationForecast(
        @Query("lat") lat: Double,
        @Query("long") long: Double
    ): Response<ResponseBody>
}
