package com.example.dailyok.ui.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dailyok.DailyOkApp
import com.example.dailyok.ui.screens.help.HelpRoute
import com.example.dailyok.ui.screens.help.HelpViewModel
import com.example.dailyok.ui.screens.home.HomeRoute
import com.example.dailyok.ui.screens.home.HomeViewModel
import com.example.dailyok.ui.screens.settings.SettingsRoute
import com.example.dailyok.ui.screens.settings.SettingsViewModel
import com.example.dailyok.ui.screens.welcome.WelcomeRoute
import com.example.dailyok.ui.screens.welcome.WelcomeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Routes {
    const val WELCOME = "welcome"
    const val CONTACTS = "contacts"
    const val HOME = "home"
    const val MEDS = "meds"
    const val HELP = "help"
    const val SETTINGS = "settings"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(app: DailyOkApp) {

    val onboardingDone by produceState<Boolean?>(initialValue = null) {
        value = app.settings.onboardingDone.first()
    }
    val done = onboardingDone ?: return
    val nav = rememberNavController()
    val context = LocalContext.current

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))
    val helpViewModel: HelpViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(
        navController = nav,
        startDestination = if (done) Routes.HOME else Routes.WELCOME,
    ) {
        composable(Routes.WELCOME) {
            val scope = rememberCoroutineScope()

            val vm: WelcomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { WelcomeViewModel(app.profileRepository, context) }
                }
            )

            WelcomeRoute(
                viewModel = vm,
                onFinished = {
                    scope.launch {
                        Log.d("Onboarding", "saving flag")
                        app.settings.setOnboardingDone(true)
                        Log.d("Onboarding", "saved")
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                },

            )
        }

        composable(Routes.CONTACTS) {
            // TODO: Implement contacts screen
        }


        composable(Routes.HOME) {
            HomeRoute(
                viewModel = homeViewModel,
                onNeedHelp = { nav.navigate(Routes.HELP)},
                onMedsClick = { nav.navigate(Routes.MEDS) },
                onSettingsClick = {nav.navigate(Routes.SETTINGS)}
                )
        }


        composable(Routes.SETTINGS) {
            SettingsRoute(viewModel = settingsViewModel)
        }


        composable(Routes.HELP) {
           HelpRoute(
               viewModel = helpViewModel,
           )
        }

    }
}


