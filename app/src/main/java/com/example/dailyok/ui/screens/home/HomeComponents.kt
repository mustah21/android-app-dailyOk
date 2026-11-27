package com.example.dailyok.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyok.R
import com.example.dailyok.ui.theme.DailyOkTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val Corner = RoundedCornerShape(16.dp)

@Composable
fun HomeHeader(
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            fontSize = 18.sp,
            color = DailyOkTheme.status.secondaryText,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = DailyOkTheme.status.secondaryText,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun DailyCheckIn(
    canPressOk: Boolean,
    lastActiveText: String,
    onOkClick: () -> Unit
) {
    if (canPressOk) {
        Button(
            onClick = onOkClick,
            shape = Corner,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        ) {
            Text(
                text = stringResource(R.string.Daily_Ok),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(R.string.LET_FAMILY_KNOW),
            fontSize = 18.sp,
            color = DailyOkTheme.status.secondaryText
        )
    } else {
        StatusBanner(
            title = "All is well",
            subtitle = lastActiveText,
            icon = Icons.Filled.Check,
            background = DailyOkTheme.status.allWellLight,
            contentColor = DailyOkTheme.status.allWell
        )
    }
}

@Composable
fun StatusBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    background: Color,
    contentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, Corner)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(40.dp)
        )

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = DailyOkTheme.status.secondaryText
        )
    }
}

@Composable
fun NeedHelpButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = Corner,
        colors = ButtonDefaults.buttonColors(
            containerColor = DailyOkTheme.status.help,
            contentColor = DailyOkTheme.status.onHelp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )

        Text(
            text = stringResource(R.string.NEED_HELP),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PauseSwitch(
    isPaused: Boolean,
    onPauseChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(
                value = isPaused,
                role = Role.Switch,
                onValueChange = onPauseChanged
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.PAUSE_FOR_HOLIDAYS),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isPaused,
            onCheckedChange = null
        )
    }
}