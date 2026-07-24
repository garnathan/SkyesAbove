package com.ganathan.skyesabove.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import com.ganathan.skyesabove.data.repository.GardenHistoryRepository
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
    fun gardenHistoryRepository(): GardenHistoryRepository
}

/**
 * Home-screen widget: transparent 4x2. Left half = multi-source forecast for the phone's
 * current location (Skyes Above's WeatherRepository); right half = the WU home station.
 *
 * Refresh is driven by [WeatherSyncWorker] via WorkManager — a Doze-aware, network-gated,
 * self-healing ~15-min heartbeat that auto-recovers the moment connectivity returns (the old
 * inexact AlarmManager schedule was frozen for hours in Doze, which is what left the widget
 * blank while the phone was idle). Adding the widget / tapping it also kicks an immediate
 * refresh so it never waits on the heartbeat.
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WeatherSyncWorker.schedulePeriodic(context)
        renderImmediately(context, mgr, ids, "system_update")  // instant paint (don't wait on WorkManager)
        WeatherSyncWorker.refreshNow(context)                  // network-gated live refresh right away
    }

    override fun onEnabled(context: Context) = WeatherSyncWorker.schedulePeriodic(context)

    override fun onDisabled(context: Context) = WeatherSyncWorker.cancelAll(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            // Ensure the heartbeat exists (e.g. after reboot) and refresh now.
            WeatherSyncWorker.schedulePeriodic(context)
            val mgr = AppWidgetManager.getInstance(context)
            renderImmediately(context, mgr, mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java)), "refresh")
            WeatherSyncWorker.refreshNow(context)
        }
    }

    /**
     * Synchronous-ish paint off the broadcast, so a freshly-added widget shows data at once.
     *
     * We always attempt the live fetch. [WidgetRepo.load] pins the request to a validated network
     * (routing around a dead/captive Wi-Fi) and, if the phone is genuinely offline, the fetch
     * fails fast (a connect/DNS error in milliseconds, not a long timeout) and records the real
     * reason. We deliberately do NOT try to pre-decide "offline" here: the earlier check keyed on
     * getActiveNetwork(), which returns null in normal states (e.g. a fresh broadcast process),
     * and that false negative blanked the widget when the phone actually had working internet.
     *
     * But we do NOT blindly repaint a dash: the ~30-min `system_update` framework tick runs this
     * inline fetch on a background broadcast thread that Doze/App-Standby denies network access
     * to (even with Wi-Fi/cellular up), which was the dominant cause of "both halves vanish". When
     * the load shows that background-denial signature ([ImmediatePaintPolicy.suppressNoDataPaint]:
     * both halves unreachable yet a usable network was present) we leave the last good render in
     * place and let the network-capable WorkManager one-shot (kicked alongside this update) refresh
     * it, instead of flashing a dash. A genuine no-network outage still blanks (real-time-only).
     */
    private fun renderImmediately(context: Context, mgr: AppWidgetManager, ids: IntArray, trigger: String) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        Thread {
            try {
                val data = WidgetRepo.load(context, trigger)
                if (ImmediatePaintPolicy.suppressNoDataPaint(data.forecast, data.home, data.usableNetworkPresent)) {
                    // Background broadcast denied network access (Doze/standby) though a usable
                    // network exists — not a real outage. Keep the last render; the WorkManager
                    // one-shot already kicked will refresh with actual network access.
                    Log.i(TAG, "immediate paint suppressed ($trigger): background-denied fetch, usable network present — leaving last render for WorkManager")
                } else {
                    val views = WidgetRenderer.buildViews(context, data)
                    for (id in ids) mgr.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                Log.w(TAG, "immediate render failed -> NO DATA", e)
                val views = WidgetRenderer.buildNoData(context)
                for (id in ids) mgr.updateAppWidget(id, views)
            }
            pending.finish()
        }.start()
    }

    companion object {
        const val TAG = "SkyesWidget"
        const val ACTION_REFRESH = "com.ganathan.skyesabove.widget.REFRESH"
    }
}
