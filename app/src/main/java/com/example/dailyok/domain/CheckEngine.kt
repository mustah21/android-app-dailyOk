package com.example.dailyok.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Settings that drive the daily checks.
 *
 * Assumes the person wakes up and goes to bed on the same calendar day (or spanning midnight).
 */
data class CheckConfig(
    /** Start of the waking day. Activity before this is "last night", not "this morning". */
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    /** By this time the person should have been active at least once since [wakeTime]. */
    val morningCheckTime: LocalTime = LocalTime.of(10, 0),
    /** End of the waking day. No daytime check runs after this. */
    val bedTime: LocalTime = LocalTime.of(22, 0),
    /** Daytime: how long without any activity before we check on the person (2–6 hours). */
    val inactivityHours: Int = 4,
    /** Holidays, hospital stays: no checks at all. */
    val paused: Boolean = false,
    /** Demo only: use this many seconds instead of [inactivityHours]. Null = normal behaviour. */
    val demoInactivitySeconds: Int? = null,
) {
    init {
        val wakeMins = wakeTime.hour * 60 + wakeTime.minute
        val bedMins = bedTime.hour * 60 + bedTime.minute
        val sleepMinutes = if (wakeMins > bedMins) wakeMins - bedMins else (1440 - bedMins) + wakeMins
        val awakeMinutes = 1440 - sleepMinutes
        require(awakeMinutes >= MIN_INACTIVITY_HOURS * 60 && sleepMinutes >= 4 * 60) {
            "Invalid sleep/awake duration"
        }
        val morningMins = morningCheckTime.hour * 60 + morningCheckTime.minute
        val morningValid = if (bedMins > wakeMins) {
            morningMins in wakeMins..bedMins
        } else {
            morningMins >= wakeMins || morningMins < bedMins
        }
        require(morningValid) {
            "Expected morningCheckTime between wakeTime and bedTime"
        }
        require(inactivityHours in MIN_INACTIVITY_HOURS..MAX_INACTIVITY_HOURS) {
            "inactivityHours must be $MIN_INACTIVITY_HOURS..$MAX_INACTIVITY_HOURS"
        }
        require(demoInactivitySeconds == null || demoInactivitySeconds in 10..3600) {
            "demoInactivitySeconds must be 10..3600"
        }
    }

    val inactivity: Duration
        get() = demoInactivitySeconds?.let { Duration.ofSeconds(it.toLong()) }
            ?: Duration.ofHours(inactivityHours.toLong())

    companion object {
        const val MIN_INACTIVITY_HOURS = 2
        const val MAX_INACTIVITY_HOURS = 6
    }
}

/** Why the app is checking on the person. */
enum class CheckReason {
    /** No activity since waking up by the morning check time. */
    MORNING_NO_ACTIVITY,
    /** No activity for longer than the inactivity period during the day. */
    DAYTIME_INACTIVITY,
}

/** What the app should do right now. */
sealed interface CheckDecision {
    /** Nothing to do. */
    data object AllWell : CheckDecision

    /** Show the "Are you OK?" prompt (which then leads to the alarm and the SMS). */
    data class Prompt(val reason: CheckReason) : CheckDecision
}

/**
 * The check logic as a pure function: time and recent events in, decision out.
 * No Android code here, so every rule can be unit tested with scripted days.
 *
 * Rules:
 *  1. Paused → never check.
 *  2. Morning check: at [CheckConfig.morningCheckTime], if there has been no activity since
 *     [CheckConfig.wakeTime] today → prompt.
 *  3. Daytime check: between the morning check (or the first activity of the day, if earlier)
 *     and [CheckConfig.bedTime], no activity for [CheckConfig.inactivity] → prompt.
 *     Sleep time never counts as inactivity.
 *  4. Only one prompt per quiet period: once the person has been prompted, they are not
 *     prompted again until there has been new activity (the prompt → alarm → SMS flow
 *     takes over from there).
 */
object CheckEngine {

    fun evaluate(
        now: LocalDateTime,
        lastActivity: LocalDateTime?,
        lastPrompt: LocalDateTime?,
        config: CheckConfig,
    ): CheckDecision {
        if (config.paused) return CheckDecision.AllWell
        if (alreadyPrompted(lastActivity, lastPrompt)) return CheckDecision.AllWell

        val isDemo = config.demoInactivitySeconds != null
        val today = now.toLocalDate()
        val wake = today.atTime(config.wakeTime)
        val morningCheck = today.atTime(config.morningCheckTime)
        val bed = if (config.bedTime > config.wakeTime) {
            today.atTime(config.bedTime)
        } else {
            today.plusDays(1).atTime(config.bedTime)
        }

        // Sleep hours: nothing to check (unless running in demo mode with custom seconds).
        if (!isDemo && (now < wake || now >= bed)) return CheckDecision.AllWell

        val activeSinceWaking = lastActivity != null && (isDemo || lastActivity >= wake)

        // Rule 2: morning check.
        if (!activeSinceWaking) {
            return if (now >= morningCheck) {
                CheckDecision.Prompt(CheckReason.MORNING_NO_ACTIVITY)
            } else {
                CheckDecision.AllWell
            }
        }

        // Rule 3: daytime inactivity (lastActivity is today, after waking).
        val deadline = lastActivity!!.plus(config.inactivity)
        return if (now >= deadline && (isDemo || deadline < bed)) {
            CheckDecision.Prompt(CheckReason.DAYTIME_INACTIVITY)
        } else {
            CheckDecision.AllWell
        }
    }

    /**
     * When the app should run [evaluate] next. Schedule an alarm for this time, and also
     * re-run [evaluate] whenever new activity is recorded or the settings change.
     */
    fun nextCheckTime(
        now: LocalDateTime,
        lastActivity: LocalDateTime?,
        lastPrompt: LocalDateTime?,
        config: CheckConfig,
    ): LocalDateTime {
        // Paused, or waiting for the person to respond: look again in an hour,
        // and anyway at the next morning check.
        if (config.paused || alreadyPrompted(lastActivity, lastPrompt)) {
            return minOf(now.plusHours(1), nextMorningCheck(now, config))
        }

        val isDemo = config.demoInactivitySeconds != null
        val today = now.toLocalDate()
        val wake = today.atTime(config.wakeTime)
        val morningCheck = today.atTime(config.morningCheckTime)
        val bed = if (config.bedTime > config.wakeTime) {
            today.atTime(config.bedTime)
        } else {
            today.plusDays(1).atTime(config.bedTime)
        }

        if (!isDemo && (now < wake || now >= bed)) return nextMorningCheck(now, config)

        val activeSinceWaking = lastActivity != null && (isDemo || lastActivity >= wake)
        if (!activeSinceWaking) {
            return if (now < morningCheck) morningCheck else now // overdue: check right away
        }

        val deadline = lastActivity!!.plus(config.inactivity)
        return when {
            !isDemo && deadline >= bed -> nextMorningCheck(now, config) // would fall into sleep hours
            deadline <= now -> now
            else -> deadline
        }
    }

    private fun alreadyPrompted(lastActivity: LocalDateTime?, lastPrompt: LocalDateTime?): Boolean =
        lastPrompt != null && (lastActivity == null || lastPrompt > lastActivity)

    private fun nextMorningCheck(now: LocalDateTime, config: CheckConfig): LocalDateTime {
        val todayCheck = now.toLocalDate().atTime(config.morningCheckTime)
        return if (now < todayCheck) todayCheck else todayCheck.plusDays(1)
    }
}
