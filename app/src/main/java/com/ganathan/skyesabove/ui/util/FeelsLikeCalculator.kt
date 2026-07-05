package com.ganathan.skyesabove.ui.util

import kotlin.math.pow

/**
 * Utility object for calculating "feels like" temperature using wind chill
 * and heat index formulas.
 *
 * - Wind Chill: Used when temperature is at or below 10C (50F) and wind speed > 4.8 km/h
 * - Heat Index: Used when temperature is at or above 27C (80F) and humidity > 40%
 * - Otherwise: Returns the actual temperature
 */
object FeelsLikeCalculator {

    /**
     * Calculates the "feels like" temperature based on actual temperature,
     * wind speed, and humidity.
     *
     * @param temperatureCelsius Actual temperature in Celsius
     * @param windSpeedMs Wind speed in meters per second
     * @param humidityPercent Relative humidity as a percentage (0-100)
     * @return Feels-like temperature in Celsius
     */
    fun calculateFeelsLike(
        temperatureCelsius: Double,
        windSpeedMs: Double,
        humidityPercent: Double
    ): Double {
        val windSpeedKmh = windSpeedMs * 3.6

        return when {
            // Cold conditions: Use wind chill
            temperatureCelsius <= 10.0 && windSpeedKmh > 4.8 -> {
                calculateWindChill(temperatureCelsius, windSpeedKmh)
            }
            // Hot conditions: Use heat index
            temperatureCelsius >= 27.0 && humidityPercent > 40.0 -> {
                calculateHeatIndex(temperatureCelsius, humidityPercent)
            }
            // Moderate conditions: Return actual temperature
            else -> temperatureCelsius
        }
    }

    /**
     * Calculates wind chill using the North American/UK formula.
     * Valid for temperatures at or below 10C and wind speeds above 4.8 km/h.
     *
     * Formula: 13.12 + 0.6215*T - 11.37*V^0.16 + 0.3965*T*V^0.16
     * Where T = temperature in Celsius, V = wind speed in km/h
     *
     * @param temperatureCelsius Temperature in Celsius
     * @param windSpeedKmh Wind speed in km/h
     * @return Wind chill temperature in Celsius
     */
    fun calculateWindChill(temperatureCelsius: Double, windSpeedKmh: Double): Double {
        // Wind chill formula is only valid for T <= 10C and V > 4.8 km/h
        if (temperatureCelsius > 10.0 || windSpeedKmh <= 4.8) {
            return temperatureCelsius
        }

        val windPower = windSpeedKmh.pow(0.16)
        return 13.12 +
               0.6215 * temperatureCelsius -
               11.37 * windPower +
               0.3965 * temperatureCelsius * windPower
    }

    /**
     * Calculates heat index using the Rothfusz regression equation.
     * Valid for temperatures at or above 27C (80F).
     *
     * @param temperatureCelsius Temperature in Celsius
     * @param humidityPercent Relative humidity as a percentage (0-100)
     * @return Heat index temperature in Celsius
     */
    fun calculateHeatIndex(temperatureCelsius: Double, humidityPercent: Double): Double {
        // Heat index is only meaningful above 27C (80F)
        if (temperatureCelsius < 27.0) {
            return temperatureCelsius
        }

        // Convert to Fahrenheit for the standard formula
        val tempF = celsiusToFahrenheit(temperatureCelsius)
        val rh = humidityPercent

        // Rothfusz regression equation
        var heatIndexF = -42.379 +
                2.04901523 * tempF +
                10.14333127 * rh -
                0.22475541 * tempF * rh -
                0.00683783 * tempF * tempF -
                0.05481717 * rh * rh +
                0.00122874 * tempF * tempF * rh +
                0.00085282 * tempF * rh * rh -
                0.00000199 * tempF * tempF * rh * rh

        // Adjustments for extreme conditions
        if (rh < 13.0 && tempF >= 80.0 && tempF <= 112.0) {
            val adjustment = ((13.0 - rh) / 4.0) *
                            kotlin.math.sqrt((17.0 - kotlin.math.abs(tempF - 95.0)) / 17.0)
            heatIndexF -= adjustment
        } else if (rh > 85.0 && tempF >= 80.0 && tempF <= 87.0) {
            val adjustment = ((rh - 85.0) / 10.0) * ((87.0 - tempF) / 5.0)
            heatIndexF += adjustment
        }

        return fahrenheitToCelsius(heatIndexF)
    }

    /**
     * Calculates apparent temperature using the Australian Bureau of Meteorology formula.
     * This is an alternative "feels like" calculation that works across all temperature ranges.
     *
     * AT = Ta + 0.33 * e - 0.70 * ws - 4.00
     * Where:
     *   Ta = dry bulb temperature (C)
     *   e = water vapor pressure (hPa) = rh/100 * 6.105 * exp(17.27 * Ta / (237.7 + Ta))
     *   ws = wind speed (m/s)
     *
     * @param temperatureCelsius Temperature in Celsius
     * @param humidityPercent Relative humidity as a percentage (0-100)
     * @param windSpeedMs Wind speed in meters per second
     * @return Apparent temperature in Celsius
     */
    fun calculateApparentTemperature(
        temperatureCelsius: Double,
        humidityPercent: Double,
        windSpeedMs: Double
    ): Double {
        // Calculate water vapor pressure
        val e = (humidityPercent / 100.0) * 6.105 *
                kotlin.math.exp((17.27 * temperatureCelsius) / (237.7 + temperatureCelsius))

        return temperatureCelsius + 0.33 * e - 0.70 * windSpeedMs - 4.00
    }

    /**
     * Converts Celsius to Fahrenheit
     */
    fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0

    /**
     * Converts Fahrenheit to Celsius
     */
    fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32.0) * 5.0 / 9.0

    /**
     * Formats the feels-like temperature with context about what's affecting it.
     *
     * @param actualTemp Actual temperature in Celsius
     * @param feelsLikeTemp Feels-like temperature in Celsius
     * @param windSpeedMs Wind speed in m/s
     * @param humidityPercent Humidity percentage
     * @return A description like "Feels colder due to wind" or "Feels warmer due to humidity"
     */
    fun getFeelsLikeDescription(
        actualTemp: Double,
        feelsLikeTemp: Double,
        windSpeedMs: Double,
        humidityPercent: Double
    ): String {
        val diff = feelsLikeTemp - actualTemp
        val windSpeedKmh = windSpeedMs * 3.6

        return when {
            kotlin.math.abs(diff) < 1.0 -> "Feels about right"
            diff < -5.0 && windSpeedKmh > 10 -> "Feels much colder due to wind"
            diff < -2.0 && windSpeedKmh > 5 -> "Feels colder due to wind chill"
            diff < 0 -> "Feels slightly cooler"
            diff > 5.0 && humidityPercent > 60 -> "Feels much warmer due to humidity"
            diff > 2.0 && humidityPercent > 50 -> "Feels warmer due to humidity"
            diff > 0 -> "Feels slightly warmer"
            else -> "Feels about right"
        }
    }
}
