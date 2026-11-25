package com.example.dailyok.ui.screens.home

import com.example.dailyok.domain.meds.Dose
import java.time.format.DateTimeFormatter

private const val DAY_MS = 24 * 60 * 60 * 1_000L

data class HomeUiState(
    val isPaused: Boolean = false,
    val lastOkClickTime: Long = 0L,
    val hasUsageAccess: Boolean = false,
    val lastActivity: String? = null,
    /** Next medicines to take, for the home screen preview. */
    val upcomingMeds: List<Dose> = emptyList(),
) {
    val isRecent: Boolean
        get() = System.currentTimeMillis() - lastOkClickTime < DAY_MS

    val canPressOk: Boolean
        get() = !isPaused && !isRecent

    val lastActiveText: String
        get() {
            if (lastOkClickTime == 0L) return ""

            val last = java.time.Instant.ofEpochMilli(lastOkClickTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()

            return "Last active today at ${
                last.format(DateTimeFormatter.ofPattern("H:mm"))
            }"
        }
}