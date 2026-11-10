package com.example.dailyok

import android.app.Application
import com.example.dailyok.ui.screens.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DailyOkApp : Application() {
    val settings: Settings by lazy { Settings() }
    val profileRepository: ProfileRepository by lazy { InMemoryProfileRepository(settings) }
}

class Settings {
    private val _onboardingDone = MutableStateFlow(value = false)
    val onboardingDone: Flow<Boolean> = _onboardingDone.asStateFlow()

    fun setOnboardingDone(done: Boolean) {
        _onboardingDone.update { done }
    }
}

class InMemoryProfileRepository(private val settings: Settings) : ProfileRepository {
    override suspend fun completeOnboarding(name: String, email: String, age: Int) {
        settings.setOnboardingDone(done = true)
    }
}
