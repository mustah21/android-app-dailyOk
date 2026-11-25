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
        if (!bedTime.isAfter(wakeTime)) {
            return Result.Invalid("Bed time must be later in the day than waking up time.")
        }
        if (Duration.between(wakeTime, bedTime) < MIN_AWAKE) {
            return Result.Invalid("Please allow at least ${MIN_AWAKE.toHours()} hours between waking up and bed time.")
        }
        return Result.Ok(
            config.copy(
                wakeTime = wakeTime,
                morningCheckTime = morningCheckFor(wakeTime),
                bedTime = bedTime,
            )
        )
    }

    fun morningCheckFor(wakeTime: LocalTime): LocalTime = wakeTime.plus(MORNING_CHECK_AFTER_WAKE)
}
