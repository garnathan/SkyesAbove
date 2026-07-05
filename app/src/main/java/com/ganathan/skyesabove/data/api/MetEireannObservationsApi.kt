package com.ganathan.skyesabove.data.api

import com.ganathan.skyesabove.data.model.ObservationsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for Met Éireann Observations API.
 * Base URL: https://prodapi.metweb.ie/
 *
 * Provides current weather observations from Irish weather stations.
 */
interface MetEireannObservationsApi {

    companion object {
        const val BASE_URL = "https://prodapi.metweb.ie/"
    }

    /**
     * Get current observations for a station.
     * Returns an array of observations throughout the day.
     *
     * @param station Station name (e.g., "dublin", "cork", "galway")
     * @return List of ObservationsResponse, use last element for most recent
     */
    @GET("observations/{station}/today")
    suspend fun getObservations(
        @Path("station") station: String
    ): Response<List<ObservationsResponse>>
}
