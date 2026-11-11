package com.example.dailyok.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Roboto is Android's default font, so FontFamily.Default renders it without any font files.
// Sizes are in sp so they also grow when the user raises the phone's font size.
// Line height is about 1.4 times the text size. Nothing is smaller than 18sp.

private val Roboto = FontFamily.Default

/** Status: the main message on the home screen ("All is well"). 32sp bold. */
val StatusText = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp)

/** Heading: screen titles. 26sp bold. */
val HeadingText = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp)

/** Button: all button labels. 22sp bold. */
val ButtonText = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)

/** Body: normal text and explanations. 20sp regular. */
val BodyText = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp)

/** Small: times, hints, secondary labels. 18sp regular. Never use anything smaller. */
val SmallText = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp)

// Material 3 slots mapped to the five DailyOK styles, so standard components
// (Text, Button, TopAppBar, ListItem ...) pick up the right sizes automatically.
val DailyOkTypography = Typography(
    displayLarge = StatusText,
    displayMedium = StatusText,
    displaySmall = StatusText,
    headlineLarge = HeadingText,
    headlineMedium = HeadingText,
    headlineSmall = HeadingText,
    titleLarge = HeadingText,
    titleMedium = ButtonText,
    titleSmall = BodyText.copy(fontWeight = FontWeight.Bold),
    bodyLarge = BodyText,
    bodyMedium = BodyText,
    bodySmall = SmallText,
    labelLarge = ButtonText,
    labelMedium = SmallText,
    labelSmall = SmallText,
)