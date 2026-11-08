package com.example.dailyok.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.dailyok.ui.screens.WelcomeRoute
import com.example.dailyok.ui.screens.WelcomeViewModel

object Routes {
    const val WELCOME = "welcome"
    const val CONTACTS = "contacts"
    const val HOME = "home"
}

@Composable
fun AppNavGraph(app: DailyOkApp) {

    val onboardingDone by produceState<Boolean?>(initialValue = null) {
        value = app.settings.onboardingDone.first()
    }
    val done = onboardingDone ?: return

    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = if (done) Routes.HOME else Routes.WELCOME,
    ) {
        composable(Routes.WELCOME) {
            val vm: WelcomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { WelcomeViewModel(app.profileRepository) }
                }
            )
            WelcomeRoute(vm) {
                nav.navigate(Routes.CONTACTS) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }
        composable(Routes.CONTACTS) { /* TODO contacts screen */ }
        composable(Routes.HOME) { /* TODO home screen */ }
    }
}