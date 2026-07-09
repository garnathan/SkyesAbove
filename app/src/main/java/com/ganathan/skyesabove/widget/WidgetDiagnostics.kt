package com.ganathan.skyesabove.widget

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

private const val TAG = "SkyesWidget"

/**
 * One recorded widget-refresh outcome. Everything the widget needs to explain — after the
 * fact, with no laptop/adb attached — *why* a value was blank at a given moment.
 *
 * The two fields that matter most for the "both halves vanish together" symptom are [trigger]
 * (which code path ran the refresh) and [net] (what the connectivity looked like at that
 * instant): a double-blank on `trigger=system_update net=NONE` is the un-network-gated system
 * widget update firing while the radios are off in Doze; a double-blank on `trigger=periodic
 * net=WIFI` is a genuine live-network failure and a different problem entirely.
 */
data class DiagEvent(
    val timeMs: Long,
    /** Which path drove this refresh: system_update | refresh | periodic | oneshot. */
    val trigger: String,
    /** Connectivity at refresh time: WIFI | CELLULAR | ETHERNET | OTHER | NONE | UNKNOWN. */
    val net: String,
    /** Forecast (left) half outcome — [SourceStatus] name. */
    val forecast: String,
    /** Home (right / WU station) half outcome — [SourceStatus] name. */
    val home: String,
    /** Age of the WU observation in seconds, or -1 when it couldn't be determined. */
    val homeObsAgeSec: Long,
    /** Wall-clock the refresh took, milliseconds. */
    val durationMs: Long,
    /** The location the refresh reported for. */
    val place: String,
    /** Short failure reason, or null on a clean cycle. */
    val error: String?
) {
    /** True when NEITHER half was live this cycle — the "both vanish together" case. */
    val bothBlank: Boolean get() = forecast != "OK" && home != "OK"
}

/**
 * A persistent, on-device rolling log of widget refreshes.
 *
 * Why this exists: the widget loses data intermittently *in the field* (while the phone is out
 * of reach of the laptop), and logcat is a volatile ring buffer that needs adb and is gone by
 * the time you look. This writes a capped ring buffer to app-private storage instead, so the
 * evidence survives reboots and is viewable from inside the app (Settings -> Widget diagnostics)
 * with no adb at all.
 *
 * Battery cost is negligible: a single small line is appended at the end of each refresh cycle
 * (which already happens roughly every 15 minutes plus on interaction). There is no polling, no
 * wake-lock, and no extra network — it only records what a refresh already computed.
 */
object WidgetDiagnostics {
    private const val FILE = "widget_diag.log"
    private const val MAX_ENTRIES = 300      // ~2-3 days at the 15-min cadence; tens of KB
    private val lock = Any()

    /** Append one refresh outcome, trimming the ring buffer to the newest [MAX_ENTRIES]. */
    fun record(context: Context, event: DiagEvent) {
        synchronized(lock) {
            try {
                val f = file(context)
                val lines = if (f.exists()) f.readLines().toMutableList() else mutableListOf()
                lines.add(encode(event))
                val trimmed = if (lines.size > MAX_ENTRIES) lines.takeLast(MAX_ENTRIES) else lines
                f.writeText(trimmed.joinToString("\n"))
            } catch (e: Exception) {
                // Diagnostics must never break a refresh — swallow and fall back to logcat.
                Log.w(TAG, "diag record failed", e)
            }
        }
    }

    /** All recorded events, oldest first. Empty if nothing has been recorded yet. */
    fun readAll(context: Context): List<DiagEvent> = synchronized(lock) {
        try {
            val f = file(context)
            if (!f.exists()) return emptyList()
            f.readLines().mapNotNull { decode(it) }
        } catch (e: Exception) {
            Log.w(TAG, "diag read failed", e)
            emptyList()
        }
    }

    /** Wipe the log (user-initiated from the diagnostics screen). */
    fun clear(context: Context) {
        synchronized(lock) { runCatching { file(context).delete() } }
    }

    private fun file(context: Context) = File(context.filesDir, FILE)

    private fun encode(e: DiagEvent): String = JSONObject().apply {
        put("t", e.timeMs)
        put("trig", e.trigger)
        put("net", e.net)
        put("fc", e.forecast)
        put("hm", e.home)
        put("age", e.homeObsAgeSec)
        put("dur", e.durationMs)
        put("pl", e.place)
        put("err", e.error ?: JSONObject.NULL)
    }.toString()

    private fun decode(line: String): DiagEvent? = try {
        val o = JSONObject(line)
        DiagEvent(
            timeMs = o.optLong("t"),
            trigger = o.optString("trig", "?"),
            net = o.optString("net", "?"),
            forecast = o.optString("fc", "?"),
            home = o.optString("hm", "?"),
            homeObsAgeSec = o.optLong("age", -1L),
            durationMs = o.optLong("dur", -1L),
            place = o.optString("pl", ""),
            error = if (o.isNull("err")) null else o.optString("err").ifBlank { null }
        )
    } catch (_: Exception) {
        null
    }

    /**
     * Roll the raw events up into the numbers that actually answer "why is it blank so often":
     * per-half success rate and, crucially, how the *both-blank* cases break down by which code
     * path ran and what the network was — so the dominant failure mode is obvious at a glance.
     */
    fun summarize(events: List<DiagEvent>, windowMs: Long, nowMs: Long): Summary {
        val since = nowMs - windowMs
        val w = events.filter { it.timeMs >= since }
        val total = w.size
        val fcOk = w.count { it.forecast == "OK" }
        val homeOk = w.count { it.home == "OK" }
        val both = w.filter { it.bothBlank }
        val bothByNet = both.groupingBy { it.net }.eachCount()
        val bothByTrigger = both.groupingBy { it.trigger }.eachCount()
        return Summary(
            total = total,
            forecastOk = fcOk,
            homeOk = homeOk,
            bothBlank = both.size,
            bothBlankByNet = bothByNet,
            bothBlankByTrigger = bothByTrigger
        )
    }

    data class Summary(
        val total: Int,
        val forecastOk: Int,
        val homeOk: Int,
        val bothBlank: Int,
        val bothBlankByNet: Map<String, Int>,
        val bothBlankByTrigger: Map<String, Int>
    ) {
        fun forecastPct(): Int = if (total == 0) 0 else (forecastOk * 100) / total
        fun homePct(): Int = if (total == 0) 0 else (homeOk * 100) / total
    }
}
