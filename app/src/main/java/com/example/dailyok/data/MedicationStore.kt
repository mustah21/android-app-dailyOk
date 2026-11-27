package com.example.dailyok.data

import android.content.Context
import com.example.dailyok.data.room.MedicationDatabase
import com.example.dailyok.data.room.MedicationEntity
import com.example.dailyok.data.room.TakenRecordEntity
import com.example.dailyok.domain.meds.Medication
import com.example.dailyok.domain.meds.TakenRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** Everything the medication reminders know: the medicines and which doses were taken. */
data class MedicationData(
    val medications: List<Medication> = emptyList(),
    val taken: List<TakenRecord> = emptyList(),
)

/**
 * Medicines and taken-history, stored in Room.
 * Public API is identical to the old SharedPreferences version so all callers compile unchanged.
 */
object MedicationStore {

    private const val KEEP_DAYS = 60L

    /** Called after every change. DailyOkApp sets it to reschedule the reminder alarm. */
    var onChanged: ((Context) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var _flow: StateFlow<MedicationData>? = null

    fun flow(context: Context): StateFlow<MedicationData> =
        _flow ?: buildFlow(context).also { _flow = it }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun current(context: Context): MedicationData = runBlocking(Dispatchers.IO) {
        val dao = db(context).medicationDao()
        val since = LocalDate.now().minusDays(KEEP_DAYS).toString()
        MedicationData(
            medications = dao.getMedications().map { it.toDomain() },
            taken = dao.getTakenSince(since).map { it.toDomain() },
        )
    }

    fun newId(): String = UUID.randomUUID().toString()

    /** Adds a medication, or replaces the one with the same id. */
    fun save(context: Context, medication: Medication) {
        scope.launch {
            db(context).medicationDao().upsertMedication(medication.toEntity())
            notify(context)
        }
    }

    /** Removes a medication. Its past doses stay in the stored history. */
    fun delete(context: Context, id: String) {
        scope.launch {
            db(context).medicationDao().deleteMedication(id)
            notify(context)
        }
    }

    fun markTaken(
        context: Context,
        medicationId: String,
        date: LocalDate,
        time: LocalTime,
        takenAt: LocalDateTime = LocalDateTime.now(),
    ) {
        scope.launch {
            db(context).medicationDao().insertTaken(
                TakenRecordEntity(
                    medicationId = medicationId,
                    date = date.toString(),
                    scheduledTime = time.toString(),
                    takenAt = takenAt.toString(),
                )
            )
            notify(context)
        }
    }

    /** Tapped "I took it" by mistake. */
    fun undoTaken(context: Context, medicationId: String, date: LocalDate, time: LocalTime) {
        scope.launch {
            db(context).medicationDao().deleteTaken(medicationId, date.toString(), time.toString())
            notify(context)
        }
    }

    // ---- private ----

    private fun db(context: Context) = MedicationDatabase.get(context.applicationContext)

    private fun notify(context: Context) {
        onChanged?.invoke(context.applicationContext)
    }

    private fun buildFlow(context: Context): StateFlow<MedicationData> {
        val dao = db(context).medicationDao()
        val since = LocalDate.now().minusDays(KEEP_DAYS).toString()
        val combined: Flow<MedicationData> = combine(
            dao.observeMedications(),
            dao.observeTakenSince(since),
        ) { meds, taken ->
            MedicationData(
                medications = meds.map { it.toDomain() },
                taken = taken.map { it.toDomain() },
            )
        }
        return combined.stateIn(scope, SharingStarted.Eagerly, MedicationData())
    }

    // ---- Mappers ----

    private fun MedicationEntity.toDomain(): Medication = Medication(
        id = id,
        name = name,
        dose = dose,
        times = timesFromJson(timesJson),
    )

    private fun Medication.toEntity(): MedicationEntity = MedicationEntity(
        id = id,
        name = name,
        dose = dose,
        timesJson = timesToJson(times),
    )

    private fun TakenRecordEntity.toDomain(): TakenRecord = TakenRecord(
        medicationId = medicationId,
        date = LocalDate.parse(date),
        scheduledTime = LocalTime.parse(scheduledTime),
        takenAt = LocalDateTime.parse(takenAt),
    )

    private fun timesToJson(times: List<LocalTime>): String =
        JSONArray(times.map { it.toString() }).toString()

    private fun timesFromJson(json: String): List<LocalTime> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { LocalTime.parse(arr.getString(it)) }
    }
}
