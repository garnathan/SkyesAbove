package com.ganathan.skyesabove.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response from the sunrise-sunset.org API.
 */
data class SunriseSunsetResponse(
    @SerializedName("results")
    val results: SunriseSunsetResults,
    @SerializedName("status")
    val status: String
)

/**
 * Results containing sun timing information.
 * All times are in ISO 8601 format when formatted=0.
 */
data class SunriseSunsetResults(
    @SerializedName("sunrise")
    val sunrise: String,
    @SerializedName("sunset")
    val sunset: String,
    @SerializedName("solar_noon")
    val solarNoon: String,
    @SerializedName("day_length")
    val dayLength: Long,  // seconds when formatted=0
    @SerializedName("civil_twilight_begin")
    val civilTwilightBegin: String,
    @SerializedName("civil_twilight_end")
    val civilTwilightEnd: String,
    @SerializedName("nautical_twilight_begin")
    val nauticalTwilightBegin: String,
    @SerializedName("nautical_twilight_end")
    val nauticalTwilightEnd: String,
    @SerializedName("astronomical_twilight_begin")
    val astronomicalTwilightBegin: String,
    @SerializedName("astronomical_twilight_end")
    val astronomicalTwilightEnd: String
)
