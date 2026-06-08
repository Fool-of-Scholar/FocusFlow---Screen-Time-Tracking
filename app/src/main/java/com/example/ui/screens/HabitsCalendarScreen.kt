package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsCalendarScreen(viewModel: FocusViewModel) {
    val timelineEntries by viewModel.timelineEntries.collectAsState()

    var showPulseDialog by remember { mutableStateOf(false) }

    // Pulse record form states
    var starsRating by remember { mutableIntStateOf(4) }
    var journalNotesText by remember { mutableStateOf("") }
    var moodTagsField by remember { mutableStateOf("focused, calm") }
    var calendarDateField by remember { mutableStateOf("June 4") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP BANNER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ATTENTION DIARY 📓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Diary",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = { showPulseDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_pulse_star_log_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add rating")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Today", fontWeight = FontWeight.Bold)
                }
            }
        }

        // DAILY COMPASS ADVISORY SUMMARY CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "MENTAL HEALING SCORECARD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Logging your mental tension pulse assists Coach Master Kitty to adjust limits dynamically to protect raw cognitive energy.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_stretching_view),
                        contentDescription = "Cat Mascot Stretching",
                        modifier = Modifier
                            .size(75.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }

        // HEADING
        item {
            Text(
                "My Digital Reflections Journal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (timelineEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                            contentDescription = "Cat Mascot Head",
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No diary logs found.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(timelineEntries) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_entry_card_${entry.id}"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📆 ${entry.dateString}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Stars visual bar row
                            Row {
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (i <= entry.pulseScore) Color(0xFFFFB300) else Color.DarkGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (entry.feelingTags.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                entry.feelingTags.split(",").forEach { rawTag ->
                                    val tag = rawTag.trim()
                                    if (tag.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (entry.journalText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "\"${entry.journalText}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        /* Hide Coach Evaluation for now (AI feature planned for future release)
                        if (entry.coachFeedback.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val mentorExpr = if (entry.pulseScore >= 4) "happy" else "sad"
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                                        contentDescription = "Cat Mascot Head",
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "COACH EVALUATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = entry.coachFeedback,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        */
                    }
                }
            }
        }
    }

    // REFLECTION PULSE POPUP SHEET DIALOG
    if (showPulseDialog) {
        AlertDialog(
            onDismissRequest = { showPulseDialog = false },
            title = { Text("🧘 Log Mental Pulse Star", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Rate your digital balance focus today. 5 stars means total Zen focus, 1 star means scrolling trap loops.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = calendarDateField,
                        onValueChange = { calendarDateField = it },
                        modifier = Modifier.fillMaxWidth().testTag("pulse_date_field"),
                        label = { Text("Date (e.g. June 4)") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rating Score: $starsRating / 5 Stars", fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star $i",
                                tint = if (i <= starsRating) Color(0xFFFFB300) else Color.DarkGray,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { starsRating = i }
                                    .testTag("pulse_star_icon_$i")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = journalNotesText,
                        onValueChange = { journalNotesText = it },
                        modifier = Modifier.fillMaxWidth().testTag("pulse_journal_notes"),
                        label = { Text("Journal Notes (How are you feeling?)") },
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = moodTagsField,
                        onValueChange = { moodTagsField = it },
                        modifier = Modifier.fillMaxWidth().testTag("pulse_mood_tags"),
                        label = { Text("Mood feeling tags (comma separated)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("pulse_confirm_button"),
                    onClick = {
                        viewModel.addJournalEntry(
                            stars = starsRating,
                            journalText = journalNotesText,
                            feelingTags = moodTagsField,
                            dateString = calendarDateField
                        )
                        showPulseDialog = false
                    }
                ) {
                    Text("Record Pulse", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPulseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
