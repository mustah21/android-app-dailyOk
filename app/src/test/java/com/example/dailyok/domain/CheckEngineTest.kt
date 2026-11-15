package com.example.dailyok.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Scripted days for the check logic.
 * Default config: wake 7:00, morning check 10:00, bed 22:00, inactivity 4 hours.
 */
class CheckEngineTest {

    private val config = CheckConfig()
    private val day: LocalDate = LocalDate.of(2026, 11, 16)
    private fun at(h: Int, m: Int = 0): LocalDateTime = day.atTime(h, m)
    private fun yesterdayAt(h: Int, m: Int = 0): LocalDateTime = day.minusDays(1).atTime(h, m)

    private fun decide(now: LocalDateTime, lastActivity: LocalDateTime?, lastPrompt: LocalDateTime? = null, cfg: CheckConfig = config) =
        CheckEngine.evaluate(now, lastActivity, lastPrompt, cfg)

    private fun next(now: LocalDateTime, lastActivity: LocalDateTime?, lastPrompt: LocalDateTime? = null, cfg: CheckConfig = config) =
        CheckEngine.nextCheckTime(now, lastActivity, lastPrompt, cfg)

    private val morningPrompt = CheckDecision.Prompt(CheckReason.MORNING_NO_ACTIVITY)
    private val daytimePrompt = CheckDecision.Prompt(CheckReason.DAYTIME_INACTIVITY)

    // ---- Normal day ----

    @Test fun `normal morning - active at 7_45 - no prompt at 10`() =
        assertEquals(CheckDecision.AllWell, decide(at(10), lastActivity = at(7, 45)))

    @Test fun `normal day - active every hour - no prompt in the afternoon`() =
        assertEquals(CheckDecision.AllWell, decide(at(15), lastActivity = at(14, 30)))

    // ---- Morning check ----

    @Test fun `lie-in - nothing since last night - prompt at morning check time`() =
        assertEquals(morningPrompt, decide(at(10), lastActivity = yesterdayAt(21, 30)))

    @Test fun `before morning check time - nothing yet - no prompt`() =
        assertEquals(CheckDecision.AllWell, decide(at(9, 59), lastActivity = yesterdayAt(21, 30)))

    @Test fun `activity in the night does not count as this morning`() =
        assertEquals(morningPrompt, decide(at(10, 5), lastActivity = at(3, 0)))

    @Test fun `first use of the app - no activity ever - prompt after morning check`() =
        assertEquals(morningPrompt, decide(at(11), lastActivity = null))

    @Test fun `morning check is scheduled for 10 when no activity yet`() =
        assertEquals(at(10), next(at(7, 30), lastActivity = yesterdayAt(21)))

    // ---- Daytime inactivity ----

    @Test fun `no activity for 4 hours in the afternoon - prompt`() =
        assertEquals(daytimePrompt, decide(at(16), lastActivity = at(12)))

    @Test fun `3 hours 59 minutes quiet - no prompt yet`() =
        assertEquals(CheckDecision.AllWell, decide(at(15, 59), lastActivity = at(12)))

    @Test fun `afternoon nap shorter than the inactivity period - no prompt`() =
        assertEquals(CheckDecision.AllWell, decide(at(15, 30), lastActivity = at(13)))

    @Test fun `shorter inactivity period of 2 hours prompts sooner`() =
        assertEquals(daytimePrompt, decide(at(14), lastActivity = at(12), cfg = config.copy(inactivityHours = 2)))

    @Test fun `next daytime check is last activity plus inactivity period`() =
        assertEquals(at(16, 30), next(at(13), lastActivity = at(12, 30)))

    // ---- Sleep hours ----

    @Test fun `never prompts during sleep hours`() {
        assertEquals(CheckDecision.AllWell, decide(at(23), lastActivity = at(12)))
        assertEquals(CheckDecision.AllWell, decide(at(5), lastActivity = yesterdayAt(12)))
    }

    @Test fun `quiet period that runs into bedtime does not prompt`() =
        assertEquals(CheckDecision.AllWell, decide(at(21, 59), lastActivity = at(19)))

    @Test fun `deadline after bedtime - next check is tomorrow morning`() =
        assertEquals(day.plusDays(1).atTime(10, 0), next(at(19), lastActivity = at(19)))

    @Test fun `in the evening after bed time - next check is tomorrow morning`() =
        assertEquals(day.plusDays(1).atTime(10, 0), next(at(22, 30), lastActivity = at(21)))

    // ---- Paused ----

    @Test fun `paused - never prompts`() {
        val paused = config.copy(paused = true)
        assertEquals(CheckDecision.AllWell, decide(at(11), lastActivity = null, cfg = paused))
        assertEquals(CheckDecision.AllWell, decide(at(18), lastActivity = at(8), cfg = paused))
    }

    // ---- Only one prompt per quiet period ----

    @Test fun `already prompted and still no activity - do not prompt again`() =
        assertEquals(CheckDecision.AllWell, decide(at(10, 30), lastActivity = yesterdayAt(21), lastPrompt = at(10)))

    @Test fun `new activity after a prompt - checks start again`() =
        assertEquals(daytimePrompt, decide(at(15), lastActivity = at(10, 5), lastPrompt = at(10)))

    @Test fun `while waiting after a prompt - look again within the hour`() =
        assertEquals(at(11), next(at(10), lastActivity = yesterdayAt(21), lastPrompt = at(10)))

    // ---- Demo ----

    @Test fun `demo inactivity of 2 minutes prompts after 2 minutes`() {
        val demo = config.copy(demoInactivityMinutes = 2)
        assertEquals(CheckDecision.AllWell, decide(at(14, 1), lastActivity = at(14), cfg = demo))
        assertEquals(daytimePrompt, decide(at(14, 2), lastActivity = at(14), cfg = demo))
        assertEquals(at(14, 2), next(at(14, 1), lastActivity = at(14), cfg = demo))
    }

    // ---- Config ----

    @Test(expected = IllegalArgumentException::class)
    fun `inactivity period outside 2 to 6 hours is rejected`() {
        config.copy(inactivityHours = 7)
    }

    @Test fun `custom times are respected`() {
        val early = CheckConfig(
            wakeTime = LocalTime.of(5, 30),
            morningCheckTime = LocalTime.of(8, 0),
            bedTime = LocalTime.of(20, 0),
        )
        assertEquals(morningPrompt, decide(at(8), lastActivity = yesterdayAt(19), cfg = early))
        assertEquals(CheckDecision.AllWell, decide(at(20, 30), lastActivity = at(14), cfg = early))
    }
}
