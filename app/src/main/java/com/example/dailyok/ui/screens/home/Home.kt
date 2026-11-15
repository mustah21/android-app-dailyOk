package com.example.dailyok.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyok.R
import java.time.LocalDate

private val TextMain = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF4A4A4A)
private val Primary = Color(0xFF1F4E9C)
private val HelpRed = Color(0xFFB3261E)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNeedHelp: () -> Unit,
    onMedsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    HomeScreen(
        state = state,
        onOkClick = viewModel::onOkClicked,
        onNeedHelp = onNeedHelp,
        onPauseChanged = viewModel::onPauseChanged,
        onMedsClick = onMedsClick,
        onSettingsClick = onSettingsClick
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
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            HomeHeader(
                onSettingsClick = onSettingsClick
            )

            Text(
                text = stringResource(R.string.Hello_Champ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            DailyCheckIn(
                canPressOk = state.canPressOk,
                lastActiveText = state.lastActiveText,
                onOkClick = onOkClick
            )

            MedicationCard(
                medication = state.medication,
                onClick = onMedsClick
            )

            NeedHelpButton(
                onClick = onNeedHelp
            )

            PauseSwitch(
                isPaused = state.isPaused,
                onPauseChanged = onPauseChanged
            )
        }
    }
}