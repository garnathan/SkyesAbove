package com.ganathan.skyesabove.ui.screens.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganathan.skyesabove.data.model.TrendMetric
import com.ganathan.skyesabove.data.model.TrendRange
import com.ganathan.skyesabove.data.model.TrendSeries
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import com.ganathan.skyesabove.data.preferences.TemperatureUnit
import com.ganathan.skyesabove.data.repository.GardenHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the garden-sensor Trends screen. */
data class TrendsUiState(
    val metric: TrendMetric = TrendMetric.TEMPERATURE,
    val range: TrendRange = TrendRange.DAY,
    val isLoading: Boolean = true,
    val error: String? = null,
    val series: TrendSeries? = null
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val repository: GardenHistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectMetric(metric: TrendMetric) {
        if (metric == _uiState.value.metric) return
        _uiState.update { it.copy(metric = metric) }
        load()
    }

    fun selectRange(range: TrendRange) {
        if (range == _uiState.value.range) return
        _uiState.update { it.copy(range = range) }
        load()
    }

    fun refresh() = load()

    private fun load() {
        val metric = _uiState.value.metric
        val range = _uiState.value.range
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val unit = runCatching { settingsDataStore.temperatureUnit.first() }
                    .getOrDefault(TemperatureUnit.CELSIUS)
                val series = repository.getTrend(metric, range, unit)
                // Ignore a stale result if the user switched selection while we were loading.
                if (_uiState.value.metric == metric && _uiState.value.range == range) {
                    _uiState.update { it.copy(isLoading = false, error = null, series = series) }
                }
            } catch (e: Exception) {
                if (_uiState.value.metric == metric && _uiState.value.range == range) {
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't load garden history")
                    }
                }
            }
        }
    }
}
