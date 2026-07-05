package com.ganathan.skyesabove

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import com.ganathan.skyesabove.data.preferences.ThemeMode
import com.ganathan.skyesabove.ui.screens.settings.SettingsScreen
import com.ganathan.skyesabove.ui.screens.weather.WeatherScreen
import com.ganathan.skyesabove.ui.theme.SkyesAboveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for Skyes Above weather app.
 * Uses single-activity architecture with Jetpack Compose navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The home-screen widget follows the phone's location; request coarse location once.
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 42)
        }

        // Enable edge-to-edge display for modern Android UI
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SkyesAboveTheme(darkTheme = darkTheme) {
                SkyesAboveApp()
            }
        }
    }
}

/**
 * Main composable function that sets up the app's navigation structure.
 */
@Composable
fun SkyesAboveApp(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.HOME,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(route = NavigationRoutes.HOME) {
            // WeatherScreen handles its own edge-to-edge display
            WeatherScreen(
                onSettingsClick = {
                    navController.navigate(NavigationRoutes.SETTINGS)
                }
            )
        }

        composable(route = NavigationRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Navigation route constants for the app.
 */
object NavigationRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}
