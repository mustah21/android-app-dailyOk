package com.example.dailyok

import android.app.Application
import com.example.dailyok.data.UserManager
import com.example.dailyok.platform.check.ActivityTracker
import com.example.dailyok.platform.check.CheckScheduler
import com.example.dailyok.ui.screens.welcome.ProfileRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DailyOkApp : Application() {
    val userManager: UserManager by lazy { UserManager(this) }
    val userSettingsStore: UserManager get() = userManager

    val profileRepository: ProfileRepository by lazy {
        DataStoreProfileRepository(userManager)
    }

    // Settings accessor delegating directly to DataStore
    val settings: AppSettings by lazy { AppSettings(userManager) }

    override fun onCreate() {
        super.onCreate()
        ActivityTracker.registerSystemSignals(this)
        CheckScheduler.reschedule(this)
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