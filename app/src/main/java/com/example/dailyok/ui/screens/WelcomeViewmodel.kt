package com.example.dailyok.ui.screens

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.Context

interface ProfileRepository {
    suspend fun completeOnboarding(name: String, email: String, age: Int)
}

data class WelcomeUiState(
    val name: String = "",
    val email: String = "",
    val age: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val ageError: String? = null,
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

    fun onAgeChange(value: String) =
        _state.update { it.copy(age = value.filter(Char::isDigit).take(3), ageError = null) }

    fun onContinue(onSaved: () -> Unit) {
        val s = _state.value
        if (s.saving) return

        val age = s.age.toIntOrNull()
        val nameError = if (s.name.isBlank()) "Please enter your name" else null
        val emailError =
            if (!Patterns.EMAIL_ADDRESS.matcher(s.email.trim()).matches()) "Please enter a valid email" else null
        val ageError =
            if (age == null || age !in AGE_RANGE) "Please enter an age from ${AGE_RANGE.first} to ${AGE_RANGE.last}" else null

        if (nameError != null || emailError != null || ageError != null) {
            _state.update { it.copy(nameError = nameError, emailError = emailError, ageError = ageError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            profiles.completeOnboarding(s.name.trim(), s.email.trim(), age!!)
            onSaved()
        }
    }

    private companion object {
        val AGE_RANGE = 10..120
    }
}