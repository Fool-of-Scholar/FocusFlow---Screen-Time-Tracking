package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLockSchedule
import com.example.ui.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSchedulesScreen(viewModel: FocusViewModel) {
    val schedules by viewModel.schedules.collectAsState()
    val userPreservedDecks by viewModel.userPreservedDecks.collectAsState()

    var showAddScheduleForm by remember { mutableStateOf(false) }

    // Forms fields
    var appNameField by remember { mutableStateOf("Deep Work Focus") }
    var startTimeField by remember { mutableStateOf("22:00") }
    var endTimeField by remember { mutableStateOf("23:30") }
    var daysField by remember { mutableStateOf("Daily") }
    var todoField by remember { mutableStateOf("Read an offline paper handbook, stretch.") }
    var smsAlertField by remember { mutableStateOf("FocusFlow curfews unlocked! Rest well offline.") }
    var cooldownField by remember { mutableStateOf("0") }
    var thresholdField by remember { mutableStateOf("0") }

    val staticPresetDecks = remember {
        listOf(
            AppLockSchedule(
                appName = "Study Block",
                startTime = "09:00",
                endTime = "12:00",
                daysOfWeek = "Weekdays",
                todoWhileLocked = "Intense deep study and focus workbook routines.",
                customAlertSms = "Study Block active. Keep phone locked!",
                cooldownMinutes = 0,
                usageThresholdMinutes = 0
            ),
            AppLockSchedule(
                appName = "Bedtime Sleep",
                startTime = "22:30",
                endTime = "06:00",
                daysOfWeek = "Daily",
                todoWhileLocked = "Nocturnal digital detox. Rest eyes.",
                customAlertSms = "Bedtime curfew active! Sleep well.",
                cooldownMinutes = 0,
                usageThresholdMinutes = 0
            ),
            AppLockSchedule(
                appName = "Workout Drift",
                startTime = "17:00",
                endTime = "19:00",
                daysOfWeek = "Mon, Wed, Fri",
                todoWhileLocked = "Screen-free physical athletic conditioning.",
                customAlertSms = "Workout block active! Stay focused.",
                cooldownMinutes = 0,
                usageThresholdMinutes = 0
            )
        )
    }

    val unifiedDecks = remember(userPreservedDecks) {
        userPreservedDecks + staticPresetDecks
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP BARS BANNERS
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
                        text = "LOCKOUT REGISTRY 🔒",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Curfew Schedules",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = { showAddScheduleForm = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_custom_schedule_fab"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add locked timer bundle")
                        Text("New Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // MASCOT HEADER BANNER CARD (Net Worth Inspiration Layout)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Content details on the right side of the mascot
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 95.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
                    ) {
                        Text(
                            text = "ACTIVE SCHEDULER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${schedules.filter { it.isLocked }.size} Active Curfews",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Distraction pipelines are automatically shielded during scheduled hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mascot on the left (front view bg remove)
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_front_view),
                        contentDescription = "Cat Mascot Front",
                        modifier = Modifier
                            .size(90.dp)
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    )
                }
            }
        }

        // UNIFIED PRESETS DECKS MANAGER - SWIPEABLE HORIZONTAL
        item {
            Column {
                Text(
                    text = "Tactile Routine Decks (${unifiedDecks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(unifiedDecks) { deck ->
                        val isCustom = userPreservedDecks.contains(deck)
                        Card(
                            modifier = Modifier
                                .width(240.dp)
                                .testTag("preset_shield_deck_${deck.appName}")
                                .clickable {
                                    // Pre-populate form editors
                                    appNameField = deck.appName
                                    startTimeField = deck.startTime
                                    endTimeField = deck.endTime
                                    daysField = deck.daysOfWeek
                                    todoField = deck.todoWhileLocked
                                    smsAlertField = deck.customAlertSms
                                    cooldownField = deck.cooldownMinutes.toString()
                                    thresholdField = deck.usageThresholdMinutes.toString()
                                    showAddScheduleForm = true
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = deck.appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (isCustom) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "✨",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                if (isCustom) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("Custom Deck ✨", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("⏰ ${deck.startTime} - ${deck.endTime}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("📆 Days: ${deck.daysOfWeek}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                if (deck.usageThresholdMinutes > 0) {
                                    Text(
                                        text = "❄️ Cooldown: ${deck.cooldownMinutes}m after ${deck.usageThresholdMinutes}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            // Quick deploy
                                            viewModel.addLockSchedule(
                                                appName = deck.appName,
                                                startTime = deck.startTime,
                                                endTime = deck.endTime,
                                                days = deck.daysOfWeek,
                                                todo = deck.todoWhileLocked,
                                                smsMsg = deck.customAlertSms,
                                                cooldownMinutes = deck.cooldownMinutes,
                                                usageThresholdMinutes = deck.usageThresholdMinutes
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("quick_deploy_${deck.appName}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Quick Deploy ⚡", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }

                                    if (isCustom) {
                                        IconButton(
                                            onClick = { viewModel.removeSavedPresetDeck(deck.appName, deck.startTime) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete custom preset",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // DEPLOYED RUNNING LIST
        item {
            Text(
                text = "Live Active Restricting Pipelines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (schedules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
                            "No lock pipelines scheduled.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(schedules, key = { it.id }) { sched ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("schedules_screen_card_${sched.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏷️ ROUTINE: ${sched.appName.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Star preservation icon (toggles custom saved deck preservation)
                                IconButton(
                                    modifier = Modifier.testTag("save_as_preset_star_${sched.id}"),
                                    onClick = {
                                        viewModel.saveScheduleToPresets(
                                            appName = sched.appName,
                                            startTime = sched.startTime,
                                            endTime = sched.endTime,
                                            days = sched.daysOfWeek,
                                            todo = sched.todoWhileLocked,
                                            smsMsg = sched.customAlertSms,
                                            cooldownMinutes = sched.cooldownMinutes,
                                            usageThresholdMinutes = sched.usageThresholdMinutes
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Save as custom preset model",
                                        tint = Color(0xFFFFB300)
                                    )
                                }

                                Switch(
                                    checked = sched.isLocked,
                                    onCheckedChange = { viewModel.toggleScheduleLock(sched) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.deleteSchedule(sched.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete restriction schedule", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("⏰ Range: ${sched.startTime}h to ${sched.endTime}h", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("📆 Cadence: ${sched.daysOfWeek}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (sched.usageThresholdMinutes > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "❄️ Cooldown Cycle: Locking app for ${sched.cooldownMinutes}m after ${sched.usageThresholdMinutes}m cumulative scrolls",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (sched.customAlertSms.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Nudge SMS Alert: ${sched.customAlertSms}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // NEW LOCK SCHEDULE ENTRY SHEET FORM
    if (showAddScheduleForm) {
        AlertDialog(
            onDismissRequest = { showAddScheduleForm = false },
            title = { Text("🔒 Configure Locking Routine", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = appNameField,
                            onValueChange = { appNameField = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_appName_textfield"),
                            label = { Text("Schedule Title (e.g. Bedtime Block, Deep Study)") }
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startTimeField,
                                onValueChange = { startTimeField = it },
                                modifier = Modifier.weight(1f).testTag("add_startTime_textfield"),
                                label = { Text("Start Time") }
                            )
                            OutlinedTextField(
                                value = endTimeField,
                                onValueChange = { endTimeField = it },
                                modifier = Modifier.weight(1f).testTag("add_endTime_textfield"),
                                label = { Text("End Time") }
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = daysField,
                            onValueChange = { daysField = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_days_textfield"),
                            label = { Text("Days cadence (e.g. Daily)") }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = todoField,
                            onValueChange = { todoField = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_todo_textfield"),
                            label = { Text("Substitute physical routine tasks / goals") },
                            maxLines = 2
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = smsAlertField,
                            onValueChange = { smsAlertField = it },
                            modifier = Modifier.fillMaxWidth().testTag("add_sms_textfield"),
                            label = { Text("Supportive substitute SMS notification") },
                            maxLines = 2
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "⚙️ Cooldown Period Trigger (Usage Threshold)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = thresholdField,
                                onValueChange = { thresholdField = it },
                                modifier = Modifier.weight(1f).testTag("add_threshold_textfield"),
                                label = { Text("Scroll Limit (Minutes)") }
                            )
                            OutlinedTextField(
                                value = cooldownField,
                                onValueChange = { cooldownField = it },
                                modifier = Modifier.weight(1f).testTag("add_cooldown_textfield"),
                                label = { Text("Lock Duration (Minutes)") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ElevatedButton(
                        modifier = Modifier.testTag("save_preset_button"),
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        onClick = {
                            if (appNameField.isNotBlank()) {
                                viewModel.saveScheduleToPresets(
                                    appName = appNameField,
                                    startTime = startTimeField,
                                    endTime = endTimeField,
                                    days = daysField,
                                    todo = todoField,
                                    smsMsg = smsAlertField,
                                    cooldownMinutes = cooldownField.toIntOrNull() ?: 0,
                                    usageThresholdMinutes = thresholdField.toIntOrNull() ?: 0
                                )
                                showAddScheduleForm = false
                            }
                        }
                    ) {
                        Text("Save Preset ⚡", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }

                    Button(
                        modifier = Modifier.testTag("add_schedule_confirm"),
                        onClick = {
                            if (appNameField.isNotBlank()) {
                                viewModel.addLockSchedule(
                                    appName = appNameField,
                                    startTime = startTimeField,
                                    endTime = endTimeField,
                                    days = daysField,
                                    todo = todoField,
                                    smsMsg = smsAlertField,
                                    cooldownMinutes = cooldownField.toIntOrNull() ?: 0,
                                    usageThresholdMinutes = thresholdField.toIntOrNull() ?: 0
                                )
                                showAddScheduleForm = false
                            }
                        }
                    ) {
                        Text("Provision Lock", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddScheduleForm = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}
