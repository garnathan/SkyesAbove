package com.ganathan.skyesabove.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.ganathan.skyesabove.data.model.PressureTendency
import com.ganathan.skyesabove.data.preferences.UserSettings
import com.ganathan.skyesabove.ui.util.WindDirectionUtil
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TAG = "SkyesWidget"
private const val NO_DATA = "—"   // shown whenever a value is NOT live/fresh (never stale cache)

/**
 * Outcome of fetching one half of the widget.
 *  - OK          : live data obtained this refresh.
 *  - STALE       : the source WAS reached, but its data is genuinely out of date
 *                  (e.g. the WU obs is older than the freshness window => the Pi is down).
 *  - FETCH_ERROR : we could not reach the source at all (network gap, timeout, HTTP error).
 *
 * The display treats STALE and FETCH_ERROR identically ("—" + a "!" flag, never a stale value),
 * but the *scheduler* uses the distinction: a FETCH_ERROR is worth retrying fast (it heals the
 * moment connectivity returns), whereas STALE means the upstream sensor is down and hammering it
 * won't help — the normal periodic cadence will pick it up when the sensor recovers.
 */
enum class SourceStatus { OK, STALE, FETCH_ERROR }

/** All display strings the widget renders, plus the per-half fetch outcomes. */
data class WidgetData(
    val place: String,
    // LEFT (forecast) — Skyes Above multi-source, for the phone's current location
    val areaTemp: String, val rain: String,
    val windValue: String, val windUnit: String,
    val todayEmoji: String,
    // RIGHT (HOME) — WU station IKILLI35
    val temp: String, val humidity: String,
    val feels: String, val feelsEmoji: String,
    val pressure: String,
    // 3-hour barometric tendency arrow next to the pressure (null => hide).
    val pressureTendency: PressureTendency? = null,
    // Per-half outcomes: drive the "!" indicators and the scheduler's retry decision.
    val forecast: SourceStatus = SourceStatus.OK,
    val home: SourceStatus = SourceStatus.OK
) {
    /** True if either half could not be REACHED this cycle (network) — worth a fast retry. */
    val anyFetchError: Boolean
        get() = forecast == SourceStatus.FETCH_ERROR || home == SourceStatus.FETCH_ERROR
}

/** Builds a WidgetData: forecast from Skyes Above's WeatherRepository, home from the WU station. */
object WidgetRepo {

    private data class LocResult(val lat: Double, val lon: Double, val name: String)

    /** Result of fetching the LEFT (forecast) half. */
    private data class ForecastResult(
        val areaTemp: String, val rain: String,
        val windValue: String, val windUnit: String, val todayEmoji: String,
        val status: SourceStatus, val error: String?
    ) {
        companion object {
            fun blank(status: SourceStatus, error: String?) =
                ForecastResult(NO_DATA, NO_DATA, NO_DATA, "km/h", NO_DATA, status, error)
        }
    }

    /** Result of fetching the RIGHT (WU home station) half. */
    private data class HomeResult(
        val temp: String, val humidity: String,
        val feels: String, val feelsEmoji: String, val pressure: String,
        val status: SourceStatus, val obsAgeSec: Long, val error: String?
    ) {
        companion object {
            fun blank(status: SourceStatus, obsAgeSec: Long, error: String?) =
                HomeResult(NO_DATA, NO_DATA, NO_DATA, NO_DATA, NO_DATA, status, obsAgeSec, error)
        }
    }

    /**
     * @param trigger which code path drove this refresh (system_update / refresh / periodic /
     *   oneshot) — recorded in [WidgetDiagnostics] so we can later tell, with no adb, whether a
     *   double-blank was the un-network-gated system update firing offline or a genuine
     *   live-network failure. Defaults are harmless if a caller doesn't set it.
     */
    fun load(context: Context, trigger: String = "unknown"): WidgetData {
        val startMs = System.currentTimeMillis()
        // REAL-TIME ONLY: every value is either live-this-refresh or "—" (NO DATA). We
        // deliberately do NOT retain last-good values and cache NOTHING — a blank/"—"
        // field is the signal that the pipeline (Pi -> WU, or the forecast source) is
        // broken, which is exactly what the user wants to be able to see at a glance.
        // When a half is not live we additionally raise a "!" flag (see SourceStatus) so the
        // failure is obvious at a glance, and the scheduler keeps retrying until it heals.
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )

