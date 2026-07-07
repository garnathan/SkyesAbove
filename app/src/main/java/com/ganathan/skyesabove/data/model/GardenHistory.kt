package com.ganathan.skyesabove.data.model

import com.ganathan.skyesabove.data.preferences.TemperatureUnit

/**
 * One garden-sensor observation, parsed from a row of the Pi's monthly CSV
 * (`data/YYYY-MM.csv` in garnathan/wunderground-killi). Values are stored in their
 * canonical units (°C, %, mbar); unit conversion happens at display time.
 */
data class SensorReading(
    val epochSec: Long,
    val temperatureC: Double?,
    val humidityPct: Double?,
    val pressureMbar: Double?,     // sea-level (matches the widget's HOME pressure)
    val dewPointC: Double?,
    val heatIndexC: Double?
)

/**
 * A garden-sensor metric that can be trended. [extract] pulls the value (in display units)
 * from a [SensorReading], honoring the user's temperature-unit preference for temp-like metrics.
 */
enum class TrendMetric(
    val label: String,
    val emoji: String,
    val fractionDigits: Int
) {
    TEMPERATURE("Temperature", "🌡️", 1) {
        override fun extract(r: SensorReading, unit: TemperatureUnit) = convertTemp(r.temperatureC, unit)
        override fun unitLabel(unit: TemperatureUnit) = tempUnitLabel(unit)
    },
    FEELS("Feels", "🥵", 1) {
        override fun extract(r: SensorReading, unit: TemperatureUnit) = convertTemp(r.heatIndexC, unit)
        override fun unitLabel(unit: TemperatureUnit) = tempUnitLabel(unit)
    },
    HUMIDITY("Humidity", "💧", 0) {
        override fun extract(r: SensorReading, unit: TemperatureUnit) = r.humidityPct
        override fun unitLabel(unit: TemperatureUnit) = "%"
    },
    PRESSURE("Pressure", "🧭", 0) {
        override fun extract(r: SensorReading, unit: TemperatureUnit) = r.pressureMbar
        override fun unitLabel(unit: TemperatureUnit) = "mb"
    },
    DEW_POINT("Dew point", "💦", 1) {
        override fun extract(r: SensorReading, unit: TemperatureUnit) = convertTemp(r.dewPointC, unit)
        override fun unitLabel(unit: TemperatureUnit) = tempUnitLabel(unit)
    };

    /** Value in display units, or null if this reading lacks the metric. */
    abstract fun extract(r: SensorReading, unit: TemperatureUnit): Double?

    abstract fun unitLabel(unit: TemperatureUnit): String

    companion object {
        private fun convertTemp(celsius: Double?, unit: TemperatureUnit): Double? = when {
            celsius == null -> null
            unit == TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
            else -> celsius
        }

        private fun tempUnitLabel(unit: TemperatureUnit) =
            if (unit == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
    }
}

/**
 * A trend time window. [windowSeconds] is the look-back from now; [bucketSeconds] is the
 * down-sample bucket used to keep the plotted line readable (stats are computed on raw points).
 */
enum class TrendRange(
    val label: String,
    val windowSeconds: Long,
    val bucketSeconds: Long
) {
    HOUR("Hour", 60L * 60, 0L),                    // ~12 raw points @5min — no down-sampling
    DAY("Day", 24L * 60 * 60, 10L * 60),           // 10-min buckets
    WEEK("Week", 7L * 24 * 60 * 60, 60L * 60),     // hourly
    MONTH("Month", 30L * 24 * 60 * 60, 3L * 60 * 60),   // 3-hourly
    YEAR("Year", 365L * 24 * 60 * 60, 24L * 60 * 60);   // daily
}

/**
 * 3-hour pressure tendency — the direction (and speed) a barometer is moving, which forecasts
 * the next several hours better than the absolute value. Classified from the change per 3h with
 * a ~1 hPa dead-band so the daily atmospheric "breathing" (~1 hPa diurnal oscillation) doesn't
 * trigger a false arrow.
 */
enum class PressureTendency(val arrow: String, val label: String, val rising: Boolean?) {
    RISING_FAST("▲▲", "rising fast", true),
    RISING("▲", "rising", true),
    STEADY("→", "steady", null),
    FALLING("▼", "falling", false),
    FALLING_FAST("▼▼", "falling fast", false);

    companion object {
        /** @param deltaPer3h pressure change over 3 hours, in hPa/mbar. */
        fun classify(deltaPer3h: Double): PressureTendency = when {
            deltaPer3h >= 3.5 -> RISING_FAST
            deltaPer3h >= 1.0 -> RISING
            deltaPer3h <= -3.5 -> FALLING_FAST
            deltaPer3h <= -1.0 -> FALLING
            else -> STEADY
        }
    }
}

/**
 * The low→high sea-level pressure scale (hPa/mbar). Anchored on 1013 (standard atmosphere);
 * the [MIN]..[MAX] span covers the real-world extremes you'd ever glance at.
 */
object PressureScale {
    const val MIN = 960.0    // deep low / stormy
    const val MAX = 1060.0   // strong high / very settled

    /** 0f (low) .. 1f (high) position of a reading on the scale. */
    fun position(mbar: Double): Float =
        ((mbar - MIN) / (MAX - MIN)).coerceIn(0.0, 1.0).toFloat()

    /** Plain-language band for a reading (classic barometer wording). */
    fun descriptor(mbar: Double): String = when {
        mbar < 985 -> "Stormy"
        mbar < 1000 -> "Rain"
        mbar < 1015 -> "Changeable"
        mbar < 1030 -> "Fair"
        else -> "Very dry"
    }
}

/** A single plotted point (epoch seconds + value in display units). */
data class TrendPoint(val epochSec: Long, val value: Double)

/** Summary statistics over the raw (un-bucketed) readings in the window. */
data class TrendStats(
    val current: Double,
    val min: Double,
    val max: Double,
    val average: Double,
    val sampleCount: Int
)

/** Everything the Trends chart needs for one (metric, range) selection. */
data class TrendSeries(
    val metric: TrendMetric,
    val range: TrendRange,
    val unitLabel: String,
    val points: List<TrendPoint>,     // down-sampled, chronological
    val stats: TrendStats?,           // null when there are no readings
    val lastUpdatedEpochSec: Long?,
    val tendency: PressureTendency? = null   // pressure only; 3h barometric trend
)
