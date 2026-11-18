package com.example.dailyok.ui.screens.home

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalTime

class HomePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun isPaused(): Boolean =
        prefs.getBoolean(KEY_PAUSED, false)

    fun lastOkClickTime(): Long =
        prefs.getLong(KEY_LAST_CLICK, 0L)

    fun saveLastOkClickTime(time: Long) {
        prefs.edit()
            .putLong(KEY_LAST_CLICK, time)
            .apply()
    }

    fun savePaused(paused: Boolean) {
        prefs.edit()
            .putBoolean(KEY_PAUSED, paused)
            .apply()
    }

    fun loadMedication(): MedicationReminder {
        return MedicationReminder(
            name = prefs.getString(KEY_MED_NAME, "BP pill") ?: "BP pill",
            time = LocalTime.of(
                prefs.getInt(KEY_MED_HOUR, 8),
                prefs.getInt(KEY_MED_MINUTE, 0)
            ),
            lastTakenEpochDay = prefs.getLong(
                KEY_MED_LAST_TAKEN,
                -1L
            )
        )
    }

    fun saveMedication(name: String, time: LocalTime) {
        prefs.edit()
            .putString(KEY_MED_NAME, name)
            .putInt(KEY_MED_HOUR, time.hour)
            .putInt(KEY_MED_MINUTE, time.minute)
            .apply()
    }

    fun saveMedicationTaken(day: Long) {
        prefs.edit()
            .putLong(KEY_MED_LAST_TAKEN, day)
            .apply()
    }

    private companion object {
        const val KEY_PAUSED = "is_paused"
        const val KEY_LAST_CLICK = "last_click_time"
        const val KEY_MED_NAME = "med_name"
        const val KEY_MED_HOUR = "med_hour"
        const val KEY_MED_MINUTE = "med_minute"
        const val KEY_MED_LAST_TAKEN = "med_last_taken_day"
    }
}