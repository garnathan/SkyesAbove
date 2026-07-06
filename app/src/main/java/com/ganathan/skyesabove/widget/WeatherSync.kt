package com.ganathan.skyesabove.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ganathan.skyesabove.MainActivity
import com.ganathan.skyesabove.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "SkyesWidget"

/**
 * Renders the widget from a freshly-loaded [WidgetData] and pushes it to every mounted instance.
 * Shared by [WeatherWidgetProvider] (immediate updates on add / tap) and [WeatherSyncWorker]
 * (the scheduled, self-healing refresh) so both paths render identically.
 */
object WidgetRenderer {

    /**
     * Load live data and push it to all widget instances.
     * @return the [WidgetData] that was rendered (its per-half [SourceStatus]es drive retry).
     */
    fun renderNow(context: Context): WidgetData {
        val data = WidgetRepo.load(context)
        push(context, buildViews(context, data))
        return data
    }

    /** Push an explicit view to all widget instances (used for the error fallback too). */
    fun push(context: Context, views: RemoteViews) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
        for (id in ids) mgr.updateAppWidget(id, views)
    }

    fun buildViews(context: Context, d: WidgetData): RemoteViews {
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

        // "!" flag per half: shown whenever that side is NOT live this refresh (stale or
        // unreachable). Values still show "—"; the "!" makes the failure obvious at a glance
        // and tells you WHICH source is down — e.g. both flagged => phone-side, not the Pi.
        v.setViewVisibility(R.id.fc_warn, if (d.forecast == SourceStatus.OK) View.GONE else View.VISIBLE)
        v.setViewVisibility(R.id.home_warn, if (d.home == SourceStatus.OK) View.GONE else View.VISIBLE)

        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        v.setOnClickPendingIntent(R.id.widget_root, pi)
        return v
    }

    /** All-"—" view with both "!" flags raised. Shown only if a render throws unexpectedly. */
    fun buildNoData(context: Context): RemoteViews = buildViews(
        context,
        WidgetData(
            place = "—",
            areaTemp = "—", rain = "—", windValue = "—", windUnit = "km/h", todayEmoji = "—",
            temp = "—", humidity = "—", feels = "—", feelsEmoji = "—", pressure = "—",
            forecast = SourceStatus.FETCH_ERROR, home = SourceStatus.FETCH_ERROR
        )
    )
}

/**
 * Doze-aware, self-healing widget refresh. Replaces the old inexact `ELAPSED_REALTIME`
 * AlarmManager schedule, which was silently deferred for hours while the phone was idle
 * (Doze) — so a single failed fetch could leave the widget blank until the phone woke.
 *
 * WorkManager instead:
 *  - runs on a ~15-min periodic heartbeat, batched into Doze maintenance windows;
 *  - is gated on [NetworkType.CONNECTED], so it never fires a doomed no-network refresh and
 *    runs the moment connectivity returns;
 *  - on a reachability failure returns [Result.retry] with exponential backoff, so a blank
 *    self-heals within seconds–minutes instead of waiting for the next heartbeat;
 *  - survives reboot (WorkManager reschedules its own persisted work).
 */
class WeatherSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val data = WidgetRenderer.renderNow(applicationContext)
            // A FETCH_ERROR means we couldn't reach a source (network) — retry fast so the
            // widget recovers as soon as signal is back. STALE (Pi down) or OK => done; the
            // periodic heartbeat will pick up recovery without hammering the dead sensor.
            if (data.anyFetchError) {
                Log.i(TAG, "worker: fetch error this cycle -> Result.retry() (self-heal)")
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.w(TAG, "worker: render crashed -> Result.retry()", e)
            runCatching { WidgetRenderer.push(applicationContext, WidgetRenderer.buildNoData(applicationContext)) }
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC = "skyes_widget_periodic"
        private const val ONESHOT = "skyes_widget_now"
        private val NETWORK = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Ensure the ~15-min network-gated heartbeat is scheduled (idempotent). */
        fun schedulePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<WeatherSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(NETWORK)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req)
            Log.i(TAG, "scheduled periodic refresh (15m, network-gated)")
        }

        /** Kick an immediate one-shot refresh (on add / tap / connectivity change). */
        fun refreshNow(context: Context) {
            val req = OneTimeWorkRequestBuilder<WeatherSyncWorker>()
                .setConstraints(NETWORK)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONESHOT, ExistingWorkPolicy.REPLACE, req)
        }

        /** Cancel everything (last widget removed). */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(ONESHOT)
        }
    }
}
