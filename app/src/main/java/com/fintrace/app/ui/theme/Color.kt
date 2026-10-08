package com.fintrace.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Fintrace brand and semantic tokens.
val FintraceTeal = Color(0xFF0E7C72)
val FintraceTealDark = Color(0xFF0F3F39)
val FintraceTealLight = Color(0xFFD2F3EE)

val LightBackground = Color(0xFFF6F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFEDF1F4)
val LightSurfaceBorder = Color(0xFFDDE3E8)
val LightTextPrimary = Color(0xFF0F1B22)
val LightTextSecondary = Color(0xFF53636D)
val LightTextTertiary = Color(0xFF81919A)

val DarkBackground = Color(0xFF0A1013)
val DarkSurface = Color(0xFF121A1F)
val DarkSurfaceElevated = Color(0xFF1B262C)
val DarkSurfaceBorder = Color(0xFF27353D)
val DarkTextPrimary = Color(0xFFE7EEF1)
val DarkTextSecondary = Color(0xFF9DB0BA)
val DarkTextTertiary = Color(0xFF71858F)

val LightIncome = Color(0xFF1E9E56)
val LightExpense = Color(0xFFD6455A)
val LightPending = Color(0xFFC27A00)
val LightPendingContainer = Color(0xFFFFF1D6)
val DarkIncome = Color(0xFF52D68A)
val DarkExpense = Color(0xFFFF7A8C)
val DarkPending = Color(0xFFFFC24D)
val DarkPendingContainer = Color(0xFF3A2C0C)

data class FintraceColors(
    val income: Color,
    val expense: Color,
    val pending: Color,
    val pendingContainer: Color
)

val LocalFintraceColors = staticCompositionLocalOf {
    FintraceColors(
        income = LightIncome,
        expense = LightExpense,
        pending = LightPending,
        pendingContainer = LightPendingContainer
    )
}

val PrimaryEmerald = FintraceTeal
val PrimaryEmeraldDark = FintraceTeal
val PrimaryEmeraldLight = Color(0xFF3CD3C0)
val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryBlueDark = Color(0xFF2563EB)
val PrimaryBlueLight = Color(0xFF60A5FA)
val AccentPurple = Color(0xFF8B5CF6)
val AccentPink = Color(0xFFEC4899)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = LightExpense
val AccentCyan = Color(0xFF06B6D4)

// Compatibility aliases for existing screens while they migrate to semantic tokens.
val ExpenseRed = LightExpense
val IncomeGreen = LightIncome
val SplitBadgeBg = Color(0x228B5CF6)
val SplitBadgeText = Color(0xFFA78BFA)

val CategoryColorPalette = listOf(
    "#0E7C72", "#3B82F6", "#6366F1", "#A855F7", "#EC4899", "#EF4444",
    "#F97316", "#F59E0B", "#84CC16", "#22C55E", "#14B8A6", "#64748B"
)
