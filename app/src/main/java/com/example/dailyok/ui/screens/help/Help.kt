package com.example.dailyok.ui.screens.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailyok.R
import com.example.dailyok.ui.screens.home.HomeScreen

@Composable
fun HelpRoute(
    viewModel: HelpViewModel,
) {
    HelpScreen(
        viewModel = viewModel
    )
}


@Composable
fun HelpScreen(
    viewModel: HelpViewModel = viewModel()
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🚁", fontSize = 100.sp)

        Text(
            text = "Help is on its way",
            fontSize = 30.sp,
            fontWeight = Bold
        )
        Text (text = stringResource(R.string.family_notified))
    }

}