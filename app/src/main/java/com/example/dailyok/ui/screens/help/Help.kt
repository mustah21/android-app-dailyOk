package com.example.dailyok.ui.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dailyok.domain.help.HelpContact
import com.example.dailyok.platform.help.HelpRequest
import com.example.dailyok.ui.common.DailyOkButton
import com.example.dailyok.ui.theme.DailyOkTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HelpRoute(viewModel: HelpViewModel, onClose: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    // "I need help" was pressed: reach out to the contacts right away.
    LaunchedEffect(Unit) { viewModel.onOpen() }

    HelpScreen(
        request = state.request,
        onCancel = {
            viewModel.onCancelRequest()
            onClose()
        },
    )
}

/** Shows who was asked for help, and one button to cancel. */
@Composable
fun HelpScreen(request: HelpRequest?, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            "Help is requested",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        if (request != null) {
            val time = Instant.ofEpochMilli(request.requestedAtMs).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            Text(
                if (request.contacts.isEmpty()) {
                    "You have no contacts saved. Add family or neighbours in Settings."
                } else {
                    "At $time we asked these people to help you. They will contact you."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            request.contacts.forEach { ContactCard(it) }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Pressed by mistake? Tap Cancel and we will tell them you are OK.",
            style = MaterialTheme.typography.bodyLarge,
            color = DailyOkTheme.status.secondaryText,
            textAlign = TextAlign.Center,
        )
        DailyOkButton(text = "Cancel", onClick = onCancel)
    }
}

@Composable
private fun ContactCard(contact: HelpContact) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(contact.name, style = MaterialTheme.typography.titleMedium)
            Text(contact.phone, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun HelpScreenPreview() {
    DailyOkTheme {
        HelpScreen(
            request = HelpRequest(
                System.currentTimeMillis(),
                listOf(
                    HelpContact("1", "Anna (daughter)", "+358 40 123 4567"),
                    HelpContact("2", "Matti (neighbour)", "+358 50 765 4321"),
                ),
            ),
            onCancel = {},
        )
    }
}
