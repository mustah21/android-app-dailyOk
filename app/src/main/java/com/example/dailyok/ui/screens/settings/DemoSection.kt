package com.example.dailyok.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dailyok.data.CheckStore
import com.example.dailyok.data.DemoConfig
import com.example.dailyok.data.DemoSettings
import com.example.dailyok.domain.CheckConfig
import com.example.dailyok.domain.CheckEngine
import com.example.dailyok.platform.check.CheckScheduler
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Settings > Demo: mock values and shortcuts for presenting the app.
 * Changes apply immediately and are saved on the phone.
 */
@Composable
fun DemoSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val demo by DemoSettings.flow(context).collectAsState()
    val store = remember { CheckStore(context) }

    // Status line refreshes every few seconds and after every action.
    var refresh by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(CheckStatus.read(store)) }
    LaunchedEffect(refresh, demo) {
        while (true) {
            status = CheckStatus.read(store)
            delay(3_000)
        }
    }
    fun afterChange() {
        CheckScheduler.reschedule(context)
        refresh++
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Demo", style = MaterialTheme.typography.titleLarge)
            Text(
                "For presentations only. Remove or hide before real users get the app.",
                style = MaterialTheme.typography.bodySmall,
            )

            // On / off
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Demo mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = demo.enabled,
                    onCheckedChange = { on -> DemoSettings.update(context) { it.copy(enabled = on) }; afterChange() },
                )
            }

            if (demo.enabled) {
                DemoSlider(
                    label = "Prompt waits ${demo.promptSeconds} s before the alarm",
                    value = demo.promptSeconds,
                    range = DemoConfig.PROMPT_SECONDS_RANGE,
                    onChange = { v -> DemoSettings.update(context) { it.copy(promptSeconds = v) } },
                )
                DemoSlider(
                    label = "Alarm counts down ${demo.alarmSeconds} s before the SMS",
                    value = demo.alarmSeconds,
                    range = DemoConfig.ALARM_SECONDS_RANGE,
                    onChange = { v -> DemoSettings.update(context) { it.copy(alarmSeconds = v) } },
                )
                DemoSlider(
                    label = if (demo.inactivityMinutes == 0) {
                        "Daytime check: normal setting (hours)"
                    } else {
                        "Daytime check after ${demo.inactivityMinutes} min without activity"
                    },
                    value = demo.inactivityMinutes,
                    range = DemoConfig.INACTIVITY_MINUTES_RANGE,
                    onChange = { v -> DemoSettings.update(context) { it.copy(inactivityMinutes = v) }; afterChange() },
                )
            } else {
                Text(
                    "Real timings: prompt 10 min, alarm 15 min, daytime check in hours.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider()
            Text("Try a check now", style = MaterialTheme.typography.titleMedium)

            DemoButton("Simulate a missed morning") {
                // Morning check 1 minute ago, woke up 30 minutes ago, last seen before that.
                val now = LocalTime.now()
                val safe = CheckConfig()
                val missedMorning = runCatching {
                    store.config().copy(
                        wakeTime = now.minusMinutes(30),
                        morningCheckTime = now.minusMinutes(1),
                        bedTime = now.plusHours(1),
                    )
                }.getOrElse { safe } // close to midnight: times would wrap around the day
                store.saveConfig(missedMorning)
                store.setLastActivityForDemo(System.currentTimeMillis() - 45 * 60_000L)
                store.clearPrompt()
                CheckScheduler.runCheck(context)
                refresh++
            }
            DemoButton("Simulate inactivity (daytime check)") {
                // Last activity was just over the inactivity period ago.
                val minutes = store.config().inactivity.toMinutes() + 1
                store.setLastActivityForDemo(System.currentTimeMillis() - minutes * 60_000L)
                store.clearPrompt()
                CheckScheduler.runCheck(context)
                refresh++
            }
            DemoButton("Run the check now") {
                CheckScheduler.runCheck(context)
                refresh++
            }
            DemoButton("Reset check times to normal") {
                val normal = CheckConfig()
                store.saveConfig(
                    store.config().copy(
                        wakeTime = normal.wakeTime,
                        morningCheckTime = normal.morningCheckTime,
                        bedTime = normal.bedTime,
                    ),
                )
                store.clearPrompt()
                afterChange()
            }

            HorizontalDivider()
            Text("Status", style = MaterialTheme.typography.titleMedium)
            Text(status.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DemoSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(text)
    }
}

/** What the checks currently know, as readable text for the demo. */
private data class CheckStatus(val text: String) {
    companion object {
        private val fmt = DateTimeFormatter.ofPattern("EEE HH:mm:ss")

        fun read(store: CheckStore): CheckStatus {
            val config = store.config()
            val lastActivity = store.lastActivity()
            val lastPrompt = store.lastPrompt()
            val now = LocalDateTime.now()
            val next = CheckEngine.nextCheckTime(now, lastActivity, lastPrompt, config)
            val text = buildString {
                appendLine("Wake ${config.wakeTime.hm()} · morning check ${config.morningCheckTime.hm()} · bed ${config.bedTime.hm()}")
                appendLine("Daytime check after ${config.inactivity.toMinutes()} min" + if (config.paused) " · PAUSED" else "")
                appendLine("Last activity: ${lastActivity?.format(fmt) ?: "none"}")
                appendLine("Last prompt: ${lastPrompt?.format(fmt) ?: "none"}")
                append("Next check: ${next.format(fmt)}")
            }
            return CheckStatus(text)
        }

        private fun LocalTime.hm() = "%02d:%02d".format(hour, minute)
    }
}
