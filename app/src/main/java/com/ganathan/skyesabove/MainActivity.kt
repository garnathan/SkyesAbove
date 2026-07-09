package com.ganathan.skyesabove

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ganathan.skyesabove.data.preferences.SettingsDataStore
import com.ganathan.skyesabove.data.preferences.ThemeMode
import com.ganathan.skyesabove.ui.screens.diagnostics.DiagnosticsScreen
import com.ganathan.skyesabove.ui.screens.settings.SettingsScreen
import com.ganathan.skyesabove.ui.screens.trends.TrendsScreen
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
 * Main composable: two top-level tabs (Forecast · Trends) via a bottom navigation bar, with
 * Settings pushed on top from the Forecast header. The bottom bar is hidden on Settings.
 * Each screen draws its own full-bleed gradient + handles its status-bar inset; the bar sits
 * below the screen area (own Column row) so nothing is hidden behind it.
 */
@Composable
fun SkyesAboveApp(
    navController: NavHostController = rememberNavController()
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute == null ||
        currentRoute == NavigationRoutes.FORECAST || currentRoute == NavigationRoutes.TRENDS

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.FORECAST,
            modifier = Modifier.weight(1f)
        ) {
            composable(route = NavigationRoutes.FORECAST) {
                WeatherScreen(
                    onSettingsClick = { navController.navigate(NavigationRoutes.SETTINGS) }
                )
            }

            composable(route = NavigationRoutes.TRENDS) {
                TrendsScreen()
            }

            composable(route = NavigationRoutes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDiagnostics = { navController.navigate(NavigationRoutes.DIAGNOSTICS) }
                )
            }

            composable(route = NavigationRoutes.DIAGNOSTICS) {
                DiagnosticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        if (showBottomBar) {
            SkyesBottomBar(currentRoute = currentRoute, navController = navController)
        }
    }
}

@Composable
private fun SkyesBottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar(containerColor = Color.Black.copy(alpha = 0.28f)) {
        SkyesBottomBarItem(
            route = NavigationRoutes.FORECAST,
            label = "Forecast",
            icon = Icons.Filled.WbSunny,
            currentRoute = currentRoute ?: NavigationRoutes.FORECAST,
            navController = navController
        )
        SkyesBottomBarItem(
            route = NavigationRoutes.TRENDS,
            label = "Trends",
            icon = Icons.Filled.ShowChart,
            currentRoute = currentRoute ?: NavigationRoutes.FORECAST,
            navController = navController
        )
    }
}

@Composable
private fun RowScope.SkyesBottomBarItem(
    route: String,
    label: String,
    icon: ImageVector,
    currentRoute: String,
    navController: NavHostController
) {
    NavigationBarItem(
        selected = currentRoute == route,
        onClick = {
            if (currentRoute != route) {
                navController.navigate(route) {
                    // Single top-level back stack entry per tab; preserve/restore state.
                    popUpTo(NavigationRoutes.FORECAST) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Color.White,
            indicatorColor = Color.White.copy(alpha = 0.20f),
            unselectedIconColor = Color.White.copy(alpha = 0.6f),
            unselectedTextColor = Color.White.copy(alpha = 0.6f)
        )
    )
}

/**
 * Navigation route constants for the app.
 */
object NavigationRoutes {
    const val FORECAST = "forecast"
    const val TRENDS = "trends"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}
