package com.ganathan.skyesabove.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import com.ganathan.skyesabove.data.preferences.TemperatureUnit
import com.ganathan.skyesabove.data.preferences.ThemeMode
import com.ganathan.skyesabove.data.preferences.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val latitude: String = "",
    val longitude: String = "",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val homeStationId: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Preset locations for quick selection.
 */
data class PresetLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * ViewModel for the Settings screen.
 *
 * Manages user preferences including location coordinates and temperature unit.
 * Uses Hilt for dependency injection and DataStore for persistence.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Preset Irish locations for quick selection.
     */
    val presetLocations = listOf(
        PresetLocation("Dublin", 53.3498, -6.2603),
        PresetLocation("Cork", 51.8985, -8.4756),
        PresetLocation("Galway", 53.2707, -9.0568)
    )

    init {
        loadSettings()
    }

    /**
     * Loads current settings from DataStore.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        latitude = settings.latitude.toString(),
                        longitude = settings.longitude.toString(),
                        temperatureUnit = settings.temperatureUnit,
                        themeMode = settings.themeMode,
                        homeStationId = settings.homeStationId,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Updates the latitude value.
     *
     * @param latitude New latitude string value
     */
    fun updateLatitude(latitude: String) {
        _uiState.update {
            it.copy(
                latitude = latitude,
                saveSuccess = false,
                errorMessage = null
            )
        }
        saveLocationIfValid()
    }

    /**
     * Updates the longitude value.
     *
     * @param longitude New longitude string value
     */
    fun updateLongitude(longitude: String) {
        _uiState.update {
            it.copy(
                longitude = longitude,
                saveSuccess = false,
                errorMessage = null
            )
        }
        saveLocationIfValid()
    }

    /**
     * Sets location from a preset.
     *
     * @param preset The preset location to use
     */
    fun setPresetLocation(preset: PresetLocation) {
        _uiState.update {
            it.copy(
                latitude = preset.latitude.toString(),
                longitude = preset.longitude.toString(),
                saveSuccess = false,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                settingsDataStore.setLocation(preset.latitude, preset.longitude)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save location: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Updates the temperature unit preference.
     *
     * @param unit New temperature unit
     */
    fun updateTemperatureUnit(unit: TemperatureUnit) {
        _uiState.update {
            it.copy(
                temperatureUnit = unit,
                saveSuccess = false,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                settingsDataStore.setTemperatureUnit(unit)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save temperature unit: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Updates the theme mode preference.
     *
     * @param mode New theme mode
     */
    fun updateThemeMode(mode: ThemeMode) {
        _uiState.update {
            it.copy(
                themeMode = mode,
                saveSuccess = false,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                settingsDataStore.setThemeMode(mode)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save theme mode: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Updates the Weather Underground home-station ID that drives the widget's HOME half.
     * Persisted on each change (blank restores the default garden station).
     *
     * @param stationId New station ID (e.g. IKILLI35)
     */
    fun updateHomeStationId(stationId: String) {
        _uiState.update {
            it.copy(
                homeStationId = stationId,
                saveSuccess = false,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                settingsDataStore.setHomeStationId(stationId)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save home station: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Resets all settings to default values.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                settingsDataStore.resetToDefaults()
                _uiState.update {
                    it.copy(
                        latitude = UserSettings.DEFAULT_LATITUDE.toString(),
                        longitude = UserSettings.DEFAULT_LONGITUDE.toString(),
                        temperatureUnit = TemperatureUnit.CELSIUS,
                        themeMode = ThemeMode.SYSTEM,
                        homeStationId = UserSettings.DEFAULT_HOME_STATION,
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to reset settings: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clears the save success flag.
     */
    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Saves location if both latitude and longitude are valid.
     */
    private fun saveLocationIfValid() {
        val latitude = _uiState.value.latitude.toDoubleOrNull()
        val longitude = _uiState.value.longitude.toDoubleOrNull()

        if (latitude != null && longitude != null) {
            // Validate coordinate ranges
            if (latitude !in -90.0..90.0) {
                _uiState.update {
                    it.copy(errorMessage = "Latitude must be between -90 and 90")
                }
                return
            }
            if (longitude !in -180.0..180.0) {
                _uiState.update {
                    it.copy(errorMessage = "Longitude must be between -180 and 180")
                }
                return
            }

            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(isSaving = true) }
                    settingsDataStore.setLocation(latitude, longitude)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Failed to save location: ${e.message}"
                        )
                    }
                }
            }
        }
    }
}
