package com.example.dailyok.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dailyok.ui.theme.ButtonText
import com.example.dailyok.ui.theme.DailyOkTheme
import com.example.dailyok.ui.theme.Dimens

/** "I need help": full width, 96dp tall, red. Red is used for help and alarms only. */
@Composable
fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier, text: String = "I need help") {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(Dimens.HelpButtonHeight),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = DailyOkTheme.status.help,
            contentColor = DailyOkTheme.status.onHelp,
        ),
    ) {
        // The text label already says what the button does, so the icon is decorative.
        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = ButtonText.copy(fontSize = ButtonText.fontSize * 1.1f))
    }
}
