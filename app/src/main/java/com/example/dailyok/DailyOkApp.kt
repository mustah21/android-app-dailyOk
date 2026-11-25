package com.example.dailyok

import android.app.Application
import androidx.work.Configuration
import com.example.dailyok.data.MedicationStore
import com.example.dailyok.data.UserManager
import com.example.dailyok.platform.check.ActivityTracker
import com.example.dailyok.platform.meds.MedicationReminders
import com.example.dailyok.platform.check.CheckScheduler
import com.example.dailyok.platform.check.CheckService
import com.example.dailyok.platform.worker.ActivityScheduler
import com.example.dailyok.platform.worker.AppWorkerFactory
import com.example.dailyok.ui.screens.welcome.ProfileRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DailyOkApp : Application(), Configuration.Provider {
    val userManager: UserManager by lazy { UserManager(this) }
    val userSettingsStore: UserManager get() = userManager

    val profileRepository: ProfileRepository by lazy {
        DataStoreProfileRepository(userManager)
    }

    val settings: AppSettings by lazy { AppSettings(userManager) }

    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(AppWorkerFactory(container.syncLastActivity))
            .build()

    override fun onCreate() {
        super.onCreate()
        ActivityTracker.registerSystemSignals(this)
        CheckScheduler.reschedule(this)
        CheckService.start(this)
        ActivityScheduler.schedule(this)   // 12-hour usage log sync
        // Medication reminders: keep the alarm on the next dose after every change.
        MedicationStore.onChanged = MedicationReminders::schedule
        MedicationReminders.schedule(this)
    }
}

class AppSettings(private val userManager: UserManager) {
    val onboardingDone: Flow<Boolean> get() = userManager.onboardingDoneFlow

    suspend fun setOnboardingDone(done: Boolean) {
        userManager.setOnboardingDone(done)
    }
}

class DataStoreProfileRepository(
    private val userManager: UserManager
) : ProfileRepository {
    override suspend fun completeOnboarding(name: String, email: String, birthDate: LocalDate?) {
        // dob is saved as an Int (epochDay or 0)
        val epochDay = birthDate?.toEpochDay()?.toInt() ?: 0
        userManager.storeUser(dob = epochDay, name = name, mail = email)
    }
}
