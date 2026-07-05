package com.ganathan.skyesabove.data.model

/**
 * Represents sun timing data for a location.
 */
data class SunData(
    val sunrise: String,
    val sunset: String,
    val civilTwilightEnd: String,
    val dayLengthHours: Double
)
