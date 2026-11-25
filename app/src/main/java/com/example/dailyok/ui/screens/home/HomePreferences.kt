package com.example.dailyok.ui.screens.home

import android.content.Context
import android.content.SharedPreferences

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

    private companion object {
        const val KEY_PAUSED = "is_paused"
        const val KEY_LAST_CLICK = "last_click_time"
    }
}