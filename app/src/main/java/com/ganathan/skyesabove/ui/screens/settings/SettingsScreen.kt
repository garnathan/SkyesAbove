package com.ganathan.skyesabove.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganathan.skyesabove.data.preferences.TemperatureUnit
import com.ganathan.skyesabove.data.preferences.ThemeMode
import com.ganathan.skyesabove.ui.theme.SkyesAboveTheme

/**
 * Settings screen composable that allows users to configure app preferences.
 *
 * @param onNavigateBack Callback invoked when the user wants to navigate back
 * @param viewModel The SettingsViewModel instance (injected via Hilt)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(
                message = "Settings saved",
                duration = SnackbarDuration.Short
            )
            viewModel.clearSaveSuccess()
        }
    }

    // Show snackbar on error
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    // Show saving indicator
                    AnimatedVisibility(
                        visible = uiState.isSaving,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    // Show saved indicator
                    AnimatedVisibility(
                        visible = uiState.saveSuccess && !uiState.isSaving,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Saved",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading settings...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Location Section
                LocationSection(
                    latitude = uiState.latitude,
                    longitude = uiState.longitude,
                    onLatitudeChange = viewModel::updateLatitude,
                    onLongitudeChange = viewModel::updateLongitude,
                    onUseCurrentLocation = { /* TODO: Implement GPS location */ },
                    presetLocations = viewModel.presetLocations,
                    onPresetSelected = viewModel::setPresetLocation
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Temperature Unit Section
                TemperatureUnitSection(
                    selectedUnit = uiState.temperatureUnit,
                    onUnitSelected = viewModel::updateTemperatureUnit
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Theme Mode Section
                ThemeModeSection(
                    selectedMode = uiState.themeMode,
                    onModeSelected = viewModel::updateThemeMode
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Home Station Section (WU PWS that drives the widget's HOME half)
                HomeStationSection(
                    stationId = uiState.homeStationId,
                    onStationIdChange = viewModel::updateHomeStationId
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Widget Diagnostics entry
                DiagnosticsSection(onOpenDiagnostics = onOpenDiagnostics)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Reset Section
                ResetSection(
                    onReset = viewModel::resetToDefaults
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Location settings section with coordinate inputs and preset locations.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationSection(
    latitude: String,
    longitude: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    presetLocations: List<PresetLocation>,
    onPresetSelected: (PresetLocation) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Coordinate Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latitude Input
                OutlinedTextField(
                    value = latitude,
                    onValueChange = onLatitudeChange,
                    label = { Text("Latitude") },
                    placeholder = { Text("e.g., 53.3498") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Longitude Input
                OutlinedTextField(
                    value = longitude,
                    onValueChange = onLongitudeChange,
                    label = { Text("Longitude") },
                    placeholder = { Text("e.g., -6.2603") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Use Current Location Button
                OutlinedButton(
                    onClick = onUseCurrentLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Use Current Location")
                }
            }
        }

        // Preset Locations
        Text(
            text = "Quick Select",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetLocations.forEach { preset ->
                val isSelected = latitude == preset.latitude.toString() &&
                        longitude == preset.longitude.toString()

                FilterChip(
                    selected = isSelected,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.name) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

/**
 * Temperature unit selection section.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemperatureUnitSection(
    selectedUnit: TemperatureUnit,
    onUnitSelected: (TemperatureUnit) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Temperature Unit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemperatureUnit.entries.forEach { unit ->
                val isSelected = unit == selectedUnit
                val label = when (unit) {
                    TemperatureUnit.CELSIUS -> "Celsius (C)"
                    TemperatureUnit.FAHRENHEIT -> "Fahrenheit (F)"
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onUnitSelected(unit) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

/**
 * Theme mode selection section.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeModeSection(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onModeSelected(mode) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

/**
 * Home station section — the Weather Underground PWS that drives the widget's HOME (right) half.
 * Configurable so the app isn't hard-wired to the author's garden sensor.
 */
@Composable
private fun HomeStationSection(
    stationId: String,
    onStationIdChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Home Station",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = stationId,
                    onValueChange = onStationIdChange,
                    label = { Text("Weather Underground station ID") },
                    placeholder = { Text("e.g., IKILLI35") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Text(
                    text = "The widget's HOME (right) half reads this Weather Underground personal " +
                        "weather station. Point it at any public WU station. Leave blank to use the " +
                        "default. Note: the Trends tab and the pressure-trend arrow stay tied to the " +
                        "app's garden sensor, so they only apply to the default station.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Widget diagnostics entry — opens the on-device refresh log that explains why the widget
 * lost data (which half, network state, observation age) without needing adb/logcat.
 */
@Composable
private fun DiagnosticsSection(
    onOpenDiagnostics: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Widget diagnostics",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "A rolling on-device log of every widget refresh — which half was live, the " +
                "network at the time, and how old the home observation was. Open it to see why " +
                "the widget lost data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onOpenDiagnostics,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Insights,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Open widget diagnostics")
        }
    }
}

/**
 * Reset to defaults section.
 */
@Composable
private fun ResetSection(
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Reset",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Reset all settings to their default values. This will set the location to Killiney, Ireland and temperature to Celsius.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Reset to Defaults")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SkyesAboveTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                latitude = "53.3498",
                longitude = "-6.2603",
                temperatureUnit = TemperatureUnit.CELSIUS,
                isLoading = false
            ),
            presetLocations = listOf(
                PresetLocation("Dublin", 53.3498, -6.2603),
                PresetLocation("Cork", 51.8985, -8.4756),
                PresetLocation("Galway", 53.2707, -9.0568)
            ),
            onNavigateBack = {},
            onLatitudeChange = {},
            onLongitudeChange = {},
            onUseCurrentLocation = {},
            onPresetSelected = {},
            onUnitSelected = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - Dark Theme")
@Composable
private fun SettingsScreenDarkPreview() {
    SkyesAboveTheme(darkTheme = true) {
        SettingsScreenContent(
            uiState = SettingsUiState(
                latitude = "51.8985",
                longitude = "-8.4756",
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                isLoading = false
            ),
            presetLocations = listOf(
                PresetLocation("Dublin", 53.3498, -6.2603),
                PresetLocation("Cork", 51.8985, -8.4756),
                PresetLocation("Galway", 53.2707, -9.0568)
            ),
            onNavigateBack = {},
            onLatitudeChange = {},
            onLongitudeChange = {},
            onUseCurrentLocation = {},
            onPresetSelected = {},
            onUnitSelected = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - Loading")
@Composable
private fun SettingsScreenLoadingPreview() {
    SkyesAboveTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(isLoading = true),
            presetLocations = emptyList(),
            onNavigateBack = {},
            onLatitudeChange = {},
            onLongitudeChange = {},
            onUseCurrentLocation = {},
            onPresetSelected = {},
            onUnitSelected = {},
            onReset = {}
        )
    }
}

/**
 * Stateless version of SettingsScreen for preview purposes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    presetLocations: List<PresetLocation>,
    onNavigateBack: () -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onPresetSelected: (PresetLocation) -> Unit,
    onUnitSelected: (TemperatureUnit) -> Unit,
    onReset: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading settings...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LocationSection(
                    latitude = uiState.latitude,
                    longitude = uiState.longitude,
                    onLatitudeChange = onLatitudeChange,
                    onLongitudeChange = onLongitudeChange,
                    onUseCurrentLocation = onUseCurrentLocation,
                    presetLocations = presetLocations,
                    onPresetSelected = onPresetSelected
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                TemperatureUnitSection(
                    selectedUnit = uiState.temperatureUnit,
                    onUnitSelected = onUnitSelected
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ResetSection(onReset = onReset)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
