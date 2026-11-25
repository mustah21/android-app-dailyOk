package com.example.dailyok.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class DailyRoutineTest {

    private val config = CheckConfig()
    private fun t(h: Int, m: Int = 0): LocalTime = LocalTime.of(h, m)

    @Test fun `wake and bed times are saved and morning check is 3 hours after waking`() {
        val result = DailyRoutine.apply(config, t(6, 30), t(21, 30))
        assertEquals(
            DailyRoutine.Result.Ok(config.copy(wakeTime = t(6, 30), morningCheckTime = t(9, 30), bedTime = t(21, 30))),
            result,
        )
    }

    @Test fun `late riser - morning check moves later too`() {
        val result = DailyRoutine.apply(config, t(9), t(23, 30)) as DailyRoutine.Result.Ok
        assertEquals(t(12), result.config.morningCheckTime)
    }

    @Test fun `other settings are kept`() {
        val custom = config.copy(inactivityHours = 2, paused = true)
        val result = DailyRoutine.apply(custom, t(8), t(22)) as DailyRoutine.Result.Ok
        assertEquals(2, result.config.inactivityHours)
        assertEquals(true, result.config.paused)
    }

    @Test fun `bed time before wake time is rejected`() {
        assertEquals(true, DailyRoutine.apply(config, t(7), t(1)) is DailyRoutine.Result.Invalid)
    }

    @Test fun `same wake and bed time is rejected`() {
        assertEquals(true, DailyRoutine.apply(config, t(7), t(7)) is DailyRoutine.Result.Invalid)
    }

    @Test fun `less than 4 hours awake is rejected`() {
        assertEquals(true, DailyRoutine.apply(config, t(7), t(10, 59)) is DailyRoutine.Result.Invalid)
    }

    @Test fun `exactly 4 hours awake is accepted`() {
        assertEquals(true, DailyRoutine.apply(config, t(7), t(11)) is DailyRoutine.Result.Ok)
    }
}
