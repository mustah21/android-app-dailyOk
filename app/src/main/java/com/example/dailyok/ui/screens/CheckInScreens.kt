package com.example.dailyok.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dailyok.ui.checkin.AlarmEffects
import com.example.dailyok.ui.theme.BodyText
import com.example.dailyok.ui.common.DailyOkButton
import com.example.dailyok.ui.theme.DailyOkTheme
import com.example.dailyok.ui.theme.Dimens
import com.example.dailyok.ui.theme.HeadingText
import com.example.dailyok.ui.theme.StatusText

/**
 * Step 1: the gentle "Are you OK?" prompt.
 * Full screen, with an "I'm OK" button that takes at least half the screen.
 */
@Composable
fun CheckInPromptScreen(
    greeting: String,                 // e.g. "Good morning!"
    onImOk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Back must not dismiss the prompt by accident; the person answers with the big button.
    BackHandler { }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Dimens.ScreenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "$greeting\nAre you OK?",
            style = StatusText,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Pick up your phone or tap the button.",
            style = BodyText,
            color = DailyOkTheme.status.secondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        ImOkButton(
            onClick = onImOk,
            modifier = Modifier.weight(1f),   // fills the rest of the screen (at least half of it)
        )
    }
}

/**
 * Step 2: the alarm, shown if the prompt was not answered.
 * Amber background, a countdown to when the family is contacted, and a large "I'm OK" button.
 * Sound, vibration and full brightness come from [com.example.dailyok.ui.checkin.AlarmEffects].
 */
@Composable
fun AlarmScreen(
    contactName: String,              // e.g. "Anna"
    secondsLeft: Int,
    onImOk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { }
    AlarmEffects()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DailyOkTheme.status.warning)
            .padding(Dimens.ScreenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Are you OK?",
            style = StatusText,
            color = DailyOkTheme.status.onWarning,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "We will send a message to $contactName in",
            style = BodyText,
            color = DailyOkTheme.status.onWarning,
            textAlign = TextAlign.Center,
        )
        Text(
            text = formatCountdown(secondsLeft),
            style = StatusText,
            color = DailyOkTheme.status.onWarning,
            textAlign = TextAlign.Center,
            // TalkBack announces the countdown as it changes, without stealing focus.
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(32.dp))
        ImOkButton(onClick = onImOk, modifier = Modifier.weight(1f))
    }
}

/**
 * Step 3: shown after the family has been contacted (or "I need help" was pressed).
 * Calm, with a large Cancel button in case it was a mistake.
 */
@Composable
fun HelpSentScreen(
    contactNames: List<String>,       // e.g. listOf("Anna", "Jussi")
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Dimens.ScreenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceBetweenTargets),
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
            style = StatusText,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "We have sent a message to ${contactNames.joinToString(" and ")}.",
            style = BodyText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Pressed by mistake? Tap Cancel and we will tell them you are OK.",
            style = BodyText,
            color = DailyOkTheme.status.secondaryText,
            textAlign = TextAlign.Center,
        )
        DailyOkButton(text = "Cancel", onClick = onCancel)
    }
}

/** The very large green "I'm OK" button used on the prompt and alarm screens. */
@Composable
private fun ImOkButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = DailyOkTheme.status.allWell,
            contentColor = DailyOkTheme.status.onAllWell,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(12.dp))
            Text("I'm OK", style = HeadingText)
        }
    }
}

private fun formatCountdown(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ---------- Previews ----------

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PromptPreview() {
    DailyOkTheme { CheckInPromptScreen(greeting = "Good morning!", onImOk = {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun AlarmPreview() {
    DailyOkTheme { AlarmScreen(contactName = "Anna", secondsLeft = 14 * 60 + 32, onImOk = {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun HelpSentPreview() {
    DailyOkTheme { HelpSentScreen(contactNames = listOf("Anna", "Jussi"), onCancel = {}) }
}
