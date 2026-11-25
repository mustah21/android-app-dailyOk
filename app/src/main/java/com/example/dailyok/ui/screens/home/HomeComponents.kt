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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwitchDefaults
import com.example.dailyok.ui.theme.ForestGreen
import com.example.dailyok.ui.theme.HelpRed
import com.example.dailyok.ui.theme.MainText
import com.example.dailyok.ui.theme.MintBg
import com.example.dailyok.ui.theme.SecondaryText
import com.example.dailyok.ui.theme.SubtleBorder

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
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM")),
            fontSize = 18.sp,
            color = SecondaryText,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = SecondaryText,
                modifier = Modifier.size(28.dp)
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
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer mint circle
        Box(
            modifier = Modifier
                .size(210.dp)
                .background(MintBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner forest green circle
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .background(ForestGreen, CircleShape)
                    .then(if (canPressOk) Modifier.clickable(onClick = onOkClick) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "I'm OK",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
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
            .background(background, RoundedCornerShape(20.dp))
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
            color = MainText
        )

        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = SecondaryText
        )
    }
}

@Composable
fun NeedHelpButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, HelpRed),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = HelpRed
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = HelpRed,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = stringResource(R.string.NEED_HELP),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HelpRed
            )
        }
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
            .heightIn(min = 56.dp)
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
            fontSize = 18.sp,
            color = MainText,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isPaused,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ForestGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SubtleBorder,
                uncheckedBorderColor = Color.Transparent,
            )
        )
    }
}