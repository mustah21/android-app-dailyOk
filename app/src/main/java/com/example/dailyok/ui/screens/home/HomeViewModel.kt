package com.example.dailyok.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

class HomeViewModel(
    context: Context
) : ViewModel() {

    private val preferences = HomePreferences(context)

    private val _state = MutableStateFlow(
        HomeUiState(
            isPaused = preferences.isPaused(),
            lastOkClickTime = preferences.lastOkClickTime(),
            medication = preferences.loadMedication()
        )
    )

    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onOkClicked() {
        if (!_state.value.canPressOk) return

        val now = System.currentTimeMillis()

        preferences.saveLastOkClickTime(now)

        _state.update {
            it.copy(lastOkClickTime = now)
        }
    }

    fun onPauseChanged(paused: Boolean) {
        preferences.savePaused(paused)

        _state.update {
            it.copy(isPaused = paused)
        }
    }

    fun onMedicationTaken() {
        val today = LocalDate.now().toEpochDay()

        preferences.saveMedicationTaken(today)

        _state.update {
            it.copy(
                medication = it.medication.copy(
                    lastTakenEpochDay = today
                )
            )
        }
    }

    fun setMedication(
        name: String,
        time: LocalTime
    ) {
        preferences.saveMedication(name, time)

        _state.update {
            it.copy(
                medication = it.medication.copy(
                    name = name,
                    time = time
                )
            )
        }
    }

    companion object {
        fun factory(context: Context) =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return HomeViewModel(
                        context.applicationContext
                    ) as T
                }
            }
    }
}