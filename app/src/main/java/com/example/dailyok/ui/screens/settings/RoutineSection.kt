package com.example.dailyok.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyok.ui.theme.ForestGreen
import com.example.dailyok.ui.theme.MainText
import com.example.dailyok.ui.theme.SecondaryText
import java.time.LocalTime

@Composable
fun RoutineSection(
    wakeTime: LocalTime,
    bedTime: LocalTime,
    morningCheckTime: LocalTime,
    error: String?,
    onWakeTimeChange: (LocalTime) -> Unit,
    onBedTimeChange: (LocalTime) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("My day", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MainText)
            TimeRow("I usually wake up at", wakeTime) { editing = "wake" }
            TimeRow("I usually go to bed at", bedTime) { editing = "bed" }
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            }
            Text(
                "We don't check on you while you sleep.",
                fontSize = 15.sp,
                color = SecondaryText,
            )
        }
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
            Text(label, fontSize = 16.sp, color = SecondaryText)
            Text(time.hm(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MainText)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, ForestGreen), RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onChange() }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Change", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 15.sp)
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
