package com.ganathan.skyesabove.ui.util

/**
 * Utility object for converting wind direction degrees to compass directions
 * and providing rotation angles for wind direction arrows.
 */
object WindDirectionUtil {

    /**
     * Compass directions with their degree ranges
     */
    enum class CompassDirection(val abbreviation: String, val fullName: String) {
        N("N", "North"),
        NNE("NNE", "North-Northeast"),
        NE("NE", "Northeast"),
        ENE("ENE", "East-Northeast"),
        E("E", "East"),
        ESE("ESE", "East-Southeast"),
        SE("SE", "Southeast"),
        SSE("SSE", "South-Southeast"),
        S("S", "South"),
        SSW("SSW", "South-Southwest"),
        SW("SW", "Southwest"),
        WSW("WSW", "West-Southwest"),
        W("W", "West"),
        WNW("WNW", "West-Northwest"),
        NW("NW", "Northwest"),
        NNW("NNW", "North-Northwest")
    }

    /**
     * Converts wind direction in degrees to a compass direction string.
     * Uses 8 primary compass directions (N, NE, E, SE, S, SW, W, NW).
     *
     * @param degrees Wind direction in degrees (0-360, where 0/360 = North)
     * @return Compass direction abbreviation (e.g., "N", "NE", "SW")
     */
    fun degreesToCompassDirection(degrees: Double?): String {
        if (degrees == null) return "N/A"

        // Normalize degrees to 0-360 range
        val normalizedDegrees = ((degrees % 360) + 360) % 360

        return when {
            normalizedDegrees >= 337.5 || normalizedDegrees < 22.5 -> "N"
            normalizedDegrees >= 22.5 && normalizedDegrees < 67.5 -> "NE"
            normalizedDegrees >= 67.5 && normalizedDegrees < 112.5 -> "E"
            normalizedDegrees >= 112.5 && normalizedDegrees < 157.5 -> "SE"
            normalizedDegrees >= 157.5 && normalizedDegrees < 202.5 -> "S"
            normalizedDegrees >= 202.5 && normalizedDegrees < 247.5 -> "SW"
            normalizedDegrees >= 247.5 && normalizedDegrees < 292.5 -> "W"
            normalizedDegrees >= 292.5 && normalizedDegrees < 337.5 -> "NW"
            else -> "N"
        }
    }

    /**
     * Converts wind direction in degrees to a 16-point compass direction.
     *
     * @param degrees Wind direction in degrees (0-360, where 0/360 = North)
     * @return CompassDirection enum value
     */
    fun degreesToCompassDirection16(degrees: Double?): CompassDirection {
        if (degrees == null) return CompassDirection.N

        // Normalize degrees to 0-360 range
        val normalizedDegrees = ((degrees % 360) + 360) % 360

        return when {
            normalizedDegrees >= 348.75 || normalizedDegrees < 11.25 -> CompassDirection.N
            normalizedDegrees >= 11.25 && normalizedDegrees < 33.75 -> CompassDirection.NNE
            normalizedDegrees >= 33.75 && normalizedDegrees < 56.25 -> CompassDirection.NE
            normalizedDegrees >= 56.25 && normalizedDegrees < 78.75 -> CompassDirection.ENE
            normalizedDegrees >= 78.75 && normalizedDegrees < 101.25 -> CompassDirection.E
            normalizedDegrees >= 101.25 && normalizedDegrees < 123.75 -> CompassDirection.ESE
            normalizedDegrees >= 123.75 && normalizedDegrees < 146.25 -> CompassDirection.SE
            normalizedDegrees >= 146.25 && normalizedDegrees < 168.75 -> CompassDirection.SSE
            normalizedDegrees >= 168.75 && normalizedDegrees < 191.25 -> CompassDirection.S
            normalizedDegrees >= 191.25 && normalizedDegrees < 213.75 -> CompassDirection.SSW
            normalizedDegrees >= 213.75 && normalizedDegrees < 236.25 -> CompassDirection.SW
            normalizedDegrees >= 236.25 && normalizedDegrees < 258.75 -> CompassDirection.WSW
            normalizedDegrees >= 258.75 && normalizedDegrees < 281.25 -> CompassDirection.W
            normalizedDegrees >= 281.25 && normalizedDegrees < 303.75 -> CompassDirection.WNW
            normalizedDegrees >= 303.75 && normalizedDegrees < 326.25 -> CompassDirection.NW
            normalizedDegrees >= 326.25 && normalizedDegrees < 348.75 -> CompassDirection.NNW
            else -> CompassDirection.N
        }
    }

    /**
     * Gets the rotation angle for a wind direction arrow.
     * The arrow should point in the direction the wind is blowing TO.
     *
     * Note: Meteorological convention is that wind direction indicates
     * where the wind is coming FROM. So a "north wind" (0 degrees) blows
     * from north to south, meaning our arrow should point south (180 degrees rotation).
     *
     * @param degrees Wind direction in degrees (direction wind is coming FROM)
     * @return Rotation angle for the arrow icon (pointing where wind is going TO)
     */
    fun getArrowRotation(degrees: Double?): Float {
        if (degrees == null) return 0f

        // Wind comes FROM the given direction, so add 180 to point TO direction
        // Then subtract 90 because arrow icons typically point right (east) by default
        return ((degrees + 180) % 360).toFloat()
    }

    /**
     * Gets the rotation angle for a wind direction arrow that should point
     * in the direction the wind is coming FROM.
     *
     * @param degrees Wind direction in degrees
     * @return Rotation angle for the arrow icon (pointing where wind is coming FROM)
     */
    fun getArrowRotationFromDirection(degrees: Double?): Float {
        if (degrees == null) return 0f

        // Arrow should point in the direction wind is coming FROM
        return (degrees % 360).toFloat()
    }

    /**
     * Formats wind speed and direction into a readable string.
     *
     * @param speed Wind speed value
     * @param degrees Wind direction in degrees
     * @param unit Unit string (e.g., "m/s", "km/h", "mph")
     * @return Formatted string like "15 km/h NW"
     */
    fun formatWindString(speed: Double?, degrees: Double?, unit: String = "m/s"): String {
        if (speed == null) return "N/A"

        val direction = degreesToCompassDirection(degrees)
        return String.format("%.1f %s %s", speed, unit, direction)
    }

    /**
     * Converts wind speed from m/s to km/h
     */
    fun msToKmh(speedMs: Double): Double = speedMs * 3.6

    /**
     * Converts wind speed from m/s to mph
     */
    fun msToMph(speedMs: Double): Double = speedMs * 2.237

    /**
     * Converts wind speed from km/h to m/s
     */
    fun kmhToMs(speedKmh: Double): Double = speedKmh / 3.6

    /**
     * Gets the Beaufort scale description for a given wind speed in m/s
     */
    fun getBeaufortDescription(speedMs: Double): String {
        return when {
            speedMs < 0.5 -> "Calm"
            speedMs < 1.6 -> "Light Air"
            speedMs < 3.4 -> "Light Breeze"
            speedMs < 5.5 -> "Gentle Breeze"
            speedMs < 8.0 -> "Moderate Breeze"
            speedMs < 10.8 -> "Fresh Breeze"
            speedMs < 13.9 -> "Strong Breeze"
            speedMs < 17.2 -> "Near Gale"
            speedMs < 20.8 -> "Gale"
            speedMs < 24.5 -> "Strong Gale"
            speedMs < 28.5 -> "Storm"
            speedMs < 32.7 -> "Violent Storm"
            else -> "Hurricane"
        }
    }
}
