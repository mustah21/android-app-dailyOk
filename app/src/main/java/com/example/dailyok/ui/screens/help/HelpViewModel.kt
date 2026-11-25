package com.example.dailyok.ui.screens.help

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailyok.data.UserManager
import com.example.dailyok.platform.sms.SmsAlertSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HelpUiState(
    // True once "I need help" has been tapped and SMS sent.
    val helpSent: Boolean = false,
    // True while the SMS is being sent (brief, but avoids double-taps).
    val sending: Boolean = false,
)

class HelpViewModel(
    private val userManager: UserManager,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(HelpUiState())
    val state: StateFlow<HelpUiState> = _state.asStateFlow()

    fun requestHelp() {
        if (_state.value.sending || _state.value.helpSent) return
        _state.update { it.copy(sending = true) }

        viewModelScope.launch {
            val contacts = userManager.contactsFlow.first()
            val phones = contacts.map { it.phone }
            SmsAlertSender.sendHelpRequest(
                context = appContext,
                contacts = phones,
                lastLocation = null,  // location not yet implemented
            )
            _state.update { it.copy(sending = false, helpSent = true) }
        }
    }

    fun cancelHelp() {
        if (!_state.value.helpSent) return

        viewModelScope.launch {
            val contacts = userManager.contactsFlow.first()
            val phones = contacts.map { it.phone }
            SmsAlertSender.sendCancellation(
                context = appContext,
                contacts = phones,
            )
            _state.update { it.copy(helpSent = false) }
        }
    }

    companion object {
        fun factory(userManager: UserManager, context: Context) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HelpViewModel(userManager, context.applicationContext) as T
            }
    }
}
