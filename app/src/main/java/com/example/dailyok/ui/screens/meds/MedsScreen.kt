package com.example.dailyok.ui.screens.meds

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TextMain      = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF4A4A4A)
private val WellGreen     = Color(0xFF1E6B3A)
private val WellGreenBg   = Color(0xFFE7F3EC)
private val Surface       = Color(0xFFF5F5F2)
private val Corner        = RoundedCornerShape(16.dp)

data class MedLogEntry(
    val name: String,
    val dose: String,
    val scheduledTime: LocalTime,
    val takenDate: LocalDate?,   // null = not yet taken today
)

val pseudoMeds = listOf(
    MedLogEntry("Blood Pressure Pill",  "1 × 10 mg",  LocalTime.of(8, 0),  LocalDate.now()),
    MedLogEntry("Vitamin D",            "1 × 1000 IU", LocalTime.of(8, 0),  LocalDate.now()),
    MedLogEntry("Evening Heart Tablet", "1 × 5 mg",   LocalTime.of(20, 0), null),
    MedLogEntry("Aspirin",              "1 × 100 mg", LocalTime.of(12, 0), null),
)


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedsScreen(onBack: () -> Unit) {
    var entries by remember { mutableStateOf(pseudoMeds) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Medication Log",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextMain,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            val takenCount = entries.count { it.takenDate == LocalDate.now() }
            val totalCount = entries.size
            val allDone    = takenCount == totalCount

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (allDone) WellGreenBg else Color(0xFFFFF3E0), Corner)
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (allDone) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = WellGreen,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Column {
                        Text(
                            text = if (allDone) "All done for today! 🎉" else "Today's progress",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allDone) WellGreen else TextMain,
                        )
                        Text(
                            text = "$takenCount of $totalCount medications taken",
                            fontSize = 15.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }

            entries.forEachIndexed { index, med ->
                MedCard(
                    med = med,
                    onMarkTaken = {
                        entries = entries.toMutableList().also { list ->
                            list[index] = med.copy(takenDate = LocalDate.now())
                        }
                    },
                )
            }

            Spacer(Modifier.height(24.dp))


            Text(
                text = "Recent history",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )

            listOf(
                "Yesterday" to "Blood Pressure Pill, Vitamin D, Evening Heart Tablet",
                "2 days ago" to "Blood Pressure Pill, Vitamin D",
                "3 days ago" to "All 4 medications taken ✓",
            ).forEach { (day, detail) ->
                Card(
                    shape = Corner,
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = day,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                        )
                        Text(
                            text = detail,
                            fontSize = 14.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedCard(med: MedLogEntry, onMarkTaken: () -> Unit) {
    val takenToday = med.takenDate == LocalDate.now()
    val timeText   = med.scheduledTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    Card(
        shape = Corner,
        colors = CardDefaults.cardColors(
            containerColor = if (takenToday) WellGreenBg else Color.White,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Status icon
            if (takenToday) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Taken",
                    tint = WellGreen,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFFDDDDD8), RoundedCornerShape(50)),
                )
            }

            // Name + dose + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = med.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )
                Text(
                    text = "${med.dose} · $timeText",
                    fontSize = 14.sp,
                    color = TextSecondary,
                )
                if (takenToday) {
                    Text(
                        text = "Taken today ✓",
                        fontSize = 13.sp,
                        color = WellGreen,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Mark taken button
            if (!takenToday) {
                Button(
                    onClick = onMarkTaken,
                    colors = ButtonDefaults.buttonColors(containerColor = WellGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(
                        text = "Mark taken",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
