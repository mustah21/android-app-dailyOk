package com.example.dailyok.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyok.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TextMain = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF4A4A4A)
private val Primary = Color(0xFF1F4E9C)
private val WellGreen = Color(0xFF1E6B3A)
private val WellGreenBg = Color(0xFFE7F3EC)
private val HelpRed = Color(0xFFB3261E)
private val WarningBg = Color(0xFFFFE8A3)

private val Corner = RoundedCornerShape(16.dp)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNeedHelp: () -> Unit,
    onMedsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    ) {
    val state by viewModel.state.collectAsState()

    HomeScreen(
        state = state,
        onOkClick = viewModel::onOkClicked,
        onNeedHelp = onNeedHelp,
        onPauseChanged = viewModel::onPauseChanged,
        onMedsClick = onMedsClick,
        onSettingsClick = onSettingsClick,
        )
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOkClick: () -> Unit,
    onNeedHelp: () -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    onMedsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,

    ) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Date
// Date + settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    fontSize = 18.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.Hello_Champ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )

            if (state.canPressOk) {
                Button(
                    onClick = onOkClick,
                    shape = Corner,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                ) {
                    Text(
                        text = stringResource(R.string.Daily_Ok),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.LET_FAMILY_KNOW),
                    fontSize = 18.sp,
                    color = TextSecondary,
                )
            } else {
                StatusBanner(
                    title = "All is well",
                    subtitle = state.lastActiveText,
                    icon = Icons.Filled.Check,
                    background = WellGreenBg,
                    contentColor = WellGreen,
                )
            }

            MedicationCard(
                medication = state.medication,
                onClick = onMedsClick,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onNeedHelp,
                shape = Corner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelpRed,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "  " + stringResource(R.string.NEED_HELP),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .toggleable(
                        value = state.isPaused,
                        role = Role.Switch,
                        onValueChange = onPauseChanged,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.PAUSE_FOR_HOLIDAYS),
                    fontSize = 20.sp,
                    color = TextMain,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = state.isPaused, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun StatusBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    background: Color,
    contentColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, Corner)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            color = TextMain,
        )
        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = TextSecondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MedicationCard(
    medication: MedicationReminder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeText = medication.time
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val taken = medication.status == MedStatus.TAKEN

    Card(
        onClick = onClick,
        shape = Corner,
        colors = CardDefaults.cardColors(
            containerColor = if (taken) WellGreenBg else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.heightIn(min = 72.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (taken) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = WellGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )
                Text(
                    text = if (taken) stringResource(R.string.medication_taken) else timeText,
                    fontSize = 15.sp,
                    color = TextSecondary,
                )
            }
        }
    }
}

