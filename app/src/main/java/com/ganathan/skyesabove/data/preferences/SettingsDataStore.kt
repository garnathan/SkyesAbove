package com.ganathan.skyesabove.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Temperature unit options.
 */
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

/**
 * Theme mode options.
 */
enum class ThemeMode {
    SYSTEM,  // Follow system setting
    LIGHT,
    DARK
}

/**
 * User settings data class.
 */
data class UserSettings(
    val latitude: Double = DEFAULT_LATITUDE,
    val longitude: Double = DEFAULT_LONGITUDE,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val openWeatherMapApiKey: String = DEFAULT_OWM_API_KEY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val homeStationId: String = DEFAULT_HOME_STATION
) {
    companion object {
        // Default location: Killiney, Ireland
        const val DEFAULT_LATITUDE = 53.2631
        const val DEFAULT_LONGITUDE = -6.1083
        // OpenWeatherMap API key - get free key at https://openweathermap.org/api
        const val DEFAULT_OWM_API_KEY = ""
        // Weather Underground PWS that drives the widget's HOME (right) half. Defaults to the
        // author's garden station; configurable so anyone can point the widget at their own PWS.
        const val DEFAULT_HOME_STATION = "IKILLI35"
    }
}

/**
 * A previously-resolved device location, cached so the widget can self-heal when a live fix fails.
 */
data class CachedLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String
)

/**
 * DataStore-based settings repository for app preferences.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val OWM_API_KEY = stringPreferencesKey("owm_api_key")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HOME_STATION_ID = stringPreferencesKey("home_station_id")

        // Last SUCCESSFULLY-resolved device location. This is a *location* cache (never a
        // weather-value cache): if a live GPS/network fix fails, the widget reuses the last
        // good fix instead of snapping to the hard-coded Killiney default, so it self-heals.
        val LAST_LOC_LAT = doublePreferencesKey("last_loc_lat")
        val LAST_LOC_LON = doublePreferencesKey("last_loc_lon")
        val LAST_LOC_NAME = stringPreferencesKey("last_loc_name")
    }

    /**
     * Flow of current user settings.
     */
    val settings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            latitude = preferences[PreferencesKeys.LATITUDE] ?: UserSettings.DEFAULT_LATITUDE,
            longitude = preferences[PreferencesKeys.LONGITUDE] ?: UserSettings.DEFAULT_LONGITUDE,
            temperatureUnit = preferences[PreferencesKeys.TEMPERATURE_UNIT]?.let {
                TemperatureUnit.valueOf(it)
            } ?: TemperatureUnit.CELSIUS,
            openWeatherMapApiKey = preferences[PreferencesKeys.OWM_API_KEY] ?: UserSettings.DEFAULT_OWM_API_KEY,
            themeMode = preferences[PreferencesKeys.THEME_MODE]?.let {
                ThemeMode.valueOf(it)
            } ?: ThemeMode.SYSTEM,
            homeStationId = preferences[PreferencesKeys.HOME_STATION_ID]?.takeIf { it.isNotBlank() }
                ?: UserSettings.DEFAULT_HOME_STATION
        )
    }

    /**
     * Flow of the Weather Underground station ID that drives the widget's HOME half.
     * Falls back to the default garden station when unset/blank.
     */
    val homeStationId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HOME_STATION_ID]?.takeIf { it.isNotBlank() }
            ?: UserSettings.DEFAULT_HOME_STATION
    }

    /**
     * Flow of current theme mode.
     */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE]?.let {
            ThemeMode.valueOf(it)
        } ?: ThemeMode.SYSTEM
    }

    /**
     * Flow of OpenWeatherMap API key.
     */
    val openWeatherMapApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.OWM_API_KEY] ?: UserSettings.DEFAULT_OWM_API_KEY
    }

    /**
     * Flow of current latitude.
     */
    val latitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LATITUDE] ?: UserSettings.DEFAULT_LATITUDE
    }

    /**
     * Flow of current longitude.
     */
    val longitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LONGITUDE] ?: UserSettings.DEFAULT_LONGITUDE
    }

    /**
     * Flow of current temperature unit.
     */
    val temperatureUnit: Flow<TemperatureUnit> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TEMPERATURE_UNIT]?.let {
            TemperatureUnit.valueOf(it)
        } ?: TemperatureUnit.CELSIUS
    }

    /**
     * Update the stored latitude.
     *
     * @param latitude New latitude value
     */
    suspend fun setLatitude(latitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LATITUDE] = latitude
        }
    }

    /**
     * Update the stored longitude.
     *
     * @param longitude New longitude value
     */
    suspend fun setLongitude(longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LONGITUDE] = longitude
        }
    }

    /**
     * Update both latitude and longitude at once.
     *
     * @param latitude New latitude value
     * @param longitude New longitude value
     */
    suspend fun setLocation(latitude: Double, longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LATITUDE] = latitude
            preferences[PreferencesKeys.LONGITUDE] = longitude
        }
    }

    /**
     * Update the temperature unit preference.
     *
     * @param unit New temperature unit
     */
    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEMPERATURE_UNIT] = unit.name
        }
    }

    /**
     * Update the OpenWeatherMap API key.
     *
     * @param apiKey New API key
     */
    suspend fun setOpenWeatherMapApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OWM_API_KEY] = apiKey
        }
    }

    /**
     * Update the theme mode preference.
     *
     * @param mode New theme mode
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    /**
     * The last successfully-resolved device location, or null if we've never resolved one.
     * Used as a self-healing fallback when a live location fix is unavailable.
     */
    val lastKnownLocation: Flow<CachedLocation?> = context.dataStore.data.map { preferences ->
        val lat = preferences[PreferencesKeys.LAST_LOC_LAT]
        val lon = preferences[PreferencesKeys.LAST_LOC_LON]
        if (lat != null && lon != null) {
            CachedLocation(lat, lon, preferences[PreferencesKeys.LAST_LOC_NAME] ?: "Current location")
        } else {
            null
        }
    }

    /**
     * Persist the last successfully-resolved device location.
     */
    suspend fun setLastKnownLocation(latitude: Double, longitude: Double, name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_LOC_LAT] = latitude
            preferences[PreferencesKeys.LAST_LOC_LON] = longitude
            preferences[PreferencesKeys.LAST_LOC_NAME] = name
        }
    }

    /**
     * Update the Weather Underground home-station ID (drives the widget's HOME half).
     * A blank value clears the override, restoring the default garden station.
     */
    suspend fun setHomeStationId(stationId: String) {
        context.dataStore.edit { preferences ->
            val trimmed = stationId.trim()
            if (trimmed.isBlank()) {
                preferences.remove(PreferencesKeys.HOME_STATION_ID)
            } else {
                preferences[PreferencesKeys.HOME_STATION_ID] = trimmed.uppercase()
            }
        }
    }

    /**
     * Reset all settings to defaults.
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
