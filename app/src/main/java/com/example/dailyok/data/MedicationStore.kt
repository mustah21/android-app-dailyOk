package com.example.dailyok.data

import android.content.Context
import androidx.core.content.edit
import com.example.dailyok.domain.meds.Medication
import com.example.dailyok.domain.meds.TakenRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
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
 * Medicines and the taken-history, stored only on the phone.
 * The home screen and the Medicines screen both collect [flow], so they always agree.
 */
object MedicationStore {

    private const val PREFS_NAME = "meds_prefs"
    private const val KEY_MEDICATIONS = "medications_json"
    private const val KEY_TAKEN = "taken_json"

    /** How long the taken-history is kept. */
    private const val KEEP_DAYS = 60L

    /** Called after every change. DailyOkApp sets it to reschedule the reminder alarm. */
    var onChanged: ((Context) -> Unit)? = null

    private var state: MutableStateFlow<MedicationData>? = null

    fun flow(context: Context): StateFlow<MedicationData> = stateFor(context).asStateFlow()

    fun current(context: Context): MedicationData = stateFor(context).value

    fun newId(): String = UUID.randomUUID().toString()

    /** Adds a medication, or replaces the one with the same id. */
    fun save(context: Context, medication: Medication) = update(context) { data ->
        val exists = data.medications.any { it.id == medication.id }
        data.copy(
            medications = if (exists) data.medications.map { if (it.id == medication.id) medication else it }
            else data.medications + medication,
        )
    }

    /** Removes a medication. Its past doses stay in the stored history. */
    fun delete(context: Context, id: String) = update(context) { data ->
        data.copy(medications = data.medications.filterNot { it.id == id })
    }

    fun markTaken(
        context: Context,
        medicationId: String,
        date: LocalDate,
        time: LocalTime,
        takenAt: LocalDateTime = LocalDateTime.now(),
    ) = update(context) { data ->
        if (data.taken.any { it.medicationId == medicationId && it.date == date && it.scheduledTime == time }) data
        else data.copy(taken = data.taken + TakenRecord(medicationId, date, time, takenAt))
    }

    /** Tapped "I took it" by mistake. */
    fun undoTaken(context: Context, medicationId: String, date: LocalDate, time: LocalTime) = update(context) { data ->
        data.copy(taken = data.taken.filterNot { it.medicationId == medicationId && it.date == date && it.scheduledTime == time })
    }

    @Synchronized
    private fun update(context: Context, change: (MedicationData) -> MedicationData) {
        val flow = stateFor(context)
        val oldest = LocalDate.now().minusDays(KEEP_DAYS)
        val new = change(flow.value).let { it.copy(taken = it.taken.filter { r -> r.date >= oldest }) }
        if (new == flow.value) return
        flow.value = new
        prefs(context).edit {
            putString(KEY_MEDICATIONS, medicationsToJson(new.medications))
            putString(KEY_TAKEN, takenToJson(new.taken))
        }
        onChanged?.invoke(context.applicationContext)
    }

    @Synchronized
    private fun stateFor(context: Context): MutableStateFlow<MedicationData> =
        state ?: MutableStateFlow(load(context)).also { state = it }

    private fun load(context: Context): MedicationData {
        val p = prefs(context)
        return MedicationData(
            medications = runCatching { medicationsFromJson(p.getString(KEY_MEDICATIONS, null)) }.getOrDefault(emptyList()),
            taken = runCatching { takenFromJson(p.getString(KEY_TAKEN, null)) }.getOrDefault(emptyList()),
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- JSON ----

    private fun medicationsToJson(list: List<Medication>): String = JSONArray().apply {
        list.forEach { m ->
            put(JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("dose", m.dose)
                put("times", JSONArray(m.times.map { it.toString() }))
            })
        }
    }.toString()

    private fun medicationsFromJson(json: String?): List<Medication> {
        if (json == null) return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                val times = o.getJSONArray("times")
                Medication(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    dose = o.optString("dose", ""),
                    times = (0 until times.length()).map { LocalTime.parse(times.getString(it)) },
                )
            }.getOrNull()
        }
    }

    private fun takenToJson(list: List<TakenRecord>): String = JSONArray().apply {
        list.forEach { r ->
            put(JSONObject().apply {
                put("medicationId", r.medicationId)
                put("date", r.date.toString())
                put("time", r.scheduledTime.toString())
                put("takenAt", r.takenAt.toString())
            })
        }
    }.toString()

    private fun takenFromJson(json: String?): List<TakenRecord> {
        if (json == null) return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                TakenRecord(
                    medicationId = o.getString("medicationId"),
                    date = LocalDate.parse(o.getString("date")),
                    scheduledTime = LocalTime.parse(o.getString("time")),
                    takenAt = LocalDateTime.parse(o.getString("takenAt")),
                )
            }.getOrNull()
        }
    }
}
