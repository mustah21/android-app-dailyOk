package com.example.dailyok.data

import android.content.Context
import com.example.dailyok.ui.checkin.CheckInTimings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock values for presenting the app (Settings > Demo).
 * Saved on the phone, so they survive restarting the app.
 */
data class DemoConfig(
    /** Demo mode on: short prompt/alarm times. Off: the real 10 min + 15 min. */
    val enabled: Boolean = true,
    /** Seconds the "Are you OK?" prompt waits before the alarm. */
    val promptSeconds: Int = 15,
    /** Seconds of alarm countdown before the family is contacted. */
    val alarmSeconds: Int = 30,
    /** Daytime check after this many seconds without activity. 0 = use the normal hours setting. */
    val inactivitySeconds: Int = 0,
) {
    val timings: CheckInTimings
        get() = if (enabled) CheckInTimings(promptSeconds, alarmSeconds) else CheckInTimings.Real

    /** The seconds the check engine should use, or null for the normal hours setting. */
    val activeInactivitySeconds: Int?
        get() = if (enabled && inactivitySeconds > 0) inactivitySeconds else null

    companion object {
        val PROMPT_SECONDS_RANGE = 5..120
        val ALARM_SECONDS_RANGE = 10..300
        /** Choices shown in Settings > Demo (seconds). 0 = normal hours setting. */
        val INACTIVITY_PRESETS = listOf(0, 30, 60, 120, 300, 600, 1800, 3600)

        fun inactivityLabel(seconds: Int): String = when {
            seconds == 0 -> "Normal"
            seconds < 60 -> "$seconds s"
            else -> "${seconds / 60} min"
        }
    }
}

object DemoSettings {

    private const val PREFS_NAME = "demo_prefs"
    private const val KEY_ENABLED = "demo_enabled"
    private const val KEY_PROMPT_S = "demo_prompt_seconds"
    private const val KEY_ALARM_S = "demo_alarm_seconds"
    private const val KEY_INACTIVITY_S = "demo_inactivity_seconds"

    private var state: MutableStateFlow<DemoConfig>? = null

    /** Live demo settings; the UI collects this. */
    fun flow(context: Context): StateFlow<DemoConfig> = stateFor(context).asStateFlow()

    fun current(context: Context): DemoConfig = stateFor(context).value

    fun update(context: Context, change: (DemoConfig) -> DemoConfig) {
        val flow = stateFor(context)
        val new = change(flow.value).let {
            it.copy(
                promptSeconds = it.promptSeconds.coerceIn(DemoConfig.PROMPT_SECONDS_RANGE),
                alarmSeconds = it.alarmSeconds.coerceIn(DemoConfig.ALARM_SECONDS_RANGE),
                inactivitySeconds = if (it.inactivitySeconds == 0) 0 else it.inactivitySeconds.coerceIn(10, 3600),
            )
        }
        flow.value = new
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, new.enabled)
            .putInt(KEY_PROMPT_S, new.promptSeconds)
            .putInt(KEY_ALARM_S, new.alarmSeconds)
            .putInt(KEY_INACTIVITY_S, new.inactivitySeconds)
            .apply()
    }

    @Synchronized
    private fun stateFor(context: Context): MutableStateFlow<DemoConfig> =
        state ?: MutableStateFlow(load(context)).also { state = it }

    private fun load(context: Context): DemoConfig {
        val p = prefs(context)
        val d = DemoConfig()
        return DemoConfig(
            enabled = p.getBoolean(KEY_ENABLED, d.enabled),
            promptSeconds = p.getInt(KEY_PROMPT_S, d.promptSeconds),
            alarmSeconds = p.getInt(KEY_ALARM_S, d.alarmSeconds),
            inactivitySeconds = p.getInt(KEY_INACTIVITY_S, d.inactivitySeconds),
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
