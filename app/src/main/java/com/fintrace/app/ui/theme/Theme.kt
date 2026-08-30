package com.fintrace.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmerald,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryEmeraldDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = PrimaryBlue,
    onSecondary = DarkBackground,
    secondaryContainer = PrimaryBlueDark,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = AccentPurple,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceBorder,
    error = ExpenseRed,
    onError = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEmeraldDark,
    onPrimary = LightSurface,
    primaryContainer = PrimaryEmeraldLight,
    onPrimaryContainer = LightTextPrimary,
    secondary = PrimaryBlueDark,
    onSecondary = LightSurface,
    secondaryContainer = PrimaryBlueLight,
    onSecondaryContainer = LightTextPrimary,
    tertiary = AccentPurple,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    error = ExpenseRed,
    onError = LightSurface
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalViewMap()

    SideEffect {
        view?.let {
            val window = (it.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, it).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, it).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
private fun LocalViewMap() = if (LocalView.current.isInEditMode) null else LocalView.current
