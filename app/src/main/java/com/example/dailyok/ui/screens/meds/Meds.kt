package com.example.dailyok.ui.screens.meds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dailyok.domain.meds.Dose
import com.example.dailyok.domain.meds.DoseStatus
import com.example.dailyok.domain.meds.Medication
import com.example.dailyok.domain.meds.MedicationDay
import com.example.dailyok.ui.theme.DailyOkTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun MedsRoute(viewModel: MedsViewModel) {
    val state by viewModel.state.collectAsState()
    MedsScreen(
        state = state,
        onTaken = viewModel::onTaken,
        onUndoTaken = viewModel::onUndoTaken,
        onAdd = viewModel::onAdd,
        onEdit = viewModel::onEdit,
        onDelete = viewModel::onDelete,
        onDraftNameChange = viewModel::onDraftNameChange,
        onDraftDoseChange = viewModel::onDraftDoseChange,
        onDraftAddTime = viewModel::onDraftAddTime,
        onDraftRemoveTime = viewModel::onDraftRemoveTime,
        onSaveDraft = viewModel::onSaveDraft,
        onDismissDraft = viewModel::onDismissDraft,
    )
}

@Composable
fun MedsScreen(
    state: MedsUiState,
    onTaken: (Dose) -> Unit,
    onUndoTaken: (Dose) -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onDraftDoseChange: (String) -> Unit,
    onDraftAddTime: (LocalTime) -> Unit,
    onDraftRemoveTime: (LocalTime) -> Unit,
    onSaveDraft: () -> Unit,
    onDismissDraft: () -> Unit,
) {
    var deleting by remember { mutableStateOf<Medication?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Medicines", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })

            // Today
            SectionTitle("Today")
            if (state.today.isEmpty()) {
                Text("No medicines yet. Add your first one below.", style = MaterialTheme.typography.bodyLarge)
            }
            state.today.forEach { dose ->
                TodayDoseCard(dose, onTaken = { onTaken(dose) }, onUndo = { onUndoTaken(dose) })
            }

            // My medicines
            SectionTitle("My medicines")
            state.medications.forEach { med ->
                MedicationCard(med, onEdit = { onEdit(med.id) }, onDelete = { deleting = med })
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add medicine", modifier = Modifier.padding(start = 8.dp))
            }

            // History
            SectionTitle("History")
            if (state.history.isEmpty()) {
                Text("Your earlier days will show here.", style = MaterialTheme.typography.bodyLarge)
            }
            state.history.forEach { HistoryDayCard(it) }
        }
    }

    state.draft?.let { draft ->
        MedicationDialog(
            draft = draft,
            onNameChange = onDraftNameChange,
            onDoseChange = onDraftDoseChange,
            onAddTime = onDraftAddTime,
            onRemoveTime = onDraftRemoveTime,
            onSave = onSaveDraft,
            onDismiss = onDismissDraft,
        )
    }

    deleting?.let { med ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${med.name}?") },
            text = { Text("You will no longer get reminders for this medicine.") },
            confirmButton = {
                TextButton(onClick = { onDelete(med.id); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun TodayDoseCard(dose: Dose, onTaken: () -> Unit, onUndo: () -> Unit) {
    val status = DailyOkTheme.status
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (dose.status == DoseStatus.TAKEN) status.allWellLight
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dose.time.hm(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(dose.medication.name, style = MaterialTheme.typography.titleMedium)
                    if (dose.medication.dose.isNotBlank()) {
                        Text(dose.medication.dose, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Text(
                when (dose.status) {
                    DoseStatus.TAKEN -> "Taken at ${dose.takenAt?.toLocalTime()?.hm() ?: ""} ✓"
                    DoseStatus.DUE -> "Time to take it"
                    DoseStatus.UPCOMING -> "Later today"
                    DoseStatus.MISSED -> "Not taken yet – it was due at ${dose.time.hm()}"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = when (dose.status) {
                    DoseStatus.TAKEN -> status.allWell
                    DoseStatus.MISSED -> MaterialTheme.colorScheme.error
                    else -> status.secondaryText
                },
            )
            when (dose.status) {
                DoseStatus.TAKEN -> TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Undo – I didn't take it")
                }
                DoseStatus.DUE, DoseStatus.MISSED -> TookItButton(onTaken)
                DoseStatus.UPCOMING -> OutlinedButton(
                    onClick = onTaken,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text("I took it early") }
            }
        }
    }
}

/** Big green "I took it" button, also used on the home screen. */
@Composable
fun TookItButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DailyOkTheme.status.allWell,
            contentColor = DailyOkTheme.status.onAllWell,
        ),
    ) { Text("I took it", style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun MedicationCard(med: Medication, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(med.name, style = MaterialTheme.typography.titleMedium)
                if (med.dose.isNotBlank()) Text(med.dose, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Every day at " + med.times.sorted().joinToString(", ") { it.hm() },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit ${med.name}") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete ${med.name}") }
        }
    }
}

@Composable
private fun HistoryDayCard(day: MedicationDay) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(day.date.dayLabel(), style = MaterialTheme.typography.titleMedium)
            Text("${day.takenCount} of ${day.doses.size} taken", style = MaterialTheme.typography.bodyLarge)
            day.doses.forEach { dose ->
                val taken = dose.status == DoseStatus.TAKEN
                Text(
                    if (taken) "✓ ${dose.time.hm()}  ${dose.medication.name} – taken at ${dose.takenAt?.toLocalTime()?.hm() ?: ""}"
                    else "✗ ${dose.time.hm()}  ${dose.medication.name} – not taken",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (taken) DailyOkTheme.status.allWell else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationDialog(
    draft: MedicationDraft,
    onNameChange: (String) -> Unit,
    onDoseChange: (String) -> Unit,
    onAddTime: (LocalTime) -> Unit,
    onRemoveTime: (LocalTime) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pickingTime by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "Add medicine" else "Edit medicine") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Name of the medicine") },
                    singleLine = true,
                    isError = draft.nameError,
                    supportingText = if (draft.nameError) {
                        { Text("Enter a name") }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = draft.dose,
                    onValueChange = onDoseChange,
                    label = { Text("How much (optional)") },
                    placeholder = { Text("e.g. 1 tablet") },
                    singleLine = true,
                )
                Text("Times every day", style = MaterialTheme.typography.titleMedium)
                draft.times.forEach { time ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(time.hm(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveTime(time) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove ${time.hm()}")
                        }
                    }
                }
                if (draft.timesError) {
                    Text("Add at least one time", color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = { pickingTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add a time", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickingTime) {
        val picker = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Time to take it") },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    onAddTime(LocalTime.of(picker.hour, picker.minute))
                    pickingTime = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
}

fun LocalTime.hm(): String = "%02d:%02d".format(hour, minute)

private fun LocalDate.dayLabel(): String = when (this) {
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> format(DateTimeFormatter.ofPattern("EEEE d.M."))
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun MedsScreenPreview() {
    val today = LocalDate.now()
    val bp = Medication("bp", "Blood pressure", "1 tablet", listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
    val d = Medication("d", "Vitamin D", times = listOf(LocalTime.of(12, 0)))
    DailyOkTheme {
        MedsScreen(
            state = MedsUiState(
                today = listOf(
                    Dose(bp, today, LocalTime.of(8, 0), DoseStatus.TAKEN, LocalDateTime.of(today, LocalTime.of(8, 10))),
                    Dose(d, today, LocalTime.of(12, 0), DoseStatus.DUE),
                    Dose(bp, today, LocalTime.of(20, 0), DoseStatus.UPCOMING),
                ),
                medications = listOf(bp, d),
                history = listOf(
                    MedicationDay(
                        today.minusDays(1),
                        listOf(
                            Dose(bp, today.minusDays(1), LocalTime.of(8, 0), DoseStatus.TAKEN, LocalDateTime.of(today.minusDays(1), LocalTime.of(8, 5))),
                            Dose(d, today.minusDays(1), LocalTime.of(12, 0), DoseStatus.MISSED),
                        ),
                    ),
                ),
            ),
            onTaken = {}, onUndoTaken = {}, onAdd = {}, onEdit = {}, onDelete = {},
            onDraftNameChange = {}, onDraftDoseChange = {}, onDraftAddTime = {}, onDraftRemoveTime = {},
            onSaveDraft = {}, onDismissDraft = {},
        )
    }
}
