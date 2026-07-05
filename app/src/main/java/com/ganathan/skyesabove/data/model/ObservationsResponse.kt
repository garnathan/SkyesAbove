package com.ganathan.skyesabove.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response from Met Éireann Observations API.
 * Note: Most numeric values come as strings from the API.
 */
data class ObservationsResponse(
    @SerializedName("name")
    val name: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("reportTime")
    val reportTime: String?,
    @SerializedName("temperature")
    val temperature: String?,
    @SerializedName("humidity")
    val humidity: String?,
    @SerializedName("rainfall")
    val rainfall: String?,
    @SerializedName("pressure")
    val pressure: String?,
    @SerializedName("windSpeed")
    val windSpeed: String?,
    @SerializedName("windGust")
    val windGust: String?,
    @SerializedName("windDirection")
    val windDirection: Int?,
    @SerializedName("cardinalWindDirection")
    val cardinalWindDirection: String?,
    @SerializedName("symbol")
    val symbol: String?,
    @SerializedName("weatherDescription")
    val weatherDescription: String?
)

/**
 * Represents parsed current weather from observations.
 */
data class CurrentObservation(
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val windGust: Double?,
    val windDirection: Int,
    val pressure: Double,
    val rainfall: Double,
    val condition: String,
    val description: String
)
