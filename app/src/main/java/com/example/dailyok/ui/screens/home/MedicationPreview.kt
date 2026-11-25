package com.example.dailyok.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dailyok.R
import androidx.compose.ui.res.stringResource
import com.example.dailyok.domain.meds.Dose
import com.example.dailyok.domain.meds.DoseStatus
import com.example.dailyok.domain.meds.Medication
import com.example.dailyok.ui.screens.meds.TookItButton
import com.example.dailyok.ui.screens.meds.hm
import com.example.dailyok.ui.theme.DailyOkTheme
import java.time.LocalDate
import java.time.LocalTime

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import com.example.dailyok.ui.theme.ForestGreen
import com.example.dailyok.ui.theme.SecondaryText

/**
 * Home screen: the next medicines to take. Doses that are due have an "I took it" button.
 * Tapping the card opens the Medicines page.
 */
@Composable
fun MedicationPreview(
    doses: List<Dose>,
    onTaken: (Dose) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.medication_label),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onOpen,
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text(
                        "See all",
                        color = ForestGreen,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            if (doses.isEmpty()) {
                Text(
                    "No medicines added yet. Add your first one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecondaryText
                )
            }
            doses.forEachIndexed { i, dose ->
                if (i > 0) HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dose.time.hm(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(dose.medication.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                dose.status == DoseStatus.DUE -> "Time to take it"
                                dose.date != LocalDate.now() -> "Tomorrow"
                                else -> listOf("Later today", dose.medication.dose).filter { it.isNotBlank() }.joinToString(" · ")
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (dose.status == DoseStatus.DUE) DailyOkTheme.status.allWell
                            else DailyOkTheme.status.secondaryText,
                        )
                    }
                }
                if (dose.status == DoseStatus.DUE) TookItButton(onClick = { onTaken(dose) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationPreviewPreview() {
    val today = LocalDate.now()
    val bp = Medication("bp", "Blood pressure", "1 tablet", listOf(LocalTime.of(8, 0)))
    val d = Medication("d", "Vitamin D", times = listOf(LocalTime.of(12, 0)))
    DailyOkTheme {
        MedicationPreview(
            doses = listOf(
                Dose(bp, today, LocalTime.of(8, 0), DoseStatus.DUE),
                Dose(d, today, LocalTime.of(12, 0), DoseStatus.UPCOMING),
            ),
            onTaken = {},
            onOpen = {},
        )
    }
}
