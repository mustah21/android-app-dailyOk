package com.example.dailyok.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailyok.AppContainer
import com.example.dailyok.domain.interfaces.ActivityRepository
import com.example.dailyok.domain.interfaces.ActivitySource
import com.example.dailyok.domain.useCase.SyncLastActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeViewModel(
    context: Context,
    repo: ActivityRepository,
    private val source: ActivitySource,
    private val syncLastActivity: SyncLastActivity,
) : ViewModel() {

    private val preferences = HomePreferences(context)

    private val formatter = DateTimeFormatter.ofPattern("EEEE h:mm a")
        .withZone(ZoneId.systemDefault())

    private val _state = MutableStateFlow(
        HomeUiState(
            isPaused = preferences.isPaused(),
            lastOkClickTime = preferences.lastOkClickTime(),
            medication = preferences.loadMedication(),
            hasUsageAccess = source.hasPermission(),
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.lastActivity.collect { last ->
                _state.update { it.copy(lastActivity = last?.let(formatter::format)) }
            }
        }
    }

    /** Call from the screen's ON_RESUME (also covers returning from settings). */
    fun refresh() {
        val granted = source.hasPermission()
        _state.update { it.copy(hasUsageAccess = granted) }
        if (granted) viewModelScope.launch { syncLastActivity() }
    }

    fun onOkClicked() {
        if (!_state.value.canPressOk) return

        val now = System.currentTimeMillis()
        preferences.saveLastOkClickTime(now)

        _state.update { it.copy(lastOkClickTime = now) }
    }

    fun onPauseChanged(paused: Boolean) {
        preferences.savePaused(paused)
        _state.update { it.copy(isPaused = paused) }
    }

    fun onMedicationTaken() {
        val today = LocalDate.now().toEpochDay()
        preferences.saveMedicationTaken(today)

        _state.update {
            it.copy(medication = it.medication.copy(lastTakenEpochDay = today))
        }
    }

    fun setMedication(name: String, time: LocalTime) {
        preferences.saveMedication(name, time)

        _state.update {
            it.copy(medication = it.medication.copy(name = name, time = time))
        }
    }
    companion object {
        fun factory(context: Context, container: AppContainer) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(
                        context.applicationContext,
                        container.activityRepository,
                        container.activitySource,
                        container.syncLastActivity,
                    ) as T
            }
    }
}