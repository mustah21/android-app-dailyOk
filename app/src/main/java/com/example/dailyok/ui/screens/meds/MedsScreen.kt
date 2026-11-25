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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import com.example.dailyok.ui.theme.ForestGreen
import com.example.dailyok.ui.theme.MainText
import com.example.dailyok.ui.theme.MintBgTaken
import com.example.dailyok.ui.theme.SecondaryText

private val Corner = RoundedCornerShape(20.dp)

data class MedLogEntry(
    val name: String,
    val dose: String,
    val scheduledTime: LocalTime,
    val takenDate: LocalDate?,   // null = not yet taken today
)

val pseudoMeds = listOf(
    MedLogEntry("Blood Pressure Pill",  "1 × 10 mg",  LocalTime.of(8, 0),  LocalDate.now()),
    MedLogEntry("Vitamin D",            "1 × 1000 IU", LocalTime.of(8, 0),  LocalDate.now()),
    MedLogEntry("Aspirin",              "1 × 100 mg", LocalTime.of(12, 0), null),
    MedLogEntry("Evening Heart Tablet", "1 × 5 mg",   LocalTime.of(20, 0), null),
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedsScreen(onBack: () -> Unit) {
    var entries by remember { mutableStateOf(pseudoMeds) }

    Scaffold(
        containerColor = Color(0xFFF8F5EE),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F5EE)),
                title = {
                    Text(
                        text = "Medication log",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MainText,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MainText,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val takenCount = entries.count { it.takenDate == LocalDate.now() }
            val totalCount = entries.size
            val progressFraction = if (totalCount > 0) takenCount.toFloat() / totalCount.toFloat() else 0f

            // Today's progress card
            Card(
                shape = Corner,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's progress",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MainText,
                        )
                        Text(
                            text = "$takenCount of $totalCount",
                            fontSize = 16.sp,
                            color = SecondaryText,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = ForestGreen,
                        trackColor = Color(0xFFEFECE6),
                    )
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

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Recent history",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MainText,
            )

            listOf(
                "Yesterday" to "Blood Pressure Pill, Vitamin D, Evening Heart Tablet",
                "2 days ago" to "Blood Pressure Pill, Vitamin D",
                "3 days ago" to "All 4 medications taken ✓",
            ).forEach { (day, detail) ->
                Card(
                    shape = Corner,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = day,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MainText,
                        )
                        Text(
                            text = detail,
                            fontSize = 15.sp,
                            color = SecondaryText,
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
    val timeText   = med.scheduledTime.format(DateTimeFormatter.ofPattern("H:mm"))

    Card(
        shape = Corner,
        colors = CardDefaults.cardColors(
            containerColor = if (takenToday) MintBgTaken else Color.White,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Status icon
            if (takenToday) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ForestGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Taken",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(BorderStroke(2.dp, Color(0xFFC2C8C4)), CircleShape),
                )
            }

            // Name + dose + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = med.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MainText,
                )
                Text(
                    text = if (takenToday) "${med.dose} · $timeText · taken" else "${med.dose} · $timeText",
                    fontSize = 15.sp,
                    color = SecondaryText,
                )
            }

            // Mark taken button
            if (!takenToday) {
                Button(
                    onClick = onMarkTaken,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 40.dp),
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
