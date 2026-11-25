package com.example.dailyok

import android.app.Application
import androidx.work.Configuration
import com.example.dailyok.data.MedicationStore
import com.example.dailyok.platform.check.ActivityTracker
import com.example.dailyok.platform.meds.MedicationReminders
import com.example.dailyok.platform.check.CheckScheduler
import com.example.dailyok.platform.check.CheckService
import com.example.dailyok.platform.worker.ActivityScheduler
import com.example.dailyok.platform.worker.AppWorkerFactory
import com.example.dailyok.ui.screens.welcome.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class DailyOkApp : Application(), Configuration.Provider {
    val settings: Settings by lazy { Settings() }
    val profileRepository: ProfileRepository by lazy { InMemoryProfileRepository(settings) }

    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(AppWorkerFactory(container.syncLastActivity))
            .build()

    override fun onCreate() {
        super.onCreate()
        // Morning and daytime checks: listen for unlocks/unplugging and set the next check alarm.
        ActivityTracker.registerSystemSignals(this)
        CheckScheduler.reschedule(this)
        CheckService.start(this)
        ActivityScheduler.schedule(this)   // 12-hour usage log sync
        // Medication reminders: keep the alarm on the next dose after every change.
        MedicationStore.onChanged = MedicationReminders::schedule
        MedicationReminders.schedule(this)
    }
}

class Settings {
    private val _onboardingDone = MutableStateFlow(value = true)
    val onboardingDone: Flow<Boolean> = _onboardingDone.asStateFlow()

    fun setOnboardingDone(done: Boolean) {
        _onboardingDone.update { done }
    }
}

class InMemoryProfileRepository(private val settings: Settings) : ProfileRepository {
    override suspend fun completeOnboarding(name: String, email: String, birthDate: LocalDate?) {
        settings.setOnboardingDone(done = true)
    }
}