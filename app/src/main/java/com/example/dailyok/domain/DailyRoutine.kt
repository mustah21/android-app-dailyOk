package com.example.dailyok.domain

import java.time.Duration
import java.time.LocalTime

/**
 * The person's own day, entered by hand in Settings ("I usually wake up at… / go to bed at…").
 * Replaces learning the routine automatically: simpler, and the person stays in control.
 *
 * The morning check is set automatically to [MORNING_CHECK_AFTER_WAKE] after waking up.
 */
object DailyRoutine {

    /** How long after waking up we expect a first sign of life. */
    val MORNING_CHECK_AFTER_WAKE: Duration = Duration.ofHours(3)

    /** Shortest day we accept, so the morning check always falls before bed time. */
    val MIN_AWAKE: Duration = Duration.ofHours(4)

    sealed interface Result {
        data class Ok(val config: CheckConfig) : Result
        data class Invalid(val message: String) : Result
    }

    /** Applies new wake and bed times to [config], or explains why they can't be used. */
    fun apply(config: CheckConfig, wakeTime: LocalTime, bedTime: LocalTime): Result {
        val wakeMins = wakeTime.hour * 60 + wakeTime.minute
        val bedMins = bedTime.hour * 60 + bedTime.minute

        if (wakeMins == bedMins) {
            return Result.Invalid("Wake time and bed time cannot be the same.")
        }

        val sleepMinutes = if (wakeMins > bedMins) {
            wakeMins - bedMins
        } else {
            (1440 - bedMins) + wakeMins
        }

        val awakeMinutes = 1440 - sleepMinutes
        if (awakeMinutes < MIN_AWAKE.toMinutes() || sleepMinutes < 4 * 60) {
            return Result.Invalid("Please allow at least 4 hours of sleep and 4 hours awake time.")
        }

        val morningCheck = morningCheckFor(wakeTime)
        val morningMins = morningCheck.hour * 60 + morningCheck.minute
        val morningValid = if (bedMins > wakeMins) {
            morningMins in wakeMins..bedMins
        } else {
            morningMins >= wakeMins || morningMins < bedMins
        }
        if (!morningValid) {
            return Result.Invalid("Morning check must be before bed time.")
        }

        return Result.Ok(
            config.copy(
                wakeTime = wakeTime,
                morningCheckTime = morningCheck,
                bedTime = bedTime,
            )
        )
    }

    fun morningCheckFor(wakeTime: LocalTime): LocalTime = wakeTime.plus(MORNING_CHECK_AFTER_WAKE)
}
