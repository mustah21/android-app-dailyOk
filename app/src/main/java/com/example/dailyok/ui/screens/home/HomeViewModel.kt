package com.example.dailyok.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

private const val DAY_MS = 24 * 60 * 60 * 1000L
enum class MedStatus { UPCOMING, DUE, TAKEN }
// hardcoded
data class MedicationReminder(
    val name: String = "BP pill",
    val time: LocalTime = LocalTime.of(8, 0),
    val lastTakenEpochDay: Long = -1L,
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
    val medication: MedicationReminder = MedicationReminder(),
) {
    val isRecent: Boolean
        get() = System.currentTimeMillis() - lastOkClickTime < DAY_MS

    // Paused -> button never works.  normal 24h rule.
    val canPressOk: Boolean
        get() = !isPaused && !isRecent

    val lastActiveText: String
        get() {
            if (lastOkClickTime == 0L) return ""
            val last = java.time.Instant.ofEpochMilli(lastOkClickTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()
            return "Last active today at ${last.format(java.time.format.DateTimeFormatter.ofPattern("H:mm"))}"
        }
}

class HomeViewModel(context: Context) : ViewModel() {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        HomeUiState(
            isPaused = prefs.getBoolean(KEY_PAUSED, false),
            lastOkClickTime = prefs.getLong(KEY_LAST_CLICK, 0L),
            medication = loadMedication(),

        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private fun loadMedication() = MedicationReminder(
        name = prefs.getString(KEY_MED_NAME, "BP pill") ?: "BP pill",
        time = LocalTime.of(
            prefs.getInt(KEY_MED_HOUR, 8),
            prefs.getInt(KEY_MED_MINUTE, 0)
        ),
        lastTakenEpochDay = prefs.getLong(KEY_MED_LAST_TAKEN, -1L),
    )

    fun onOkClicked() {
        if (!_state.value.canPressOk) return

        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_CLICK, now).apply()
        prefs.edit().putLong(KEY_LAST_CLICK, now).apply()
        _state.update { it.copy(lastOkClickTime = now) }
    }

    fun onPauseChanged(paused: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSED, paused).apply()
        _state.update { it.copy(isPaused = paused) }
    }

    fun onMedicationTaken() {
        val today = LocalDate.now().toEpochDay()
        prefs.edit().putLong(KEY_MED_LAST_TAKEN, today).apply()
        _state.update { it.copy(medication = it.medication.copy(lastTakenEpochDay = today)) }
    }

    // Call this from a future settings screen to change the pill/time.
    fun setMedication(name: String, time: LocalTime) {
        prefs.edit()
            .putString(KEY_MED_NAME, name)
            .putInt(KEY_MED_HOUR, time.hour)
            .putInt(KEY_MED_MINUTE, time.minute)
            .apply()
        _state.update { it.copy(medication = it.medication.copy(name = name, time = time)) }
    }

    companion object {
        private const val KEY_PAUSED = "is_paused"
        private const val KEY_LAST_CLICK = "last_click_time"
        private const val KEY_MED_NAME = "med_name"
        private const val KEY_MED_HOUR = "med_hour"
        private const val KEY_MED_MINUTE = "med_minute"
        private const val KEY_MED_LAST_TAKEN = "med_last_taken_day"

        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(context.applicationContext) as T
        }
    }
}