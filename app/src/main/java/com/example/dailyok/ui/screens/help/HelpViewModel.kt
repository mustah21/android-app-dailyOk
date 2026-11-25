package com.example.dailyok.ui.screens.help

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dailyok.platform.help.HelpRequest
import com.example.dailyok.platform.help.HelpRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HelpUiState(
    /** The help request sent to the contacts when the screen opened. */
    val request: HelpRequest? = null,
)

/**
 * "I need help": opening the screen asks the contacts for help straight away ([HelpRequester]),
 * in the order set in Settings > Contacts. The only action left is cancelling.
 */
class HelpViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(HelpUiState())
    val state: StateFlow<HelpUiState> = _state.asStateFlow()

    /** Called when the screen opens: send the help request (only once while it is active). */
    fun onOpen() {
        _state.value = HelpUiState(request = HelpRequester.requestHelp(appContext))
    }

    /** Pressed by mistake or the person is fine: close the request and tell the contacts. */
    fun onCancelRequest() {
        HelpRequester.cancel(appContext)
        _state.value = HelpUiState(request = null)
    }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HelpViewModel(context.applicationContext) as T
        }
    }
}
