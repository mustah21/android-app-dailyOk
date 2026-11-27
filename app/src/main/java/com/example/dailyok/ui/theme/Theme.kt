package com.example.dailyok.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val DailyOkDarkColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    onPrimary = OnColor,
    secondary = DarkAllWellGreen,
    onSecondary = OnColor,
    secondaryContainer = DarkAllWellLight,
    onSecondaryContainer = DarkMainText,
    tertiary = DarkWarningAmber,
    onTertiary = DarkOnWarning,
    error = HelpRed,
    onError = OnColor,
    background = DarkBackground,
    onBackground = DarkMainText,
    surface = DarkBackground,
    onSurface = DarkMainText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkOutline,
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

private val DarkStatusColors = StatusColors(
    allWell = DarkAllWellGreen,
    onAllWell = OnColor,
    allWellLight = DarkAllWellLight,
    help = HelpRed,
    onHelp = OnColor,
    warning = DarkWarningAmber,
    onWarning = DarkOnWarning,
    secondaryText = DarkSecondaryText,
)

private val LocalStatusColors = staticCompositionLocalOf { StatusColors() }

/**
 * Wrap every screen in DailyOkTheme. Supports light and night (dark grey) mode.
 */
@Composable
fun DailyOkTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DailyOkDarkColorScheme else DailyOkColorScheme
    val statusColors = if (darkTheme) DarkStatusColors else StatusColors()
    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
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