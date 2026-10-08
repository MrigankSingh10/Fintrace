package com.fintrace.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkTealContainer = Color(0xFF0F3F39)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3CD3C0),
    onPrimary = Color(0xFF00201C),
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = Color(0xFFBFF3EC),
    secondary = Color(0xFF9CCAC3),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF1E4D47),
    onSecondaryContainer = Color(0xFFB8EDE5),
    tertiary = Color(0xFFD0BCFF),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = FintraceTeal,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = FintraceTealLight,
    onPrimaryContainer = Color(0xFF00332E),
    secondary = Color(0xFF416A65),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8E3),
    onSecondaryContainer = Color(0xFF0A201D),
    tertiary = Color(0xFF6750A4),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        FintraceColors(DarkIncome, DarkExpense, DarkPending, DarkPendingContainer)
    } else {
        FintraceColors(LightIncome, LightExpense, LightPending, LightPendingContainer)
    }
    val view = LocalView.current

    SideEffect {
        if (!view.isInEditMode) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalFintraceColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
