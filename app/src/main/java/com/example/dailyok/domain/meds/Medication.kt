package com.example.dailyok.domain.meds

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** A medicine the person takes, at one or more times every day. */
data class Medication(
    val id: String,
    val name: String,
    /** Free text, e.g. "1 tablet" or "5 ml". Empty if not given. */
    val dose: String = "",
    val times: List<LocalTime>,
) {
    init {
        require(name.isNotBlank()) { "Medication needs a name" }
        require(times.isNotEmpty()) { "Medication needs at least one time" }
    }
}

/** A record that one scheduled dose was taken. Kept for the history. */
data class TakenRecord(
    val medicationId: String,
    val date: LocalDate,
    val scheduledTime: LocalTime,
    val takenAt: LocalDateTime,
)

enum class DoseStatus {
    /** Later today. */
    UPCOMING,
    /** Time has come, not taken yet. */
    DUE,
    /** Taken. */
    TAKEN,
    /** Not taken and the time is long past. */
    MISSED,
}

/** One scheduled dose of a medication on one day. */
data class Dose(
    val medication: Medication,
    val date: LocalDate,
    val time: LocalTime,
    val status: DoseStatus,
    val takenAt: LocalDateTime? = null,
) {
    val dateTime: LocalDateTime get() = date.atTime(time)
}

/** History for one day: every dose and whether it was taken. */
data class MedicationDay(val date: LocalDate, val doses: List<Dose>) {
    val takenCount: Int get() = doses.count { it.status == DoseStatus.TAKEN }
}

/**
 * The medication rules as pure functions (no Android code), so they can be unit tested.
 *
 * A dose is DUE from its time until [MISSED_AFTER] later, then it counts as MISSED.
 * It can still be marked as taken while due or missed on the same day.
 */
object MedicationSchedule {

    val MISSED_AFTER: Duration = Duration.ofHours(3)

    /** All doses on [date], in time order. */
    fun dosesOn(date: LocalDate, medications: List<Medication>, taken: List<TakenRecord>, now: LocalDateTime): List<Dose> =
        medications.flatMap { med ->
            med.times.distinct().map { time ->
                val record = taken.firstOrNull { it.medicationId == med.id && it.date == date && it.scheduledTime == time }
                Dose(med, date, time, statusOf(date.atTime(time), record, now), record?.takenAt)
            }
        }.sortedWith(compareBy({ it.time }, { it.medication.name }))

    /**
     * What to show on the home screen: doses not taken yet, the ones due now first,
     * then the next ones (continuing into tomorrow if today is done).
     */
    fun upcoming(medications: List<Medication>, taken: List<TakenRecord>, now: LocalDateTime, limit: Int = 3): List<Dose> {
        val today = now.toLocalDate()
        val todayOpen = dosesOn(today, medications, taken, now)
            .filter { it.status == DoseStatus.DUE || it.status == DoseStatus.UPCOMING }
        val tomorrow = dosesOn(today.plusDays(1), medications, taken, now)
        return (todayOpen.sortedBy { if (it.status == DoseStatus.DUE) 0 else 1 } + tomorrow).take(limit)
    }

    /** The last [days] days, newest first, today included. Days with no doses are left out. */
    fun history(medications: List<Medication>, taken: List<TakenRecord>, now: LocalDateTime, days: Int = 7): List<MedicationDay> =
        (0 until days).map { now.toLocalDate().minusDays(it.toLong()) }
            .map { date -> MedicationDay(date, dosesOn(date, medications, taken, now)) }
            .filter { it.doses.isNotEmpty() }

    /** When the next reminder should fire, or null if there are no medications. */
    fun nextReminder(medications: List<Medication>, taken: List<TakenRecord>, now: LocalDateTime): LocalDateTime? {
        val today = now.toLocalDate()
        return (dosesOn(today, medications, taken, now) + dosesOn(today.plusDays(1), medications, taken, now))
            .filter { it.status == DoseStatus.UPCOMING }
            .minOfOrNull { it.dateTime }
    }

    /** The doses whose time is exactly [at] (what a reminder fired for), not taken yet. */
    fun dueAt(at: LocalDateTime, medications: List<Medication>, taken: List<TakenRecord>, now: LocalDateTime): List<Dose> =
        dosesOn(at.toLocalDate(), medications, taken, now)
            .filter { it.time == at.toLocalTime() && it.status != DoseStatus.TAKEN }

    private fun statusOf(at: LocalDateTime, record: TakenRecord?, now: LocalDateTime): DoseStatus = when {
        record != null -> DoseStatus.TAKEN
        now < at -> DoseStatus.UPCOMING
        now < at.plus(MISSED_AFTER) -> DoseStatus.DUE
        else -> DoseStatus.MISSED
    }
}
