package com.example.dailyok.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dailyok.DailyOkApp
import com.example.dailyok.ui.screens.WelcomeRoute
import com.example.dailyok.ui.screens.WelcomeViewModel
import kotlinx.coroutines.flow.first

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
    val context = LocalContext.current
    NavHost(
        navController = nav,
        startDestination = if (done) Routes.HOME else Routes.WELCOME,
    ) {
        composable(Routes.WELCOME) {
            val vm: WelcomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { WelcomeViewModel(app.profileRepository, context) }
                }
            )
            WelcomeRoute(vm) {
                nav.navigate(Routes.CONTACTS) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }
        composable(Routes.CONTACTS) {
            // TODO: Implement contacts screen
        }
        composable(Routes.HOME) {
            // TODO: Implement home screen
        }
    }
}
