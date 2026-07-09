package com.ganathan.skyesabove.ui.screens.diagnostics

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganathan.skyesabove.widget.DiagEvent
import com.ganathan.skyesabove.widget.WeatherWidgetProvider
import com.ganathan.skyesabove.widget.WidgetDiagnostics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** UI state for the widget-diagnostics screen. */
data class DiagnosticsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val events: List<DiagEvent> = emptyList(),         // newest first
    val summary24h: WidgetDiagnostics.Summary? = null,
    val summary7d: WidgetDiagnostics.Summary? = null
)

/**
 * Reads the on-device widget refresh log ([WidgetDiagnostics]) and rolls it up into per-half
 * success rates plus a breakdown of the "both halves blank" cases by network and code path —
 * the numbers that answer why the widget loses data. Also lets the user force a live refresh
 * and watch a new event land, and clear the log.
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Re-read the log from disk and recompute the summaries. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = withContext(Dispatchers.IO) {
                val all = WidgetDiagnostics.readAll(context)
                val now = System.currentTimeMillis()
                Triple(
                    all,
                    WidgetDiagnostics.summarize(all, DAY_MS, now),
                    WidgetDiagnostics.summarize(all, 7 * DAY_MS, now)
                )
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    events = result.first.reversed(),
                    summary24h = result.second,
                    summary7d = result.third
                )
            }
        }
    }

    /**
     * Fire a live widget refresh (same broadcast the home-screen sends), wait for the async
     * refresh to record its event, then reload the log so the new row appears.
     */
    fun refreshWidgetNow() {
        _uiState.update { it.copy(refreshing = true) }
        context.sendBroadcast(
            Intent(context, WeatherWidgetProvider::class.java).setAction(WeatherWidgetProvider.ACTION_REFRESH)
        )
        viewModelScope.launch {
            delay(4000)   // let the (possibly slow) fetch finish and record
            load()
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    /** Wipe the log. */
    fun clear() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { WidgetDiagnostics.clear(context) }
            load()
        }
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
