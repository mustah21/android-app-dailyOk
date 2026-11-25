package com.example.dailyok.ui.screens.meds

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailyok.data.MedicationStore
import com.example.dailyok.domain.meds.Dose
import com.example.dailyok.domain.meds.Medication
import com.example.dailyok.domain.meds.MedicationDay
import com.example.dailyok.domain.meds.MedicationSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.LocalTime

/** The add / edit medicine dialog. */
data class MedicationDraft(
    val id: String? = null,          // null = adding a new medicine
    val name: String = "",
    val dose: String = "",
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val nameError: Boolean = false,
    val timesError: Boolean = false,
)

data class MedsUiState(
    /** Every dose today, in time order. */
    val today: List<Dose> = emptyList(),
    val medications: List<Medication> = emptyList(),
    /** Earlier days, newest first. */
    val history: List<MedicationDay> = emptyList(),
    val draft: MedicationDraft? = null,
)

/** Updates the screen every 30 seconds, so "upcoming" turns into "time to take it" on its own. */
fun minuteClock(): Flow<LocalDateTime> = flow {
    while (true) {
        emit(LocalDateTime.now())
        delay(30_000)
    }
}

class MedsViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val draft = MutableStateFlow<MedicationDraft?>(null)

    val state: StateFlow<MedsUiState> =
        combine(MedicationStore.flow(appContext), minuteClock(), draft) { data, now, d ->
            val today = now.toLocalDate()
            MedsUiState(
                today = MedicationSchedule.dosesOn(today, data.medications, data.taken, now),
                medications = data.medications.sortedBy { it.times.min() },
                history = MedicationSchedule.history(data.medications, data.taken, now, days = HISTORY_DAYS + 1)
                    .filter { it.date != today },
                draft = d,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedsUiState())

    // Today
    fun onTaken(dose: Dose) = MedicationStore.markTaken(appContext, dose.medication.id, dose.date, dose.time)

    fun onUndoTaken(dose: Dose) = MedicationStore.undoTaken(appContext, dose.medication.id, dose.date, dose.time)

    // My medicines
    fun onAdd() {
        draft.value = MedicationDraft()
    }

    fun onEdit(id: String) {
        val med = MedicationStore.current(appContext).medications.firstOrNull { it.id == id } ?: return
        draft.value = MedicationDraft(id = med.id, name = med.name, dose = med.dose, times = med.times.sorted())
    }

    fun onDelete(id: String) = MedicationStore.delete(appContext, id)

    // Dialog
    fun onDraftNameChange(value: String) = draft.update { it?.copy(name = value, nameError = false) }

    fun onDraftDoseChange(value: String) = draft.update { it?.copy(dose = value) }

    fun onDraftAddTime(time: LocalTime) = draft.update { d ->
        d?.copy(times = (d.times + time).distinct().sorted(), timesError = false)
    }

    fun onDraftRemoveTime(time: LocalTime) = draft.update { it?.copy(times = it.times - time) }

    fun onDismissDraft() {
        draft.value = null
    }

    fun onSaveDraft() {
        val d = draft.value ?: return
        val nameBad = d.name.isBlank()
        val timesBad = d.times.isEmpty()
        if (nameBad || timesBad) {
            draft.value = d.copy(nameError = nameBad, timesError = timesBad)
            return
        }
        MedicationStore.save(
            appContext,
            Medication(
                id = d.id ?: MedicationStore.newId(),
                name = d.name.trim(),
                dose = d.dose.trim(),
                times = d.times.distinct().sorted(),
            ),
        )
        draft.value = null
    }

    companion object {
        const val HISTORY_DAYS = 7

        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MedsViewModel(context.applicationContext) as T
        }
    }
}
