package com.example.dailyok.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dailyok.ui.theme.DailyOkTheme
import com.example.dailyok.ui.theme.Dimens
import com.example.dailyok.ui.theme.SmallText
import com.example.dailyok.ui.theme.StatusText

/** Green status card for the home screen: tick icon, "All is well", last active time. */
@Composable
fun StatusCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DailyOkTheme.status.allWellLight,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 26.dp, horizontal = Dimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(Dimens.StatusIconSize),
                shape = CircleShape,
                color = DailyOkTheme.status.allWell,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = DailyOkTheme.status.onAllWell,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = StatusText, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = SmallText, color = DailyOkTheme.status.secondaryText, textAlign = TextAlign.Center)
        }
    }
}
