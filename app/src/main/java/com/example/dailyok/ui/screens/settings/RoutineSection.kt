package com.example.dailyok.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

/**
 * Settings > My day: the person enters when they usually wake up and go to bed.
 * No checks run during sleep hours; the morning check is 3 hours after waking up.
 */
@Composable
fun RoutineSection(
    wakeTime: LocalTime,
    bedTime: LocalTime,
    morningCheckTime: LocalTime,
    error: String?,
    onWakeTimeChange: (LocalTime) -> Unit,
    onBedTimeChange: (LocalTime) -> Unit,
) {
    // Which time is being edited: "wake", "bed" or null.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My day", style = MaterialTheme.typography.titleLarge)
        TimeRow("I usually wake up at", wakeTime) { editing = "wake" }
        TimeRow("I usually go to bed at", bedTime) { editing = "bed" }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        }
        Text(
            "We don't check on you while you sleep. If there is no sign of you by " +
                "${morningCheckTime.hm()}, we ask if you are OK.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    when (editing) {
        "wake" -> TimeDialog("Waking up time", wakeTime, onDismiss = { editing = null }) {
            onWakeTimeChange(it); editing = null
        }
        "bed" -> TimeDialog("Bed time", bedTime, onDismiss = { editing = null }) {
            onBedTimeChange(it); editing = null
        }
    }
}

@Composable
private fun TimeRow(label: String, time: LocalTime, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(time.hm(), style = MaterialTheme.typography.headlineMedium)
        }
        OutlinedButton(onClick = onChange, modifier = Modifier.heightIn(min = 56.dp)) {
            Text("Change")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun LocalTime.hm(): String = "%02d:%02d".format(hour, minute)
