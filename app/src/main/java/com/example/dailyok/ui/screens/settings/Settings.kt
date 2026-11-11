package com.example.dailyok.ui.screens.settings



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()
    SettingsScreen(
        state = state,
        onInactivityHoursChange = viewModel::onInactivityHoursChange,
        onAddContact = viewModel::onAddContact,
        onEditContact = viewModel::onEditContact,
        onDeleteContact = viewModel::onDeleteContact,
        onDraftNameChange = viewModel::onDraftNameChange,
        onDraftPhoneChange = viewModel::onDraftPhoneChange,
        onSaveContact = viewModel::onSaveContact,
        onDismissDraft = viewModel::onDismissDraft,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onInactivityHoursChange: (Int) -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onDraftPhoneChange: (String) -> Unit,
    onSaveContact: () -> Unit,
    onDismissDraft: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)

            // Inactivity period
            Text("Daytime inactivity period", style = MaterialTheme.typography.titleLarge)
            Text(
                "Alert my contacts after ${state.inactivityHours} hours without activity",
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = state.inactivityHours.toFloat(),
                onValueChange = { onInactivityHoursChange(it.roundToInt()) },
                valueRange = SettingsViewModel.MIN_HOURS.toFloat()..SettingsViewModel.MAX_HOURS.toFloat(),
                steps = SettingsViewModel.MAX_HOURS - SettingsViewModel.MIN_HOURS - 1,  // 3 -> 2,3,4,5,6
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${SettingsViewModel.MIN_HOURS} h", style = MaterialTheme.typography.bodyLarge)
                Text("${SettingsViewModel.MAX_HOURS} h", style = MaterialTheme.typography.bodyLarge)
            }

            // Contacts
            Text("Contacts", style = MaterialTheme.typography.titleLarge)
            state.contacts.forEach { contact ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.titleMedium)
                            Text(contact.phone, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { onEditContact(contact.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${contact.name}")
                        }
                        IconButton(onClick = { onDeleteContact(contact.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${contact.name}")
                        }
                    }
                }
            }
            Button(onClick = onAddContact, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add contact", modifier = Modifier.padding(start = 8.dp))
            }

            // Activity log
            ActivityLogSection(state.activity)
        }

        state.draft?.let { draft ->
            AlertDialog(
                onDismissRequest = onDismissDraft,
                title = { Text(if (draft.id == null) "Add contact" else "Edit contact") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = onDraftNameChange,
                            label = { Text("Name") },
                            singleLine = true,
                            isError = draft.nameError,
                            supportingText = if (draft.nameError) {
                                { Text("Enter a name") }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                            ),
                        )
                        OutlinedTextField(
                            value = draft.phone,
                            onValueChange = onDraftPhoneChange,
                            label = { Text("Phone number") },
                            singleLine = true,
                            isError = draft.phoneError,
                            supportingText = if (draft.phoneError) {
                                { Text("Enter a valid phone number") }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )
                    }
                },
                confirmButton = { TextButton(onClick = onSaveContact) { Text("Save") } },
                dismissButton = { TextButton(onClick = onDismissDraft) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun ActivityLogSection(days: List<ActivityDay>) {
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    Text("Activity log", style = MaterialTheme.typography.titleLarge)
    days.forEach { day ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(day.date.format(dateFormat), style = MaterialTheme.typography.titleMedium)
                Text(day.summary, style = MaterialTheme.typography.bodyLarge)
                day.alerts.forEach { alert ->
                    Text(
                        text = "${alert.time.format(timeFormat)}  ${alert.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (alert.urgent) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}



@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SettingsScreenPreview() {
    val today = java.time.LocalDate.now()
    MaterialTheme {
        SettingsScreen(
            state = SettingsUiState(
                contacts = listOf(
                    Contact(id = "1", name = "Anna (daughter)", phone = "+358 40 123 4567"),
                    Contact(id = "2", name = "Matti (neighbour)", phone = "+358 50 765 4321"),
                ),
                inactivityHours = 4,
                activity = listOf(
                    ActivityDay(today, "Normal activity", emptyList()),
                    ActivityDay(
                        today.minusDays(1), "1 alert",
                        listOf(
                            ActivityAlert(LocalTime.of(14, 20), "No activity for 4 hours", urgent = true),
                            ActivityAlert(LocalTime.of(14, 35), "Anna confirmed you are OK", urgent = false),
                        ),
                    ),
                ),
            ),
            onInactivityHoursChange = {},
            onAddContact = {},
            onEditContact = {},
            onDeleteContact = {},
            onDraftNameChange = {},
            onDraftPhoneChange = {},
            onSaveContact = {},
            onDismissDraft = {},
        )
    }
}