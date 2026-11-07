package com.example.dailyok.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dailyok.R

@Composable
fun WelcomeRoute(
    viewModel: WelcomeViewModel,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    WelcomeScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onAgeChange = viewModel::onAgeChange,
        onContinue = { viewModel.onContinue(onFinished) },
    )
}

@Composable
fun WelcomeScreen(
    state: WelcomeUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val fieldText = MaterialTheme.typography.titleLarge

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_step),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(text = stringResource(R.string.welcome_name_label)) },
                textStyle = fieldText,
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text(text = stringResource(R.string.welcome_email_label)) },
                textStyle = fieldText,
                singleLine = true,
                isError = state.emailError != null,
                supportingText = state.emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.age,
                onValueChange = onAgeChange,
                label = { Text(text = stringResource(R.string.welcome_age_label)) },
                textStyle = fieldText,
                singleLine = true,
                isError = state.ageError != null,
                supportingText = state.ageError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onContinue() }),
                modifier = Modifier.fillMaxWidth(0.25f),
            )

            Text(
                text = stringResource(R.string.welcome_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onContinue,
                enabled = !state.saving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
            ) {
                Text(text = stringResource(R.string.welcome_continue),
                     style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen(
            state = WelcomeUiState(),
            onNameChange = {},
            onEmailChange = {},
            onAgeChange = {},
            onContinue = {},
        )
    }
}