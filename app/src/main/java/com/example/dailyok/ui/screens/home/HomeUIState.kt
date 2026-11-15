package com.example.dailyok.ui.screens.home

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val DAY_MS = 24 * 60 * 60 * 1_000L

enum class MedStatus {
    UPCOMING,
    DUE,
    TAKEN
}

data class MedicationReminder(
    val name: String = "BP pill",
    val time: LocalTime = LocalTime.of(8, 0),
    val lastTakenEpochDay: Long = -1L
) {
    val isTakenToday: Boolean
        get() = lastTakenEpochDay == LocalDate.now().toEpochDay()

    val status: MedStatus
        get() = when {
            isTakenToday -> MedStatus.TAKEN
            !LocalTime.now().isBefore(time) -> MedStatus.DUE
            else -> MedStatus.UPCOMING
        }
}

data class HomeUiState(
    val isPaused: Boolean = false,
    val lastOkClickTime: Long = 0L,
    val medication: MedicationReminder = MedicationReminder()
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