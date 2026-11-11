package com.example.dailyok.ui.screens.welcome

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

interface ProfileRepository {
    suspend fun completeOnboarding(name: String, email: String, birthDate: LocalDate?)
}

data class WelcomeUiState(
    val name: String = "",
    val email: String = "",
    val birthDate: LocalDate? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val birthDateError: String? = null,
    val saving: Boolean = false,
)

class WelcomeViewModel(
    private val profiles: ProfileRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, nameError = null) }

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null) }

    fun onBirthDateChange(value: LocalDate) =
        _state.update { it.copy(birthDate = value, birthDateError = null) }
    fun onContinue(onSaved: () -> Unit) {
        val s = _state.value
        if (s.saving) return

        val birthDate = s.birthDate
        val nameError = if (s.name.isBlank()) "Please enter your name" else null
        val emailError =
            if (!Patterns.EMAIL_ADDRESS.matcher(s.email.trim()).matches()) "Please enter a valid email" else null
        if (nameError != null || emailError != null ) {
            _state.update { it.copy(nameError = nameError, emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            profiles.completeOnboarding(s.name.trim(), s.email.trim(), birthDate)
            onSaved()
        }
    }

    private companion object {
        val birthDate_RANGE = 10..120
    }
}