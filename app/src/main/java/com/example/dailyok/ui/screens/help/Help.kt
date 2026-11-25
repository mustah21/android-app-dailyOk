package com.example.dailyok.ui.screens.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HelpRed = Color(0xFFB3261E)

@Composable
fun HelpRoute(
    viewModel: HelpViewModel,
    onCancel: () -> Unit = {},
) {
    HelpScreen(
        viewModel = viewModel,
        onCancel = onCancel,
    )
}

@Composable
fun HelpScreen(
    viewModel: HelpViewModel,
    onCancel: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    if (state.helpSent) {
        HelpSentContent(
            onCancel = {
                viewModel.cancelHelp()
                onCancel()
            },
        )
    } else {
        HelpRequestContent(
            sending = state.sending,
            onRequestHelp = viewModel::requestHelp,
        )
    }
}

@Composable
private fun HelpRequestContent(
    sending: Boolean,
    onRequestHelp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = HelpRed,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "I need help",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tap the button below to alert both of your emergency contacts right now.",
            fontSize = 18.sp,
            color = Color(0xFF4A4A4A),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onRequestHelp,
            enabled = !sending,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = HelpRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        ) {
            if (sending) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Text(
                    text = "Send help alert",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun HelpSentContent(
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = "Help is on the way",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "We have sent an alert to your emergency contacts.",
            fontSize = 18.sp,
            color = Color(0xFF4A4A4A),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Pressed by mistake? Tap Cancel and we will tell them you are OK.",
            fontSize = 16.sp,
            color = Color(0xFF4A4A4A),
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onCancel,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(
                text = "Cancel – All OK",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