        // ROOT-CAUSE FIX for "both halves vanish together": when the phone is associated with a
        // connected-but-dead network (a captive / not-yet-validated Wi-Fi whose DNS is broken),
        // the app resolves every host against that dead network and ALL fetches fail — even
        // though cellular is up and validated. Pin this refresh's connections to a network that
        // the system has actually VALIDATED as having working internet, routing around the dead
        // one. Costs nothing when the default is already good (fast path returns it); no polling.
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val validated = pickValidatedNetwork(cm)
        val bound = validated != null && (cm?.bindProcessToNetwork(validated) ?: false)
        try {
            val loc = resolveLocation(context, ep)

            // Which WU station drives the HOME half — configurable in Settings so the app isn't
            // hard-wired to the author's garden PWS. Defaults to the garden station (IKILLI35).
            val stationId = runCatching {
                runBlocking { ep.settingsDataStore().homeStationId.first() }
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: UserSettings.DEFAULT_HOME_STATION

            // The two halves are independent network fetches — run them CONCURRENTLY so a refresh
            // finishes in about max(forecast, WU) instead of their sum. Same requests, same data,
            // just not serialised: less wall-clock, less wake time, and far less chance of the
            // immediate (broadcast) path overrunning its budget. No extra battery cost.
            val (fc, home) = runBlocking(Dispatchers.IO) {
                val fcDef = async { fetchForecast(ep, loc) }
                val homeDef = async { fetchHome(stationId) }
                fcDef.await() to homeDef.await()
            }

            // 3-hour barometric tendency (arrow next to pressure). Only meaningful when the home
            // pressure is live AND we're pointed at the garden station — the tendency is computed
            // from the garden Pi's GitHub history, which is meaningless for someone else's PWS.
            val isGardenStation = stationId.equals(UserSettings.DEFAULT_HOME_STATION, ignoreCase = true)
            val pressureTendency = if (home.status == SourceStatus.OK && isGardenStation) {
                runCatching { runBlocking { ep.gardenHistoryRepository().pressureTendency() } }.getOrNull()
            } else null

            val net = networkState(context)
            val err = listOfNotNull(
                fc.error?.let { "fc:$it" },
                home.error?.let { "hm:$it" }
            ).joinToString("; ").ifBlank { null }

            WidgetDiagnostics.record(
                context,
                DiagEvent(
                    timeMs = startMs, trigger = trigger, net = net,
                    forecast = fc.status.name, home = home.status.name, homeObsAgeSec = home.obsAgeSec,
                    durationMs = System.currentTimeMillis() - startMs, place = loc.name, error = err
                )
            )
            Log.i(
                TAG,
                "refresh done trig=$trigger net=$net bound=$bound place='${loc.name}' " +
                    "forecast=${fc.status} home=${home.status} obsAge=${home.obsAgeSec}s tendency=$pressureTendency"
            )

            return WidgetData(
                place = loc.name,
                areaTemp = fc.areaTemp, rain = fc.rain, windValue = fc.windValue, windUnit = fc.windUnit, todayEmoji = fc.todayEmoji,
                temp = home.temp, humidity = home.humidity, feels = home.feels, feelsEmoji = home.feelsEmoji, pressure = home.pressure,
                pressureTendency = pressureTendency,
                forecast = fc.status, home = home.status
            )
        } finally {
            // Always release the binding so we don't pin the process to a network that may later
            // go away; the next refresh re-picks. bindProcessToNetwork(null) restores the default.
            if (bound) runCatching { cm?.bindProcessToNetwork(null) }
        }
    }

