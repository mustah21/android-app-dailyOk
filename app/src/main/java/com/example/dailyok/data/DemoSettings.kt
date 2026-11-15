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
    /** Daytime check after this many minutes without activity. 0 = use the normal hours setting. */
    val inactivityMinutes: Int = 0,
) {
    val timings: CheckInTimings
        get() = if (enabled) CheckInTimings(promptSeconds, alarmSeconds) else CheckInTimings.Real

    /** The minutes the check engine should use, or null for the normal hours setting. */
    val activeInactivityMinutes: Int?
        get() = if (enabled && inactivityMinutes > 0) inactivityMinutes else null

    companion object {
        val PROMPT_SECONDS_RANGE = 5..120
        val ALARM_SECONDS_RANGE = 10..300
        val INACTIVITY_MINUTES_RANGE = 0..30
    }
}

object DemoSettings {

    private const val PREFS_NAME = "demo_prefs"
    private const val KEY_ENABLED = "demo_enabled"
    private const val KEY_PROMPT_S = "demo_prompt_seconds"
    private const val KEY_ALARM_S = "demo_alarm_seconds"
    private const val KEY_INACTIVITY_MIN = "demo_inactivity_minutes"

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
                inactivityMinutes = it.inactivityMinutes.coerceIn(DemoConfig.INACTIVITY_MINUTES_RANGE),
            )
        }
        flow.value = new
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, new.enabled)
            .putInt(KEY_PROMPT_S, new.promptSeconds)
            .putInt(KEY_ALARM_S, new.alarmSeconds)
            .putInt(KEY_INACTIVITY_MIN, new.inactivityMinutes)
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
            inactivityMinutes = p.getInt(KEY_INACTIVITY_MIN, d.inactivityMinutes),
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
