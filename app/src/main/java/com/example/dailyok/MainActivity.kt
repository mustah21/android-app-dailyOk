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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.dailyok.data.CheckStore
import com.example.dailyok.data.DemoSettings
import com.example.dailyok.platform.check.ActivityTracker
import com.example.dailyok.platform.check.CheckNotifier
import com.example.dailyok.platform.check.CheckScheduler
import com.example.dailyok.platform.help.HelpRequester
import com.example.dailyok.platform.sms.SmsAlertSender
import com.example.dailyok.ui.checkin.CheckInHost
import com.example.dailyok.ui.navigation.AppNavGraph
import com.example.dailyok.ui.theme.DailyOkTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val smsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DailyOkApp
        handleCheckInIntent(intent)
        askForNotificationPermission()
        askForSmsPermission()
        setContent {
            val promptPending by CheckScheduler.promptPending.collectAsState()
            val demo by DemoSettings.flow(this).collectAsState()   // Settings > Demo

            val savedContacts by app.userManager.contactsFlow.collectAsState(initial = emptyList())
            val nightMode by app.userManager.nightModeFlow.collectAsState(initial = false)
            val contactNames = if (savedContacts.isEmpty()) listOf("Contact 1", "Contact 2") else savedContacts.map { it.name }
            val contactPhones = savedContacts.map { it.phone }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !nightMode
                    controller.isAppearanceLightNavigationBars = !nightMode
                }
            }

            DailyOkTheme(darkTheme = nightMode) {
                CheckInHost(
                    contactNames = contactNames,
                    timings = demo.timings,
                    showDemoTrigger = demo.enabled,
                    promptPending = promptPending,
                    onPromptShown = CheckScheduler::onPromptShown,
                    onCancelAlert = { HelpRequester.cancel(this) },   // "Help is on the way" > Cancel
                    onImOk = {
                        ActivityTracker.record(this, "I'm OK pressed")
                        showOverLockScreen(false)
                    },
                    onSendAlert = { _ ->
                        val lastActivityMs = CheckStore(this).lastActivity()
                            ?.atZone(java.time.ZoneId.systemDefault())
                            ?.toInstant()?.toEpochMilli() ?: 0L
                        val lastActivityText = SmsAlertSender.formatActivityTime(lastActivityMs)
                        SmsAlertSender.sendNoAnswerAlert(
                            context = this,
                            contacts = contactPhones,
                            lastActivity = lastActivityText,
                            lastLocation = null, // location not yet implemented
                        )
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

    private fun askForSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermission.launch(Manifest.permission.SEND_SMS)
        }
    }
}
