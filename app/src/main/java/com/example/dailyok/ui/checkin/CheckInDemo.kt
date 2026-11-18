package com.example.dailyok.ui.checkin

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dailyok.data.CheckStore
import com.example.dailyok.ui.screens.AlarmScreen
import com.example.dailyok.ui.screens.CheckInPromptScreen
import com.example.dailyok.ui.screens.HelpSentScreen
import com.example.dailyok.ui.theme.BodyText
import com.example.dailyok.ui.theme.HeadingText
import kotlinx.coroutines.delay

/** Where the check-in currently is. */
enum class CheckInStep { None, Prompt, Alarm, HelpSent }

/**
 * Timings. Real app: 10 min to answer the prompt, then 15 min of alarm before the SMS.
 * For the demo they are short, so the whole flow can be shown in about a minute.
 */
data class CheckInTimings(val promptSeconds: Int, val alarmSeconds: Int) {
    companion object {
        val Real = CheckInTimings(promptSeconds = 10 * 60, alarmSeconds = 15 * 60)
        val Demo = CheckInTimings(promptSeconds = 15, alarmSeconds = 30)
    }
}

/**
 * Runs the check-in flow on top of the normal app content:
 * Prompt -> (no answer) -> Alarm with countdown -> (no answer) -> Help sent.
 * "I'm OK" at any step goes back to the normal screen.
 *
 * Hidden demo trigger: long-press the invisible 64dp square in the top-right corner
 * to start the prompt or jump straight to the alarm.
 * Remove [showDemoTrigger] (set it to false) before real users get the app.
 */
@Composable
fun CheckInHost(
    contactNames: List<String>,
    timings: CheckInTimings = CheckInTimings.Demo,
    showDemoTrigger: Boolean = true,
    onSendAlert: (List<String>) -> Unit = {},   // later: send the real SMS here
    onCancelAlert: () -> Unit = {},              // later: send "all OK, sorry" SMS here
    promptPending: Boolean = false,              // a background check wants the prompt (CheckScheduler)
    onPromptShown: () -> Unit = {},              // called once the prompt is on screen
    onImOk: () -> Unit = {},                     // "I'm OK" pressed: counts as activity
    content: @Composable () -> Unit,             // the normal app (home screen etc.)
) {
    var step by rememberSaveable { mutableStateOf(CheckInStep.None) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var demoMenuOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val store = remember { CheckStore(context) }
    val activeContactNames = remember(step, contactNames) {
        val storeContacts = store.contacts()
        if (storeContacts.isNotEmpty()) {
            storeContacts.map { it.name.substringBefore(" (") }
        } else if (contactNames.isNotEmpty()) {
            contactNames
        } else {
            emptyList()
        }
    }

    fun syncWithStore() {
        val flowStep = store.checkInStep()
        val now = System.currentTimeMillis()
        when (flowStep) {
            "PROMPT" -> {
                val elapsed = ((now - store.promptStartMs()) / 1000).toInt()
                if (elapsed < timings.promptSeconds) {
                    step = CheckInStep.Prompt
                } else {
                    val rem = timings.alarmSeconds - (elapsed - timings.promptSeconds)
                    if (rem > 0) {
                        secondsLeft = rem
                        step = CheckInStep.Alarm
                    } else {
                        step = CheckInStep.HelpSent
                    }
                }
            }
            "ALARM" -> {
                val elapsed = ((now - store.alarmStartMs()) / 1000).toInt()
                val rem = timings.alarmSeconds - elapsed
                if (rem > 0) {
                    secondsLeft = rem
                    step = CheckInStep.Alarm
                } else {
                    step = CheckInStep.HelpSent
                }
            }
            "HELP_SENT" -> {
                step = CheckInStep.HelpSent
            }
        }
    }

    fun startAlarm() {
        store.startAlarmFlow()
        secondsLeft = timings.alarmSeconds   // set before showing, so the countdown never flashes 0:00
        step = CheckInStep.Alarm
    }

    // A morning or daytime check asked for the prompt (see CheckScheduler).
    LaunchedEffect(promptPending, Unit) {
        syncWithStore()
        if (promptPending) {
            if (store.checkInStep() == "NONE") {
                store.startPromptFlow()
            }
            if (step == CheckInStep.None) step = CheckInStep.Prompt
            onPromptShown()
        }
    }

    // Timers & clock sync: compute step and remaining countdown based on real time.
    LaunchedEffect(step) {
        while (true) {
            syncWithStore()
            if (step == CheckInStep.Alarm && secondsLeft > 0) {
                delay(1000)
            } else if (step == CheckInStep.Prompt) {
                delay(1000)
            } else {
                delay(2000)
            }
        }
    }

    val imOk = {
        store.clearCheckInFlow()
        onImOk()
        step = CheckInStep.None
    }

    Box(Modifier.fillMaxSize()) {
        when (step) {
            CheckInStep.None -> content()
            CheckInStep.Prompt -> CheckInPromptScreen(greeting = greetingForNow(), onImOk = imOk)
            CheckInStep.Alarm -> AlarmScreen(
                contactName = activeContactNames.firstOrNull() ?: "your contacts",
                secondsLeft = secondsLeft,
                onImOk = imOk,
            )
            CheckInStep.HelpSent -> HelpSentScreen(
                contactNames = activeContactNames,
                onCancel = {
                    store.clearCheckInFlow()
                    onCancelAlert()
                    step = CheckInStep.None
                },
            )
        }

        if (showDemoTrigger) {
            // Invisible, not announced by TalkBack: for the presenter only.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { demoMenuOpen = true })
                    },
            )
        }
    }

    if (demoMenuOpen) {
        AlertDialog(
            onDismissRequest = { demoMenuOpen = false },
            title = { Text("Demo", style = HeadingText) },
            text = { Text("Show a check-in screen:", style = BodyText) },
            confirmButton = {
                TextButton(onClick = { step = CheckInStep.Prompt; demoMenuOpen = false }) {
                    Text("Start prompt")
                }
            },
            dismissButton = {
                TextButton(onClick = { startAlarm(); demoMenuOpen = false }) {
                    Text("Alarm now")
                }
            },
        )
    }
}

private fun greetingForNow(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning!"
        in 12..17 -> "Good afternoon!"
        else -> "Good evening!"
    }
}
