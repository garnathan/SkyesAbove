package com.ganathan.skyesabove.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.ganathan.skyesabove.ui.util.WindDirectionUtil
import dagger.hilt.android.EntryPointAccessors
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

/** All display strings the widget renders. */
data class WidgetData(
    val place: String,
    // LEFT (forecast) — Skyes Above multi-source, for the phone's current location
    val areaTemp: String, val rain: String,
    val windValue: String, val windUnit: String,
    val todayEmoji: String,
    // RIGHT (HOME) — WU station IKILLI35
    val temp: String, val humidity: String,
    val feels: String, val feelsEmoji: String,
    val pressure: String
)

/** Builds a WidgetData: forecast from Skyes Above's WeatherRepository, home from the WU station. */
object WidgetRepo {

    private data class LocResult(val lat: Double, val lon: Double, val name: String)

    fun load(context: Context): WidgetData {
        // REAL-TIME ONLY: every value is either live-this-refresh or "—" (NO DATA). We
        // deliberately do NOT retain last-good values and cache NOTHING — a blank/"—"
        // field is the signal that the pipeline (Pi -> WU, or the forecast source) is
        // broken, which is exactly what the user wants to be able to see at a glance.
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )
        val loc = resolveLocation(context, ep)

        // LEFT half — live multi-source forecast for the current location; "—" on any failure.
        var areaTemp = NO_DATA; var rain = NO_DATA
        var windValue = NO_DATA; var windUnit = "km/h"; var todayEmoji = NO_DATA
        try {
            val cur = runBlocking { ep.weatherRepository().getWeather(loc.lat, loc.lon) }
                .getOrNull()?.current
            if (cur != null) {
                areaTemp = "${cur.temperature.roundToInt()}°"
                rain = formatPrecip(cur.precipitation)
                windValue = "${cur.windSpeed.roundToInt()}"
                windUnit = "km/h " + WindDirectionUtil.degreesToCompassDirection(cur.windDirection.toDouble())
                todayEmoji = conditionEmoji(cur.description, cur.symbol)
            } else {
                Log.w(TAG, "forecast returned null -> NO DATA")
            }
        } catch (e: Exception) {
            Log.w(TAG, "forecast fetch failed -> NO DATA", e)
        }

        // RIGHT half — the user's own WU HOME station. WuStation.fetch() THROWS unless the
        // observation is FRESH (WU keeps serving the last obs after the Pi dies), so a dead
        // pipeline shows "—" rather than a stuck stale reading.
        var temp = NO_DATA; var humidity = NO_DATA
        var feels = NO_DATA; var feelsEmoji = NO_DATA; var pressure = NO_DATA
        try {
            val h = WuStation.fetch()
            temp = h.temp; humidity = h.humidity; feels = h.feels; feelsEmoji = h.feelsEmoji; pressure = h.pressure
        } catch (e: Exception) {
            Log.w(TAG, "WU station stale/failed -> NO DATA", e)
        }

        return WidgetData(
            place = loc.name,
            areaTemp = areaTemp, rain = rain, windValue = windValue, windUnit = windUnit, todayEmoji = todayEmoji,
            temp = temp, humidity = humidity, feels = feels, feelsEmoji = feelsEmoji, pressure = pressure
        )
    }

    /** Follow the phone's location if we have it; else the app's stored location; else Killiney. */
    private fun resolveLocation(context: Context, ep: WidgetEntryPoint): LocResult {
        freshLocation(context)?.let {
            return LocResult(it.latitude, it.longitude, placeName(context, it.latitude, it.longitude))
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

/** The user's home Weather Underground PWS (IKILLI35) — the right half of the widget. */
object WuStation {
    private const val STATION = "IKILLI35"
    private const val STALE_SEC = 15L * 60L      // reject a WU obs older than 15 min => Pi likely down => NO DATA
    private val KEY = com.ganathan.skyesabove.BuildConfig.WU_API_KEY

    data class Home(
        val temp: String, val humidity: String,
        val feels: String, val feelsEmoji: String, val pressure: String
    )

    /** Fetch the CURRENT observation, but only if it is FRESH. WU keeps returning the last
     *  observation after the Pi stops uploading, so a stale obs (older than STALE_SEC) is
     *  rejected (throws) and the widget shows NO DATA instead of a stuck reading. A small
     *  retry rides out transient network blips. */
    fun fetch(): Home {
        val url = "https://api.weather.com/v2/pws/observations/current" +
            "?stationId=$STATION&format=json&units=m&apiKey=$KEY"
        var last: Exception? = null
        for (attempt in 0 until 2) {
            try {
                val obs = JSONObject(httpGet(url)).getJSONArray("observations").getJSONObject(0)
                // REAL-TIME GUARD: reject a stale observation (a dead Pi -> WU serves its last one).
                val obsEpoch = obs.optLong("epoch", 0L)
                val ageSec = System.currentTimeMillis() / 1000L - obsEpoch
                if (obsEpoch <= 0L || ageSec > STALE_SEC)
                    throw RuntimeException("stale WU obs (age=${ageSec}s > ${STALE_SEC}s)")
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
                    pressure = if (press.isNaN()) "—" else "${press.roundToInt()}"
                )
            } catch (e: Exception) {
                last = e
                if (attempt == 0) Thread.sleep(1500)
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
