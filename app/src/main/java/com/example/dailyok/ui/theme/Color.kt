package com.example.dailyok.ui.theme

import androidx.compose.ui.graphics.Color

// DailyOK palette (see "DailyOK: Design Style Guide").
// Every text pairing passes WCAG 2.1 AA (4.5:1); contrast noted beside each colour.

val Background = Color(0xFFFFFFFF)       // Screen background
val Surface = Color(0xFFF5F5F2)          // Cards and panels (15.9:1 with MainText)
val MainText = Color(0xFF1A1A1A)         // Body text and headings (17.4:1 on white)
val SecondaryText = Color(0xFF4A4A4A)    // Labels, times, hints (8.9:1 on white)
val Outline = Color(0xFFDDDDD8)          // Borders and dividers (decoration only, never text)

val PrimaryBlue = Color(0xFF1F4E9C)      // Main buttons, links, active switch (8.0:1 with white)
val AllWellGreen = Color(0xFF1E6B3A)     // "All is well" status, success (6.5:1 with white)
val AllWellLight = Color(0xFFE7F3EC)     // Background of the status card (15.3:1 with MainText)
val HelpRed = Color(0xFFB3261E)          // "I need help" and alarm ONLY (6.5:1 with white)
val WarningAmber = Color(0xFFFFE8A3)     // Pause on, low battery (14.4:1 with MainText)

val OnColor = Color(0xFFFFFFFF)          // Text on blue, green and red
