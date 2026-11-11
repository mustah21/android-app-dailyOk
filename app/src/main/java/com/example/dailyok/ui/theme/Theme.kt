package com.example.dailyok.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Material 3 roles mapped to the DailyOK palette.
// Dynamic colour (Material You) is deliberately NOT used: it would replace this palette
// with colours from the user's wallpaper and break the contrast rules.
private val DailyOkColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnColor,
    secondary = AllWellGreen,
    onSecondary = OnColor,
    secondaryContainer = AllWellLight,
    onSecondaryContainer = MainText,
    tertiary = WarningAmber,
    onTertiary = MainText,
    error = HelpRed,
    onError = OnColor,
    background = Background,
    onBackground = MainText,
    surface = Background,
    onSurface = MainText,
    surfaceVariant = Surface,
    onSurfaceVariant = SecondaryText,
    outline = Outline,
)

/** Colours for the app's states, so screens never pick status colours by hand. */
@Immutable
data class StatusColors(
    val allWell: Color = AllWellGreen,
    val onAllWell: Color = OnColor,
    val allWellLight: Color = AllWellLight,
    val help: Color = HelpRed,
    val onHelp: Color = OnColor,
    val warning: Color = WarningAmber,
    val onWarning: Color = MainText,
    val secondaryText: Color = SecondaryText,
)

private val LocalStatusColors = staticCompositionLocalOf { StatusColors() }

/**
 * Wrap every screen in DailyOkTheme. Light theme only for now (the style guide
 * makes dark mode optional for the demo).
 */
@Composable
fun DailyOkTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides StatusColors()) {
        MaterialTheme(
            colorScheme = DailyOkColorScheme,
            typography = DailyOkTypography,
            shapes = DailyOkShapes,
            content = content,
        )
    }
}

/** Access the extra DailyOK values anywhere: DailyOkTheme.status.help */
object DailyOkTheme {
    val status: StatusColors
        @Composable @ReadOnlyComposable
        get() = LocalStatusColors.current
}