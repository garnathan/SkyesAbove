package com.ganathan.skyesabove.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = OnPrimaryLight,
    primaryContainer = Purple80,
    onPrimaryContainer = PurpleDeep,
    secondary = Pink40,
    onSecondary = OnPrimaryLight,
    secondaryContainer = Pink80,
    onSecondaryContainer = PinkDark,
    tertiary = Purple60,
    onTertiary = OnPrimaryLight,
    tertiaryContainer = Purple80,
    onTertiaryContainer = PurpleDark,
    error = ErrorRed,
    onError = OnPrimaryLight,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Purple80,
    surfaceTint = Purple40
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = PurpleDeep,
    primaryContainer = PurpleDark,
    onPrimaryContainer = Purple80,
    secondary = Pink80,
    onSecondary = PinkDark,
    secondaryContainer = Pink40,
    onSecondaryContainer = Pink80,
    tertiary = Purple60,
    onTertiary = PurpleDeep,
    tertiaryContainer = PurpleDark,
    onTertiaryContainer = Purple80,
    error = ErrorRed,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Purple40,
    surfaceTint = Purple80
)

/**
 * Creates the vibrant gradient brush used for backgrounds
 */
@Composable
fun skyesAboveGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                GradientDarkStart,
                GradientDarkMid,
                GradientDarkEnd
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                GradientPurpleStart,
                GradientMidPurple,
                GradientPinkEnd
            )
        )
    }
}

/**
 * Creates a horizontal gradient for cards and accents
 */
@Composable
fun skyesAboveHorizontalGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.horizontalGradient(
            colors = listOf(
                Purple40.copy(alpha = 0.3f),
                Pink40.copy(alpha = 0.3f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Purple60.copy(alpha = 0.7f),
                Pink60.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
fun SkyesAboveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
