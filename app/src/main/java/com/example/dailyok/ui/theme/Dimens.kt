package com.example.dailyok.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Sizes from the style guide. Google's minimum touch target is 48dp; DailyOK uses larger ones.
object Dimens {
    val ButtonHeight = 64.dp          // Normal buttons, full width
    val HelpButtonHeight = 96.dp      // "I need help" button
    val MinTouchTarget = 56.dp        // Switches, checkboxes, list rows
    val ListRowHeight = 64.dp         // Rows in settings and the activity log
    val SpaceBetweenTargets = 16.dp   // Gap between buttons
    val ScreenMargin = 24.dp          // Padding around every screen
    val CardPadding = 20.dp
    val CornerRadius = 16.dp
    val StatusIconSize = 56.dp        // Tick icon on the status card
}

val DailyOkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(Dimens.CornerRadius),
    large = RoundedCornerShape(Dimens.CornerRadius),
    extraLarge = RoundedCornerShape(24.dp),
)
