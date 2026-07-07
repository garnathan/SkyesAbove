package com.ganathan.skyesabove.data.repository

import android.util.Log
import com.ganathan.skyesabove.data.api.GardenHistoryApi
import com.ganathan.skyesabove.data.model.PressureTendency
import com.ganathan.skyesabove.data.model.SensorReading
import com.ganathan.skyesabove.data.model.TrendMetric
import com.ganathan.skyesabove.data.model.TrendPoint
import com.ganathan.skyesabove.data.model.TrendRange
import com.ganathan.skyesabove.data.model.TrendSeries
import com.ganathan.skyesabove.data.model.TrendStats
import com.ganathan.skyesabove.data.preferences.TemperatureUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the garden Pi's published sensor history from GitHub (garnathan/wunderground-killi)
 * and shapes it into a [TrendSeries] for the Trends screen.
 *
 * The monthly CSVs are the source of truth (recent.json was reset to `[]` in the fresh-start
 * wipe). For a given range we fetch every month file spanning the window in parallel, parse,
 * filter to the window, compute stats on the RAW readings, then down-sample for a clean line.
 * Parsed months are cached briefly so flipping metric/range doesn't re-download.
 */
@Singleton
class GardenHistoryRepository @Inject constructor(
    private val api: GardenHistoryApi
) {
    private data class CachedMonth(val readings: List<SensorReading>, val fetchedAtMs: Long)

    private val cacheLock = Mutex()
    private val monthCache = HashMap<String, CachedMonth>()

    // The Pi lives in Ireland; month filenames follow its local date.
    private val piZone: ZoneId = runCatching { ZoneId.of("Europe/Dublin") }.getOrDefault(ZoneId.systemDefault())

    /**
     * Build the trend series for a metric + range. Returns a series with empty points and
     * null stats if no data is available (rather than throwing) so the UI can show an
     * "no data yet" state; throws only on a hard, total fetch failure.
     */
    suspend fun getTrend(
        metric: TrendMetric,
        range: TrendRange,
        unit: TemperatureUnit,
        nowMs: Long = System.currentTimeMillis()
    ): TrendSeries = withContext(Dispatchers.IO) {
        val nowSec = nowMs / 1000L
        val startSec = nowSec - range.windowSeconds

        // Load enough to cover both the range window AND the last 3h (for the tendency), even
        // when the range is shorter than 3h (e.g. "Hour"). loadReadings returns whole month
        // files, so this only widens which months are fetched — usually none.
        val allReadings = loadReadings(minOf(startSec, nowSec - TENDENCY_WINDOW_SEC), nowSec)
            .sortedBy { it.epochSec }
        val readings = allReadings.filter { it.epochSec in startSec..nowSec }

        // Pair each reading with its metric value (in display units), dropping nulls.
        val valued = readings.mapNotNull { r ->
            metric.extract(r, unit)?.let { v -> r.epochSec to v }
        }

        val stats = if (valued.isEmpty()) null else {
            val values = valued.map { it.second }
            TrendStats(
                current = values.last(),
                min = values.min(),
                max = values.max(),
                average = values.average(),
                sampleCount = values.size
            )
        }

        TrendSeries(
            metric = metric,
            range = range,
            unitLabel = metric.unitLabel(unit),
            points = downsample(valued, range.bucketSeconds),
            stats = stats,
            lastUpdatedEpochSec = readings.lastOrNull()?.epochSec,
            tendency = if (metric == TrendMetric.PRESSURE) tendencyFrom(allReadings, nowSec) else null
        )
    }

    /**
     * The 3-hour barometric tendency (for the widget's arrow), independent of any chart range.
     * Returns null if we can't establish a meaningful recent trend.
     */
    suspend fun pressureTendency(nowMs: Long = System.currentTimeMillis()): PressureTendency? =
        withContext(Dispatchers.IO) {
            val nowSec = nowMs / 1000L
            tendencyFrom(loadReadings(nowSec - TENDENCY_WINDOW_SEC, nowSec), nowSec)
        }

    /**
     * Compute the pressure tendency from the last 3h of readings, normalised to a per-3h rate
     * (so a partial window still classifies sensibly). Needs ≥2 points spanning ≥1h.
     */
    private fun tendencyFrom(readings: List<SensorReading>, nowSec: Long): PressureTendency? {
        val cutoff = nowSec - TENDENCY_WINDOW_SEC
        val recent = readings
            .filter { it.epochSec in cutoff..nowSec && it.pressureMbar != null }
            .sortedBy { it.epochSec }
        if (recent.size < 2) return null
        val first = recent.first()
        val last = recent.last()
        val spanSec = (last.epochSec - first.epochSec).toDouble()
        if (spanSec < 60L * 60) return null   // <1h span — not enough to trust
        val deltaPer3h = (last.pressureMbar!! - first.pressureMbar!!) * (TENDENCY_WINDOW_SEC / spanSec)
        return PressureTendency.classify(deltaPer3h)
    }

    /** Fetch + cache every month file spanning [startSec, endSec]; merge & de-dupe by epoch. */
    private suspend fun loadReadings(startSec: Long, endSec: Long): List<SensorReading> {
        val months = monthsSpanning(startSec, endSec)
        val perMonth = coroutineScope {
            months.map { ym -> async { monthReadings(ym) } }.map { it.await() }
        }
        // De-dupe by epoch (months don't overlap, but be safe), keep chronological.
        return perMonth.flatten().associateBy { it.epochSec }.values.sortedBy { it.epochSec }
    }

    private suspend fun monthReadings(yearMonth: String): List<SensorReading> {
        cacheLock.withLock {
            monthCache[yearMonth]?.let { cached ->
                if (System.currentTimeMillis() - cached.fetchedAtMs < CACHE_TTL_MS) return cached.readings
            }
        }
        val readings = try {
            val resp = api.getRaw(GardenHistoryApi.monthCsvUrl(yearMonth))
            if (resp.isSuccessful) {
                resp.body()?.string()?.let { parseCsv(it) } ?: emptyList()
            } else {
                if (resp.code() != 404) Log.w(TAG, "history $yearMonth -> HTTP ${resp.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "history $yearMonth fetch failed", e)
            emptyList()
        }
        cacheLock.withLock { monthCache[yearMonth] = CachedMonth(readings, System.currentTimeMillis()) }
        return readings
    }

    private fun monthsSpanning(startSec: Long, endSec: Long): List<String> {
        val startYm = YearMonth.from(Instant.ofEpochSecond(startSec).atZone(piZone))
        val endYm = YearMonth.from(Instant.ofEpochSecond(endSec).atZone(piZone))
        val out = ArrayList<String>()
        var ym = startYm
        while (!ym.isAfter(endYm)) {
            out.add(ym.toString())   // ISO "YYYY-MM"
            ym = ym.plusMonths(1)
        }
        return out
    }

    /** Parse the Pi CSV, resolving columns by header name (order-independent). */
    private fun parseCsv(text: String): List<SensorReading> {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return emptyList()
        val header = lines.first().split(',').map { it.trim() }
        fun idx(name: String) = header.indexOf(name)
        val iEpoch = idx("epoch")
        val iTemp = idx("temperature_c")
        val iHum = idx("humidity_pct")
        val iPress = idx("pressure_sealevel_mbar")
        val iDew = idx("dew_point_c")
        val iHeat = idx("heat_index_c")
        if (iEpoch < 0) return emptyList()

        return lines.drop(1).mapNotNull { line ->
            val c = line.split(',')
            val epoch = c.getOrNull(iEpoch)?.trim()?.toLongOrNull() ?: return@mapNotNull null
            fun d(i: Int) = if (i in 0 until c.size) c[i].trim().toDoubleOrNull() else null
            SensorReading(
                epochSec = epoch,
                temperatureC = d(iTemp),
                humidityPct = d(iHum),
                pressureMbar = d(iPress),
                dewPointC = d(iDew),
                heatIndexC = d(iHeat)
            )
        }
    }

    /** Average values into fixed time buckets to keep the plotted line readable. */
    private fun downsample(valued: List<Pair<Long, Double>>, bucketSeconds: Long): List<TrendPoint> {
        if (valued.isEmpty()) return emptyList()
        if (bucketSeconds <= 0L) return valued.map { TrendPoint(it.first, it.second) }
        val buckets = LinkedHashMap<Long, MutableList<Double>>()
        for ((epoch, v) in valued) {
            val key = epoch / bucketSeconds
            buckets.getOrPut(key) { ArrayList() }.add(v)
        }
        return buckets.map { (key, vs) ->
            TrendPoint(epochSec = key * bucketSeconds + bucketSeconds / 2, value = vs.average())
        }.sortedBy { it.epochSec }
    }

    companion object {
        private const val TAG = "GardenHistory"
        private const val CACHE_TTL_MS = 5L * 60 * 1000  // re-fetch a month at most every 5 min
        private const val TENDENCY_WINDOW_SEC = 3L * 60 * 60   // 3-hour barometric tendency window
    }
}