    /**
     * The best currently-connected network that the system has VALIDATED as having real internet
     * (and that advertises INTERNET), or null if none. Used to route a refresh around a
     * connected-but-dead network (captive/broken Wi-Fi) that would otherwise fail DNS for every
     * host. Fast path: the active default if it's already validated; otherwise scan all networks
     * (this is how we find validated cellular when the phone is sitting on a dead Wi-Fi).
     */
    private fun pickValidatedNetwork(cm: ConnectivityManager?): Network? {
        cm ?: return null
        fun validatedInternet(n: Network?): Boolean {
            val c = n?.let { cm.getNetworkCapabilities(it) } ?: return false
            return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        cm.activeNetwork?.let { if (validatedInternet(it)) return it }
        @Suppress("DEPRECATION")
        return cm.allNetworks.firstOrNull { validatedInternet(it) }
    }

    /** LEFT half — live multi-source forecast for the current location; "—" on any failure. */
    private suspend fun fetchForecast(ep: WidgetEntryPoint, loc: LocResult): ForecastResult {
        return try {
            val result = ep.weatherRepository().getWeather(loc.lat, loc.lon)
            val res = result.getOrNull()
            val cur = res?.current
            if (cur != null) {
                // Show "now/high" — current temp + today's forecast high (e.g. 23/25°),
                // so a glance gives both what it is now and where it's heading today.
                val now = cur.temperature.roundToInt()
                val today = java.time.LocalDate.now().toString()   // yyyy-MM-dd
                val high = res.hourly
                    .filter { it.time.startsWith(today) }
                    .maxOfOrNull { it.temperature }
                    ?.roundToInt()
                ForecastResult(
                    areaTemp = if (high != null && high >= now) "$now/$high°" else "$now°",
                    rain = formatPrecip(cur.precipitation),
                    windValue = "${cur.windSpeed.roundToInt()}",
                    windUnit = "km/h " + WindDirectionUtil.degreesToCompassDirection(cur.windDirection.toDouble()),
                    todayEmoji = conditionEmoji(cur.description, cur.symbol),
                    status = SourceStatus.OK, error = null
                )
            } else {
                // All public sources returned nothing. Surface WHY (the per-source reason built
                // by WeatherRepository — e.g. "UnknownHostException" from a dead-Wi-Fi DNS) so
                // the diagnostics log is self-explanatory without needing logcat.
                val reason = result.exceptionOrNull()?.message ?: "no current data"
                Log.w(TAG, "forecast unavailable -> NO DATA (!): $reason")
                ForecastResult.blank(SourceStatus.FETCH_ERROR, reason)
            }
        } catch (e: Exception) {
            Log.w(TAG, "forecast fetch failed -> NO DATA (!)", e)
            ForecastResult.blank(SourceStatus.FETCH_ERROR, e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * RIGHT half — the user's own WU HOME station. WuStation.fetch() THROWS unless the
     * observation is FRESH (WU keeps serving the last obs after the Pi dies), so a dead
     * pipeline shows "—" rather than a stuck stale reading.
     */
    private suspend fun fetchHome(stationId: String): HomeResult {
        return try {
            val h = WuStation.fetch(stationId)
            HomeResult(h.temp, h.humidity, h.feels, h.feelsEmoji, h.pressure, SourceStatus.OK, h.obsAgeSec, null)
        } catch (e: WuStation.StaleObsException) {
            // Reached WU, but the observation is old => the sensor/pipeline is down.
            Log.w(TAG, "WU obs stale (${e.ageSec}s) -> NO DATA (!) — station likely down")
            HomeResult.blank(SourceStatus.STALE, e.ageSec, "stale ${e.ageSec}s")
        } catch (e: Exception) {
            Log.w(TAG, "WU station unreachable -> NO DATA (!)", e)
            HomeResult.blank(SourceStatus.FETCH_ERROR, -1L, e.message ?: e.javaClass.simpleName)
        }
    }

    /** Coarse connectivity label for the diagnostic log (WIFI / CELLULAR / OTHER / NONE). */
    fun networkState(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "UNKNOWN"
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "NONE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    /**
     * Resolve the location to report for, in priority order:
     *   1. A fresh live fix (and persist it as the new last-good).
     *   2. The last SUCCESSFULLY-resolved fix (self-heals a transient GPS/permission failure).
     *   3. The app's stored/settings location.
     *   4. Killiney (hard default).
     * Note: this caches *location*, never weather values.
     */
    private fun resolveLocation(context: Context, ep: WidgetEntryPoint): LocResult {
        freshLocation(context)?.let {
            val name = placeName(context, it.latitude, it.longitude)
            runCatching {
                runBlocking { ep.settingsDataStore().setLastKnownLocation(it.latitude, it.longitude, name) }
            }
            return LocResult(it.latitude, it.longitude, name)
        }
        // No live fix — reuse the last good one before falling back to defaults.
        runCatching { runBlocking { ep.settingsDataStore().lastKnownLocation.first() } }.getOrNull()?.let {
            Log.i(TAG, "no live fix -> reusing last-good location '${it.name}'")
            return LocResult(it.latitude, it.longitude, it.name)
        }
        return try {
            val s = runBlocking { ep.settingsDataStore().settings.first() }
            LocResult(s.latitude, s.longitude, placeName(context, s.latitude, s.longitude))
        } catch (_: Exception) {
            LocResult(53.2631, -6.1083, "Killiney")
        }
    }

    /**
     * Actively acquire a fresh fix each refresh cycle (not just the cached last-known),
     * so the widget follows the phone as it travels. Falls back to last-known on timeout.
     */
    private fun freshLocation(context: Context): Location? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = when {
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider != null) {
                val holder = arrayOfNulls<Location>(1)
                val latch = CountDownLatch(1)
                try {
                    lm.getCurrentLocation(provider, null, context.mainExecutor) { loc ->
                        holder[0] = loc
                        latch.countDown()
                    }
                    latch.await(8, TimeUnit.SECONDS)
                } catch (_: Exception) {
                }
                holder[0]?.let { return it }
            }
        }
        return lastKnown(context)
    }

    private fun lastKnown(context: Context): Location? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var best: Location? = null
        for (p in listOf(
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        )) {
            try {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (best == null || l.time > best.time) best = l
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
        return best
    }

    @Suppress("DEPRECATION")
    private fun placeName(context: Context, lat: Double, lon: Double): String = try {
        val a = Geocoder(context).getFromLocation(lat, lon, 1)?.firstOrNull()
        a?.locality ?: a?.subAdminArea ?: a?.adminArea ?: "Current location"
    } catch (_: Exception) {
        "Current location"
    }

    private fun formatPrecip(mm: Double): String = when {
        mm < 0.05 -> "0mm"
        mm < 10.0 -> String.format("%.1fmm", mm)
        else -> "${mm.roundToInt()}mm"
    }

    /** Cross-source condition -> emoji. Uses the (always-populated) description, with an OWM-code fallback. */
    private fun conditionEmoji(desc: String?, symbol: String?): String {
        val d = (desc ?: "").lowercase()
        when {
            d.contains("thunder") -> return "⛈️"
            d.contains("snow") || d.contains("sleet") || d.contains("flurr") -> return "❄️"
            d.contains("rain") || d.contains("drizzle") || d.contains("shower") -> return "🌧️"
            d.contains("fog") || d.contains("mist") || d.contains("haze") -> return "🌫️"
            d.contains("overcast") -> return "☁️"
            d.contains("partly") || d.contains("few cloud") || d.contains("scattered") || d.contains("broken") -> return "⛅"
            d.contains("cloud") -> return "☁️"
            d.contains("clear") || d.contains("sunny") || d.contains("sun") -> return "☀️"
        }
        val s = (symbol ?: "").lowercase()
        return when {
            s.startsWith("01") -> "☀️"
            s.startsWith("02") -> "🌤️"
            s.startsWith("03") -> "⛅"
            s.startsWith("04") -> "☁️"
            s.startsWith("09") || s.startsWith("10") -> "🌧️"
            s.startsWith("11") -> "⛈️"
            s.startsWith("13") -> "❄️"
            s.startsWith("50") -> "🌫️"
            s.contains("clear") || s.contains("sun") -> "☀️"
            s.contains("partly") -> "⛅"
            s.contains("rain") || s.contains("drizzle") -> "🌧️"
            s.contains("snow") -> "❄️"
            s.contains("thunder") -> "⛈️"
            s.contains("fog") -> "🌫️"
            s.contains("cloud") -> "☁️"
            else -> "⛅"
        }
    }
}

/** The user's home Weather Underground PWS — the right half of the widget. The station ID is
 *  configurable (Settings), defaulting to the author's garden station (IKILLI35). */
object WuStation {
    private const val STALE_SEC = 15L * 60L      // reject a WU obs older than 15 min => sensor likely down => NO DATA
    private val KEY = com.ganathan.skyesabove.BuildConfig.WU_API_KEY

    data class Home(
        val temp: String, val humidity: String,
        val feels: String, val feelsEmoji: String, val pressure: String,
        /** Age of the observation in seconds at fetch time — recorded for diagnostics. */
        val obsAgeSec: Long
    )

    /** Thrown when WU IS reachable but the latest observation is older than the freshness
     *  window — i.e. the sensor stopped uploading. Distinct from a network/HTTP failure so the
     *  scheduler can tell "sensor down" (don't hammer) from "unreachable" (retry fast). */
    class StaleObsException(val ageSec: Long) : RuntimeException("stale WU obs (age=${ageSec}s > ${STALE_SEC}s)")

    /** Fetch the CURRENT observation for [stationId], but only if it is FRESH. WU keeps
     *  returning the last observation after the sensor stops uploading, so a stale obs (older
     *  than STALE_SEC) is rejected (throws StaleObsException) and the widget shows NO DATA
     *  instead of a stuck reading. A small retry rides out transient network blips. */
    fun fetch(stationId: String): Home {
        val url = "https://api.weather.com/v2/pws/observations/current" +
            "?stationId=$stationId&format=json&units=m&apiKey=$KEY"
        var last: Exception? = null
        for (attempt in 0 until 2) {
            try {
                val obs = JSONObject(httpGet(url)).getJSONArray("observations").getJSONObject(0)
                // REAL-TIME GUARD: reject a stale observation (a dead sensor -> WU serves its last one).
                val obsEpoch = obs.optLong("epoch", 0L)
                val ageSec = System.currentTimeMillis() / 1000L - obsEpoch
                if (obsEpoch <= 0L || ageSec > STALE_SEC)
                    throw StaleObsException(ageSec)
                val m = obs.getJSONObject("metric")
                val t = m.getDouble("temp")
                val heat = m.optDouble("heatIndex", t)
                val chill = m.optDouble("windChill", t)
                val feelsV = if (t <= 10.0 && !chill.isNaN()) chill else if (!heat.isNaN()) heat else t
                val hum = obs.getInt("humidity")
                val press = m.optDouble("pressure", Double.NaN)
                val feelsEmoji = when {
                    feelsV >= 27.0 -> "🥵"
                    feelsV <= 4.0 -> "🥶"
                    else -> "🌡️"
                }
                return Home(
                    temp = "${t.roundToInt()}°",
                    humidity = "$hum%",
                    feels = "${feelsV.roundToInt()}°",
                    feelsEmoji = feelsEmoji,
                    pressure = if (press.isNaN()) "—" else "${press.roundToInt()}",
                    obsAgeSec = ageSec
                )
            } catch (e: StaleObsException) {
                throw e   // reaching WU worked; retrying won't un-stale the obs — surface it now
            } catch (e: Exception) {
                last = e
                if (attempt == 0) Thread.sleep(1500)   // ride out a transient network blip
            }
        }
        throw last ?: RuntimeException("WU fetch failed")
    }

    private fun httpGet(urlStr: String): String {
        val c = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000; readTimeout = 12000; requestMethod = "GET"
        }
        try {
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            return body
        } finally {
            c.disconnect()
        }
    }
}
