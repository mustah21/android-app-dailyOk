package com.example.dailyok.domain.meds

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MedicationScheduleTest {

    private val day: LocalDate = LocalDate.of(2026, 11, 24)
    private fun at(h: Int, m: Int = 0): LocalDateTime = day.atTime(h, m)
    private fun t(h: Int, m: Int = 0): LocalTime = LocalTime.of(h, m)

    private val bp = Medication("bp", "Blood pressure", "1 tablet", listOf(t(8), t(20)))
    private val vitD = Medication("d", "Vitamin D", times = listOf(t(12)))
    private val meds = listOf(bp, vitD)

    private fun taken(med: Medication, time: LocalTime, takenAt: LocalDateTime, date: LocalDate = day) =
        TakenRecord(med.id, date, time, takenAt)

    private fun statuses(doses: List<Dose>) = doses.map { "${it.medication.id}@${it.time}:${it.status}" }

    @Test fun `doses of the day are in time order`() =
        assertEquals(
            listOf("bp@08:00:UPCOMING", "d@12:00:UPCOMING", "bp@20:00:UPCOMING"),
            statuses(MedicationSchedule.dosesOn(day, meds, emptyList(), at(7))),
        )

    @Test fun `dose is due from its time`() =
        assertEquals(DoseStatus.DUE, MedicationSchedule.dosesOn(day, meds, emptyList(), at(8))[0].status)

    @Test fun `dose is missed 3 hours after its time`() {
        assertEquals(DoseStatus.DUE, MedicationSchedule.dosesOn(day, meds, emptyList(), at(10, 59))[0].status)
        assertEquals(DoseStatus.MISSED, MedicationSchedule.dosesOn(day, meds, emptyList(), at(11))[0].status)
    }

    @Test fun `taken dose shows when it was taken`() {
        val dose = MedicationSchedule.dosesOn(day, meds, listOf(taken(bp, t(8), at(8, 10))), at(9))[0]
        assertEquals(DoseStatus.TAKEN, dose.status)
        assertEquals(at(8, 10), dose.takenAt)
    }

    @Test fun `taking the morning dose does not count for the evening one`() =
        assertEquals(
            listOf("bp@08:00:TAKEN", "d@12:00:UPCOMING", "bp@20:00:UPCOMING"),
            statuses(MedicationSchedule.dosesOn(day, meds, listOf(taken(bp, t(8), at(8, 5))), at(9))),
        )

    @Test fun `yesterday's record does not count today`() =
        assertEquals(
            DoseStatus.DUE,
            MedicationSchedule.dosesOn(day, meds, listOf(taken(bp, t(8), at(8), day.minusDays(1))), at(9))[0].status,
        )

    @Test fun `home preview - due doses first, then upcoming`() =
        assertEquals(
            listOf("bp@08:00:DUE", "d@12:00:UPCOMING", "bp@20:00:UPCOMING"),
            statuses(MedicationSchedule.upcoming(meds, emptyList(), at(8, 30))),
        )

    @Test fun `home preview - taken and missed doses are left out`() =
        assertEquals(
            listOf("d@12:00:DUE", "bp@20:00:UPCOMING", "bp@08:00:UPCOMING"),
            statuses(MedicationSchedule.upcoming(meds, emptyList(), at(12, 0))),
        )

    @Test fun `home preview - after the last dose shows tomorrow`() {
        val doses = MedicationSchedule.upcoming(meds, listOf(taken(bp, t(20), at(20))), at(21), limit = 2)
        assertEquals(listOf(day.plusDays(1), day.plusDays(1)), doses.map { it.date })
        assertEquals(listOf(t(8), t(12)), doses.map { it.time })
    }

    @Test fun `home preview - no medications, nothing to show`() =
        assertEquals(emptyList<Dose>(), MedicationSchedule.upcoming(emptyList(), emptyList(), at(9)))

    @Test fun `history - newest day first with taken and missed`() {
        val history = MedicationSchedule.history(
            listOf(vitD),
            listOf(taken(vitD, t(12), at(12, 15).minusDays(1), date = day.minusDays(1))),
            at(9),
            days = 3,
        )
        assertEquals(listOf(day, day.minusDays(1), day.minusDays(2)), history.map { it.date })
        assertEquals(listOf(DoseStatus.UPCOMING), history[0].doses.map { it.status })
        assertEquals(listOf(DoseStatus.TAKEN), history[1].doses.map { it.status })
        assertEquals(listOf(DoseStatus.MISSED), history[2].doses.map { it.status })
        assertEquals(1, history[1].takenCount)
    }

    @Test fun `next reminder is the next dose time`() =
        assertEquals(at(12), MedicationSchedule.nextReminder(meds, emptyList(), at(9)))

    @Test fun `next reminder after the last dose is tomorrow morning`() =
        assertEquals(day.plusDays(1).atTime(8, 0), MedicationSchedule.nextReminder(meds, emptyList(), at(20, 30)))

    @Test fun `no medications - no reminder`() =
        assertEquals(null, MedicationSchedule.nextReminder(emptyList(), emptyList(), at(9)))

    @Test fun `reminder lists the doses at that time`() {
        val both = listOf(bp, vitD.copy(times = listOf(t(8))))
        assertEquals(2, MedicationSchedule.dueAt(at(8), both, emptyList(), at(8)).size)
        assertEquals(1, MedicationSchedule.dueAt(at(8), both, listOf(taken(bp, t(8), at(8))), at(8)).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `medication without times is rejected`() {
        Medication("x", "Aspirin", times = emptyList())
    }
}
