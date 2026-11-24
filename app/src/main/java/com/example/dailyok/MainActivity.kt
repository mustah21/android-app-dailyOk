package com.example.dailyok

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.dailyok.data.CheckStore
import com.example.dailyok.data.DemoSettings
import com.example.dailyok.platform.check.ActivityTracker
import com.example.dailyok.platform.check.CheckNotifier
import com.example.dailyok.platform.check.CheckScheduler
import com.example.dailyok.platform.help.HelpRequester
import com.example.dailyok.ui.checkin.CheckInHost
import com.example.dailyok.ui.navigation.AppNavGraph
import com.example.dailyok.ui.theme.DailyOkTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DailyOkApp
        handleCheckInIntent(intent)
        askForNotificationPermission()
        setContent {
            val promptPending by CheckScheduler.promptPending.collectAsState()
            val demo by DemoSettings.flow(this).collectAsState()   // Settings > Demo

//            TEST> this is for testing purpose, we remove when app is final
            DailyOkTheme {
                CheckInHost(
                    contactNames = emptyList(),
                    timings = demo.timings,
                    showDemoTrigger = demo.enabled,
                    promptPending = promptPending,
                    onPromptShown = CheckScheduler::onPromptShown,
                    onCancelAlert = { HelpRequester.cancel(this) },   // "Help is on the way" > Cancel
                    onImOk = {
                        ActivityTracker.record(this, "I'm OK pressed")
                        showOverLockScreen(false)
                    },
                ) {
                    AppNavGraph(app)   // your normal screen
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCheckInIntent(intent)
    }

    /** Opened from the "Are you OK?" notification: show the prompt, even over the lock screen. */
    private fun handleCheckInIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(CheckNotifier.EXTRA_START_CHECKIN, false) != true) return
        intent.removeExtra(CheckNotifier.EXTRA_START_CHECKIN)
        showOverLockScreen(true)
        CheckScheduler.requestPrompt()
    }

    private fun showOverLockScreen(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(show)
            setTurnScreenOn(show)
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
