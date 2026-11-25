package com.example.dailyok.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailyok.data.CheckStore
import com.example.dailyok.data.UserManager
import com.example.dailyok.domain.DailyRoutine
import com.example.dailyok.platform.check.CheckScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import com.example.dailyok.data.db.ActivityEventEntity
import com.example.dailyok.data.db.AppDatabase
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId


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
    val draft: ContactDraft? = null,
    val activity: List<ActivityDay> = emptyList(),
    )

class SettingsViewModel(
    private val userManager: UserManager? = null,
    context: Context? = null,
) : ViewModel() {

    private val appContext = context?.applicationContext
    private val store = appContext?.let { CheckStore(it) }

    private val _state = MutableStateFlow(
        SettingsUiState(
            contacts = if (userManager == null) sampleContacts() else emptyList(),
            inactivityHours = store?.config()?.inactivityHours ?: 4,
            wakeTime = store?.config()?.wakeTime ?: LocalTime.of(7, 0),
            bedTime = store?.config()?.bedTime ?: LocalTime.of(22, 0),
            morningCheckTime = store?.config()?.morningCheckTime ?: LocalTime.of(10, 0),
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        userManager?.let { manager ->
            viewModelScope.launch {
                manager.contactsFlow.collect { contactList ->
                    _state.update { it.copy(contacts = contactList) }
                }
            }
        }

        appContext?.let { ctx ->
            viewModelScope.launch {
                AppDatabase.get(ctx).activityEventDao().observeLatest()
                    .map { it.toActivityDays() }
                    .collect { days -> _state.update { it.copy(activity = days) } }
            }
        }

    }

    // Inactivity period
    fun onInactivityHoursChange(hours: Int) {
        val validHours = hours.coerceIn(MIN_HOURS, MAX_HOURS)
        store?.setInactivityHours(validHours)
        appContext?.let { CheckScheduler.reschedule(it) }
        _state.update { it.copy(inactivityHours = validHours) }
    }

    // My day: wake up and bed time
    fun onWakeTimeChange(time: LocalTime) = saveRoutine(time, _state.value.bedTime)

    fun onBedTimeChange(time: LocalTime) = saveRoutine(_state.value.wakeTime, time)

    private fun saveRoutine(wake: LocalTime, bed: LocalTime) {
        val config = store?.config() ?: return
        when (val result = DailyRoutine.apply(config, wake, bed)) {
            is DailyRoutine.Result.Ok -> {
                store.saveConfig(result.config)
                appContext?.let { CheckScheduler.reschedule(it) }
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

    fun onSaveContact() {
        val s = _state.value
        val d = s.draft ?: return
        val nameBad = d.name.isBlank()
        val phoneBad = userManager?.let { !UserManager.PhoneValidator.isValid(d.phone) } ?: false
        if (nameBad || phoneBad) {
            _state.update { it.copy(draft = d.copy(nameError = nameBad, phoneError = phoneBad)) }
            return
        }

        val trimmedName = d.name.trim()
        val trimmedPhone = d.phone.trim()

        if (userManager != null) {
            viewModelScope.launch {
                userManager.saveContact(id = d.id, name = trimmedName, phone = trimmedPhone)
            }
            _state.update { it.copy(draft = null) }
        } else {
            val saved = Contact(
                id = d.id ?: UUID.randomUUID().toString(),
                name = trimmedName,
                phone = trimmedPhone,
            )
            val list = if (d.id == null) s.contacts + saved
            else s.contacts.map { if (it.id == saved.id) saved else it }
            store?.saveContacts(list)
            _state.update { it.copy(contacts = list, draft = null) }
        }
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
        store?.saveContacts(list)
        s.copy(contacts = list)
    }

    fun onDeleteContact(id: String) {
        if (userManager != null) {
            viewModelScope.launch {
                userManager.removeContact(id)
            }
        } else {
            _state.update { s ->
                val list = s.contacts.filterNot { it.id == id }
                store?.saveContacts(list)
                s.copy(contacts = list)
            }
        }
    }

    companion object {
        const val MIN_HOURS = 2
        const val MAX_HOURS = 6

        fun factory(userManager: UserManager, context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(userManager = userManager, context = context.applicationContext) as T
        }

        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(context = context.applicationContext) as T
        }

    }
}

private fun sampleContacts() = listOf(
    Contact(name = "Anna (daughter)", phone = "+358 40 123 4567"),
    Contact(name = "Matti (neighbour)", phone = "+358 50 765 4321"),
)

private fun List<ActivityEventEntity>.toActivityDays(): List<ActivityDay> {
    val zone = ZoneId.systemDefault()
    return map { it to Instant.ofEpochMilli(it.timestamp).atZone(zone) }
        .groupBy({ (_, time) -> time.toLocalDate() }, { it })
        .map { (date, items) ->
            ActivityDay(
                date = date,
                summary = if (items.size == 1) "1 activity" else "${items.size} activities",
                alerts = items.map { (event, time) ->
                    ActivityAlert(
                        time = time.toLocalTime(),
                        message = event.type.replaceFirstChar { it.uppercase() },
                        urgent = false,
                    )
                },
            )
        }
}