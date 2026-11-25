package com.example.dailyok.ui.screens.home

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.dailyok.R
import com.example.dailyok.domain.meds.Dose

private val TextMain = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF4A4A4A)
private val Primary = Color(0xFF1F4E9C)
private val HelpRed = Color(0xFFB3261E)
private val WellGreen = Color(0xFF2E7D32)
private val Corner = RoundedCornerShape(16.dp)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNeedHelp: () -> Unit,
    onMedsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    HomeScreen(
        state = state,
        onOkClick = viewModel::onOkClicked,
        onNeedHelp = onNeedHelp,
        onPauseChanged = viewModel::onPauseChanged,
        onMedsClick = onMedsClick,
        onMedTaken = viewModel::onMedTaken,
        onSettingsClick = onSettingsClick,
        onGrantUsageAccess = {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
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
    onGrantUsageAccess: () -> Unit,
    modifier: Modifier = Modifier,
    onMedTaken: (Dose) -> Unit = {}
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

            if (state.canPressOk) {
                Button(
                    onClick = onOkClick,
                    shape = Corner,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellGreen,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 192.dp),
                ) {
                    Text(
                        text = stringResource(R.string.Daily_Ok),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            MedicationPreview(
                doses = state.upcomingMeds,
                onTaken = onMedTaken,
                onOpen = onMedsClick
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
