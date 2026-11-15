package com.example.dailyok.data

import android.content.Context
import com.example.dailyok.domain.CheckConfig
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Everything the daily checks need to remember, stored only on the phone.
 *
 * Uses the same "app_prefs" file as HomeViewModel, so the home screen's pause switch
 * ("is_paused") and its "I'm OK" button ("last_click_time") feed straight into the checks.
 */
class CheckStore(private val context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Latest activity of any kind: pickup, unlock, charger unplugged, "I'm OK", app opened. */
    fun lastActivity(): LocalDateTime? {
        val ms = maxOf(prefs.getLong(KEY_LAST_ACTIVITY, 0L), prefs.getLong(KEY_HOME_OK_CLICK, 0L))
        return ms.toLocalDateTimeOrNull()
    }

    fun recordActivity(atMs: Long = System.currentTimeMillis()) {
        if (atMs > prefs.getLong(KEY_LAST_ACTIVITY, 0L)) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY, atMs).apply()
        }
    }

    /** When the "Are you OK?" prompt was last started by a check. */
    fun lastPrompt(): LocalDateTime? = prefs.getLong(KEY_LAST_PROMPT, 0L).toLocalDateTimeOrNull()

    fun recordPrompt(atMs: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_PROMPT, atMs).apply()
    }

    fun config(): CheckConfig {
        val defaults = CheckConfig()
        return CheckConfig(
            wakeTime = prefs.getTime(KEY_WAKE_MIN, defaults.wakeTime),
            morningCheckTime = prefs.getTime(KEY_MORNING_MIN, defaults.morningCheckTime),
            bedTime = prefs.getTime(KEY_BED_MIN, defaults.bedTime),
            inactivityHours = prefs.getInt(KEY_INACTIVITY_HOURS, defaults.inactivityHours)
                .coerceIn(CheckConfig.MIN_INACTIVITY_HOURS, CheckConfig.MAX_INACTIVITY_HOURS),
            paused = prefs.getBoolean(KEY_PAUSED, false),
            demoInactivitySeconds = DemoSettings.current(context).activeInactivitySeconds,
        )
    }

    /** Call from the Settings screen. Throws if the times are not wake < morning check < bed. */
    fun saveConfig(config: CheckConfig) {
        prefs.edit()
            .putInt(KEY_WAKE_MIN, config.wakeTime.toMinuteOfDay())
            .putInt(KEY_MORNING_MIN, config.morningCheckTime.toMinuteOfDay())
            .putInt(KEY_BED_MIN, config.bedTime.toMinuteOfDay())
            .putInt(KEY_INACTIVITY_HOURS, config.inactivityHours)
            .putBoolean(KEY_PAUSED, config.paused)
            .apply()
    }

    fun setInactivityHours(hours: Int) = saveConfig(config().copy(inactivityHours = hours))

    // ---- Demo helpers (Settings > Demo) ----

    /** Set the last activity to any time, even earlier than the stored one. */
    fun setLastActivityForDemo(atMs: Long) {
        prefs.edit().putLong(KEY_LAST_ACTIVITY, atMs).putLong(KEY_HOME_OK_CLICK, 0L).apply()
    }

    /** Forget the last prompt, so the next check can prompt again. */
    fun clearPrompt() {
        prefs.edit().remove(KEY_LAST_PROMPT).apply()
    }

    private fun android.content.SharedPreferences.getTime(key: String, default: LocalTime): LocalTime {
        val minutes = getInt(key, -1)
        return if (minutes < 0) default else LocalTime.of(minutes / 60, minutes % 60)
    }

    private fun LocalTime.toMinuteOfDay() = hour * 60 + minute

    private fun Long.toLocalDateTimeOrNull(): LocalDateTime? =
        if (this <= 0L) null
        else LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

    companion object {
        const val PREFS_NAME = "app_prefs"
        // Shared with HomeViewModel:
        private const val KEY_PAUSED = "is_paused"
        private const val KEY_HOME_OK_CLICK = "last_click_time"
        // Owned by the checks:
        private const val KEY_LAST_ACTIVITY = "check_last_activity_ms"
        private const val KEY_LAST_PROMPT = "check_last_prompt_ms"
        private const val KEY_WAKE_MIN = "check_wake_minute"
        private const val KEY_MORNING_MIN = "check_morning_minute"
        private const val KEY_BED_MIN = "check_bed_minute"
        private const val KEY_INACTIVITY_HOURS = "check_inactivity_hours"
    }
}
