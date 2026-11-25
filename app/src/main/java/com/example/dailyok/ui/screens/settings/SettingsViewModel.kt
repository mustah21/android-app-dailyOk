package com.example.dailyok.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dailyok.data.CheckStore
import com.example.dailyok.domain.DailyRoutine
import com.example.dailyok.platform.check.CheckScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
)

data class ContactDraft(
    val id: String? = null,          // null = adding a new contact
    val name: String = "",
    val phone: String = "",
    val nameError: Boolean = false,
    val phoneError: Boolean = false,
)

data class ActivityAlert(
    val time: LocalTime,
    val message: String,
    val urgent: Boolean,
)

data class ActivityDay(
    val date: LocalDate,
    val summary: String,
    val alerts: List<ActivityAlert>,
)

data class SettingsUiState(
    val contacts: List<Contact> = emptyList(),
    val inactivityHours: Int = 4,
    /** My day: entered by the person, no checks while they sleep. */
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val bedTime: LocalTime = LocalTime.of(22, 0),
    val morningCheckTime: LocalTime = LocalTime.of(10, 0),
    val routineError: String? = null,
    val draft: ContactDraft? = null,          // non-null = dialog is open
    val activity: List<ActivityDay> = emptyList(),
)

class SettingsViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val store = CheckStore(appContext)

    private val _state = MutableStateFlow(
        SettingsUiState(
            contacts = store.contacts(),
            inactivityHours = store.config().inactivityHours,
            wakeTime = store.config().wakeTime,
            bedTime = store.config().bedTime,
            morningCheckTime = store.config().morningCheckTime,
            activity = sampleActivity(),
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    // Inactivity period
    fun onInactivityHoursChange(hours: Int) {
        val validHours = hours.coerceIn(MIN_HOURS, MAX_HOURS)
        store.setInactivityHours(validHours)
        CheckScheduler.reschedule(appContext)
        _state.update { it.copy(inactivityHours = validHours) }
    }

    // My day: wake up and bed time
    fun onWakeTimeChange(time: LocalTime) = saveRoutine(time, _state.value.bedTime)

    fun onBedTimeChange(time: LocalTime) = saveRoutine(_state.value.wakeTime, time)

    private fun saveRoutine(wake: LocalTime, bed: LocalTime) {
        when (val result = DailyRoutine.apply(store.config(), wake, bed)) {
            is DailyRoutine.Result.Ok -> {
                store.saveConfig(result.config)
                CheckScheduler.reschedule(appContext)
                _state.update {
                    it.copy(
                        wakeTime = result.config.wakeTime,
                        bedTime = result.config.bedTime,
                        morningCheckTime = result.config.morningCheckTime,
                        routineError = null,
                    )
                }
            }
            is DailyRoutine.Result.Invalid -> _state.update { it.copy(routineError = result.message) }
        }
    }

    // Contact dialog
    fun onAddContact() =
        _state.update { it.copy(draft = ContactDraft()) }

    fun onEditContact(id: String) = _state.update { s ->
        val c = s.contacts.firstOrNull { it.id == id } ?: return@update s
        s.copy(draft = ContactDraft(id = c.id, name = c.name, phone = c.phone))
    }

    fun onDraftNameChange(value: String) = _state.update { s ->
        val d = s.draft ?: return@update s
        s.copy(draft = d.copy(name = value, nameError = false))
    }

    fun onDraftPhoneChange(value: String) = _state.update { s ->
        val d = s.draft ?: return@update s
        s.copy(draft = d.copy(phone = value, phoneError = false))
    }

    fun onDismissDraft() =
        _state.update { it.copy(draft = null) }

    fun onSaveContact() = _state.update { s ->
        val d = s.draft ?: return@update s
        val nameBad = d.name.isBlank()
        val phoneBad = d.phone.count(Char::isDigit) < 6
        if (nameBad || phoneBad) {
            return@update s.copy(draft = d.copy(nameError = nameBad, phoneError = phoneBad))
        }
        val saved = Contact(
            id = d.id ?: UUID.randomUUID().toString(),
            name = d.name.trim(),
            phone = d.phone.trim(),
        )
        val list = if (d.id == null) s.contacts + saved
        else s.contacts.map { if (it.id == saved.id) saved else it }
        store.saveContacts(list)
        s.copy(contacts = list, draft = null)
    }

    /**
     * Moves a contact up (-1) or down (+1) in the list. The order is who "I need help" calls
     * first, second and so on, before emergency services.
     */
    fun onMoveContact(id: String, by: Int) = _state.update { s ->
        val from = s.contacts.indexOfFirst { it.id == id }
        val to = from + by
        if (from < 0 || to !in s.contacts.indices) return@update s
        val list = s.contacts.toMutableList().apply { add(to, removeAt(from)) }
        store.saveContacts(list)
        s.copy(contacts = list)
    }

    fun onDeleteContact(id: String) = _state.update { s ->
        val list = s.contacts.filterNot { it.id == id }
        store.saveContacts(list)
        s.copy(contacts = list)
    }

    companion object {
        const val MIN_HOURS = 2
        const val MAX_HOURS = 6

        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(context.applicationContext) as T
        }
    }
}

private fun sampleActivity(): List<ActivityDay> {
    val today = LocalDate.now()
    return listOf(
        ActivityDay(today, "Normal activity", emptyList()),
        ActivityDay(
            today.minusDays(1), "1 alert",
            listOf(
                ActivityAlert(LocalTime.of(14, 20), "No activity for 4 hours", urgent = true),
                ActivityAlert(LocalTime.of(14, 35), "Anna confirmed you are OK", urgent = false),
            ),
        ),
        ActivityDay(today.minusDays(2), "Normal activity", emptyList()),
        ActivityDay(
            today.minusDays(3), "1 alert",
            listOf(ActivityAlert(LocalTime.of(11, 5), "No activity for 4 hours", urgent = true)),
        ),
        ActivityDay(today.minusDays(4), "Normal activity", emptyList()),
    )
}