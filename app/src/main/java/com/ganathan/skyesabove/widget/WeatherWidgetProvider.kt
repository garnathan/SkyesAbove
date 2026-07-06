package com.ganathan.skyesabove.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.ganathan.skyesabove.MainActivity
import com.ganathan.skyesabove.R
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import com.ganathan.skyesabove.data.repository.WeatherRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the (non-Hilt) widget reach the app's singleton graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun weatherRepository(): WeatherRepository
    fun settingsDataStore(): SettingsDataStore
}

/**
 * Home-screen widget: transparent 4x2. Left half = multi-source forecast for the phone's
 * current location (Skyes Above's WeatherRepository); right half = the WU home station.
 * Refreshes on a ~15-min alarm (re-fetching a fresh location each cycle) + tap-to-open.
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) =
        refresh(context, mgr, ids)

    override fun onEnabled(context: Context) = scheduleAlarm(context)

    override fun onDisabled(context: Context) = cancelAlarm(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            refresh(context, mgr, mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java)))
        }
    }

    private fun refresh(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        Thread {
            val views = try {
                // load() fetches AND persists last-good per-source (no cache poisoning on failure)
                buildViews(context, WidgetRepo.load(context))
            } catch (e: Exception) {
                buildViewsFromCache(context)
            }
            for (id in ids) mgr.updateAppWidget(id, views)
            pending.finish()
        }.start()
    }

    private fun buildViews(context: Context, d: WidgetData): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_weather)
        v.setTextViewText(R.id.place_lbl, d.place.uppercase())
        v.setTextViewText(R.id.area_temp, d.areaTemp)
        v.setTextViewText(R.id.rain, d.rain)
        v.setTextViewText(R.id.wind, d.windValue)
        v.setTextViewText(R.id.wind_unit, d.windUnit)
        v.setTextViewText(R.id.cond_ico, d.todayEmoji)
        v.setTextViewText(R.id.temp, d.temp)
        v.setTextViewText(R.id.humidity, d.humidity)
        v.setTextViewText(R.id.feels_ico, d.feelsEmoji)
        v.setTextViewText(R.id.feels, d.feels)
        v.setTextViewText(R.id.pressure, d.pressure)

        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        v.setOnClickPendingIntent(R.id.widget_root, pi)
        return v
    }

    private fun buildViewsFromCache(context: Context): RemoteViews {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        fun g(k: String, dft: String): String = p.getString("c_$k", dft) ?: dft
        return buildViews(
            context,
            WidgetData(
                place = g("place", "—"),
                areaTemp = g("areaTemp", "—"), rain = g("rain", "—"),
                windValue = g("windValue", "—"), windUnit = g("windUnit", "km/h"),
                todayEmoji = g("todayEmoji", "⛅"),
                temp = g("temp", "—"), humidity = g("humidity", "—"),
                feels = g("feels", "—"), feelsEmoji = g("feelsEmoji", "🌡️"),
                pressure = g("pressure", "—")
            )
        )
    }

    private fun alarmPi(context: Context): PendingIntent {
        val i = Intent(context, WeatherWidgetProvider::class.java).setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context, 1, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun scheduleAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 30_000L,
            15 * 60_000L, // ~15 min, battery-friendly; re-fetches a fresh location each cycle
            alarmPi(context)
        )
    }

    private fun cancelAlarm(context: Context) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(alarmPi(context))
    }

    companion object {
        const val ACTION_REFRESH = "com.ganathan.skyesabove.widget.REFRESH"
        const val PREFS = "skyes_widget"
    }
}
