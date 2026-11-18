package com.example.dailyok.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dailyok.data.CheckStore
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