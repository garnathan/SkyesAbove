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
        renderImmediately(context, mgr, ids)     // instant paint (don't wait on WorkManager)
        WeatherSyncWorker.refreshNow(context)    // network-gated live refresh right away
    }

    override fun onEnabled(context: Context) = WeatherSyncWorker.schedulePeriodic(context)

    override fun onDisabled(context: Context) = WeatherSyncWorker.cancelAll(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            // Ensure the heartbeat exists (e.g. after reboot) and refresh now.
            WeatherSyncWorker.schedulePeriodic(context)
            val mgr = AppWidgetManager.getInstance(context)
            renderImmediately(context, mgr, mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java)))
            WeatherSyncWorker.refreshNow(context)
        }
    }

    /** Synchronous-ish paint off the broadcast, so a freshly-added widget shows data at once. */
    private fun renderImmediately(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        Thread {
            val views = try {
                WidgetRenderer.buildViews(context, WidgetRepo.load(context))
            } catch (e: Exception) {
                Log.w(TAG, "immediate render failed -> NO DATA", e)
                WidgetRenderer.buildNoData(context)
            }
            for (id in ids) mgr.updateAppWidget(id, views)
            pending.finish()
        }.start()
    }

    companion object {
        const val TAG = "SkyesWidget"
        const val ACTION_REFRESH = "com.ganathan.skyesabove.widget.REFRESH"
    }
}
