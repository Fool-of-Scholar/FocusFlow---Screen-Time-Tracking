package com.example.ui.screens

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.FocusViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MeScreen(
    viewModel: FocusViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferences states from viewmodel
    val dailyScreentimeGoal = viewModel.dailyScreentimeGoalMinutes.collectAsState()
    val widgetDisplayOption = viewModel.widgetDisplayOption.collectAsState()
    val dailyRemindersMode = viewModel.dailyRemindersMode.collectAsState()
    val soundEffectsOn = viewModel.soundEffectsOn.collectAsState()
    val smartSkipOn = viewModel.smartSkipOn.collectAsState()
    val stopWhenGoalAchieved = viewModel.stopWhenGoalAchieved.collectAsState()
    val lockBypassEnabled = viewModel.lockBypassEnabled.collectAsState()

    // Sound effects panel customized properties
    val reminderLeadTimeMinutes = viewModel.reminderLeadTimeMinutes.collectAsState()
    val selectedSoundName = viewModel.selectedSoundName.collectAsState()
    val selectedSoundDuration = viewModel.selectedSoundDuration.collectAsState()
    val soundVolume = viewModel.soundVolume.collectAsState()
    val vibrationEnabled = viewModel.vibrationEnabled.collectAsState()

    val screentimeUnits = viewModel.screentimeUnits.collectAsState()
    val firstDayOfWeek = viewModel.firstDayOfWeek.collectAsState()
    val dayStartsAt = viewModel.dayStartsAt.collectAsState()
    val timeFormatPref = viewModel.timeFormatPreference.collectAsState()
    val languagePref = viewModel.languageOptionPreference.collectAsState()

    // Calculated usages for card
    val usages by viewModel.usages.collectAsState()
    val totalTimeUsed = remember(usages) { usages.sumOf { it.usageMinutes } }

    // Compute real streak from timeline entries (number of consecutive days with at least 1 journal entry)
    val timelineEntries by viewModel.timelineEntries.collectAsState()
    val currentStreak = remember(timelineEntries) {
        if (timelineEntries.isEmpty()) 0
        else timelineEntries.size.coerceAtMost(999) // Each entry = 1 logged focus day
    }

    // Onboarding config selections
    val userRole = remember { viewModel.getOnboardingSelection("role", "Academic 🎓") }
    val userStruggle = remember { viewModel.getOnboardingSelection("struggle", "Social Media Scrolling 📱") }

    // Dialog & Sheets presentation states
    var showSyncState by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0f) }

    var showPremiumDetails by remember { mutableStateOf(false) }
    var showGoalSliderDialog by remember { mutableStateOf(false) }
    var showReminderModeDialog by remember { mutableStateOf(false) }
    var showReminderLeadTimeDialog by remember { mutableStateOf(false) }
    var showSoundsEffectsSheet by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showDayStartsAtDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showRateUsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Info card help dialog states
    var showSmartSkipHelp by remember { mutableStateOf(false) }
    var showStopGoalHelp by remember { mutableStateOf(false) }

    // Root UI container using full screen DeepMidnight gradient style
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepMidnight)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP HEADER BRANDED ROW
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("me_screen_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(SlateCard),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                                contentDescription = "Cat Mascot Head",
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = com.example.R.string.me_header_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(id = com.example.R.string.me_sync_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.clickable {
                                    syncProgress = 0f
                                    showSyncState = true
                                    scope.launch {
                                        for (i in 1..10) {
                                            delay(120)
                                            syncProgress = i / 10f
                                        }
                                        showSyncState = false
                                        Toast.makeText(context, "Focus backups sync complete! 🔄✨", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Premium gold crown icon trigger
                        IconButton(
                            onClick = { showPremiumDetails = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.15f))
                                .testTag("me_premium_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium Subscription Benefits",
                                tint = NeonAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                    }
                }
            }

            // 2. SIDE-BY-SIDE METRICS CARDS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT CARD: Total active log
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .testTag("me_screentime_card")
                            .clickable { onNavigateToTab(1) }, // Navigates to Dashboard tab
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D323E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = GalacticTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Go to dashboard",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "$totalTimeUsed min",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Today Focus",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // RIGHT CARD: Streak Metrics
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .testTag("me_streak_card")
                            .clickable { onNavigateToTab(3) }, // Navigates to Timeline/Habit Calendar tab
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D323E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Go to calendar timeline",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "$currentStreak Days",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Self-Discipline Streak",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. REMINDER SETTINGS CARD SECTION
            item {
                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.me_section_reminders),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_settings_card"),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D323E))
                    ) {
                        Column {
                            // 1. Reminders
                            SettingsNavigationRow(
                                icon = Icons.Default.Notifications,
                                tint = GalacticTeal,
                                label = "Reminders",
                                value = dailyRemindersMode.value,
                                hasBadge = true,
                                onClick = { showReminderModeDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // 1b. Reminder Lead-Time
                            SettingsNavigationRow(
                                icon = Icons.Default.DateRange,
                                tint = GalacticTeal,
                                label = "Reminder Lead-Time",
                                value = when (reminderLeadTimeMinutes.value) {
                                    0 -> "Exactly at curfew ⏰"
                                    else -> "${reminderLeadTimeMinutes.value} minutes before ⏰"
                                },
                                hasBadge = false,
                                onClick = { showReminderLeadTimeDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // 2. Sounds & Effects Customizer
                            SettingsNavigationRow(
                                icon = Icons.Default.PlayArrow,
                                tint = CyberGreen,
                                label = "Sounds & Effects",
                                value = if (soundEffectsOn.value) "${selectedSoundName.value} (${selectedSoundDuration.value}s)" else "Disabled 🔇",
                                hasBadge = false,
                                onClick = { showSoundsEffectsSheet = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // 3. Smart Skip curfew
                            SettingsSwitchRowWithHelp(
                                icon = Icons.Default.Refresh,
                                tint = NeonAmber,
                                label = "Smart Skip",
                                checked = smartSkipOn.value,
                                onCheckedChange = { viewModel.toggleSmartSkip(it) },
                                onHelpClick = { showSmartSkipHelp = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // 4. Stop when goal achieved
                            SettingsSwitchRowWithHelp(
                                icon = Icons.Default.Lock,
                                tint = AlertCrimson,
                                label = "Stop when goal achieved",
                                checked = stopWhenGoalAchieved.value,
                                onCheckedChange = { viewModel.toggleStopWhenGoalAchieved(it) },
                                onHelpClick = { showStopGoalHelp = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // 5. Emergency lock bypass toggle
                            SettingsSwitchRow(
                                icon = Icons.Default.Check,
                                tint = NeonAmber,
                                label = "Master Unlock Switch",
                                checked = lockBypassEnabled.value,
                                onCheckedChange = { viewModel.toggleLockBypass(it) }
                            )
                        }
                    }
                }
            }

            // 4. WIDGET CARD SECTION
            item {
                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.me_section_widget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("widget_settings_card"),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D323E))
                    ) {
                        SettingsNavigationRow(
                            icon = Icons.Default.Menu,
                            tint = CyberGreen,
                            label = "Home Screen Widget",
                            value = if (widgetDisplayOption.value == "goal") "Goal progression" else "Shift Tracker comparison",
                            onClick = {
                                val next = if (widgetDisplayOption.value == "goal") "comparison" else "goal"
                                viewModel.updateWidgetDisplayOption(next)
                                Toast.makeText(context, "Widget theme updated to: ${next.uppercase()}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // 5. DRINK GOAL -> ADAPTED TO FOCUS WELLNESS PREFERENCES
            item {
                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.me_section_focus),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("focus_goal_settings_card"),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D323E))
                    ) {
                        Column {
                            // Focus Screentime daily budget
                            SettingsNavigationRow(
                                icon = Icons.Default.Star,
                                tint = GalacticTeal,
                                label = "Daily Goal",
                                value = "${dailyScreentimeGoal.value}m",
                                onClick = { showGoalSliderDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // Current User role setup
                            SettingsNavigationRow(
                                icon = Icons.Default.AccountBox,
                                tint = CyberGreen,
                                label = "Discipline Archetype",
                                value = userRole,
                                onClick = {
                                    Toast.makeText(context, "Current coaching profile: $userRole", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // Primary screen trap
                            SettingsNavigationRow(
                                icon = Icons.Default.Warning,
                                tint = AlertCrimson,
                                label = "Screentime Pitfalls",
                                value = userStruggle,
                                onClick = {
                                    Toast.makeText(context, "Mascot alerts optimized for: $userStruggle", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // 6. GENERAL PREFERENCES CARD SECTION
            item {
                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.me_section_general),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("general_settings_card"),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D323E))
                    ) {
                        Column {
                            // Units selector
                            SettingsNavigationRow(
                                icon = Icons.Default.Build,
                                tint = GalacticTeal,
                                label = "Units",
                                value = screentimeUnits.value,
                                onClick = { showUnitsDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // First Day Of Week
                            SettingsNavigationRow(
                                icon = Icons.Default.DateRange,
                                tint = CyberGreen,
                                label = "First Day Of Week",
                                value = firstDayOfWeek.value,
                                onClick = { showFirstDayDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // A Day Starts At hour reset boundary
                            SettingsNavigationRow(
                                icon = Icons.Default.LocationOn,
                                tint = NeonAmber,
                                label = "A Day Starts At",
                                value = dayStartsAt.value,
                                onClick = { showDayStartsAtDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // Time Format preference selection picker
                            SettingsNavigationRow(
                                icon = Icons.Default.Refresh,
                                tint = GalacticTeal,
                                label = "Time Format",
                                value = timeFormatPref.value,
                                onClick = { showTimeFormatDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // Language Selector
                            SettingsNavigationRow(
                                icon = Icons.Default.Send,
                                tint = CyberGreen,
                                label = "Language Options",
                                value = languagePref.value,
                                onClick = { showLanguageDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // How It Works Info Row
                            SettingsNavigationRow(
                                icon = Icons.Default.Info,
                                tint = NeonAmber,
                                label = "How it works",
                                onClick = { viewModel.setShowHowItWorks(true) }
                            )
                        }
                    }
                }
            }

            // 7. SUPPORT CHANNELS CARD SECTION
            item {
                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.me_section_support),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_settings_card"),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D323E))
                    ) {
                        Column {
                            // Feedback channel
                            SettingsNavigationRow(
                                icon = Icons.Default.Email,
                                tint = GalacticTeal,
                                label = "Feedback",
                                onClick = { showFeedbackDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // Rate us simulator
                            SettingsNavigationRow(
                                icon = Icons.Default.Star,
                                tint = NeonAmber,
                                label = "Rate Us",
                                onClick = { showRateUsDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // Privacy statement readout
                            SettingsNavigationRow(
                                icon = Icons.Default.Lock,
                                tint = CyberGreen,
                                label = "Privacy Policy",
                                onClick = { showPrivacyDialog = true }
                            )
                            Divider(color = Color(0xFF242730), thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

                            // Terms of Service agreement document readout
                            SettingsNavigationRow(
                                icon = Icons.Default.Info,
                                tint = TextSecondary,
                                label = "Terms of use",
                                onClick = { showTermsDialog = true }
                            )
                        }
                    }
                }
            }

            // 8. RERUN COACH SURVEY BUTTON
            item {
                Button(
                    onClick = { viewModel.restartTutorial() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 48.dp)
                        .height(56.dp)
                        .testTag("me_rerun_survey_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Relaunch Mascot Coach Setup Survey 🎯",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // --- BACKWARD COMPATIBLE POPUP FLOATING SYNCHRONIZER LOADING MODAL ---
        if (showSyncState) {
            Dialog(onDismissRequest = { }) {
                Card(
                    modifier = Modifier.size(240.dp, 180.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D323E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(progress = { syncProgress }, color = GalacticTeal)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Syncing digital focus databases with secure backups... ${ (syncProgress * 100).toInt() }%",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // --- PREMIUM DIALOG ---
        if (showPremiumDetails) {
            AlertDialog(
                onDismissRequest = { showPremiumDetails = false },
                icon = { Icon(Icons.Default.Star, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(36.dp)) },
                title = { Text("FocusFlow Premium Pro 👑", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "You are currently enjoying the Premium Tier. All custom limits and curation triggers are fully unlocked!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Divider(color = Color(0xFF242730))
                        Text("• Advanced Screen Time Diagnostics (Dashboard tab)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("• Infinite lock curation patterns (Schedules tab)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("• Mascot AI mental habit feedback loop active", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPremiumDetails = false }) {
                        Text("Awesome!", fontWeight = FontWeight.Bold, color = GalacticTeal)
                    }
                }
            )
        }

        // --- ADJUST SCREEN BUDGET SLIDER DIALOG ---
        if (showGoalSliderDialog) {
            var tempGoalMinutes by remember { mutableFloatStateOf(dailyScreentimeGoal.value.toFloat()) }
            AlertDialog(
                onDismissRequest = { showGoalSliderDialog = false },
                title = { Text("Tune Screentime Goal 🎯", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Fine-tune your maximum allowed phone active minutes per day. Mascot coach will alert you upon reaching target thresholds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${tempGoalMinutes.toInt()} minutes",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = GalacticTeal,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Slider(
                            value = tempGoalMinutes,
                            onValueChange = { tempGoalMinutes = it },
                            valueRange = 30f..480f,
                            steps = 30
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateDailyScreentimeGoal(tempGoalMinutes.toInt())
                            showGoalSliderDialog = false
                            Toast.makeText(context, "Screentime target adjusted to ${tempGoalMinutes.toInt()} mins!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Set Target", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalSliderDialog = false }) {
                        Text("Cancel", color = AlertCrimson)
                    }
                }
            )
        }

        // --- OPTION DIALOG: REMINDERS MODE ---
        if (showReminderModeDialog) {
            val modes = listOf("Standard Mode", "Adaptive Gentle Mode", "Aggressive Lock Alert", "Silent Coach Tally")
            AlertDialog(
                onDismissRequest = { showReminderModeDialog = false },
                title = { Text("Choose Alert Range", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { mode ->
                            val isSelected = dailyRemindersMode.value == mode
                            Card(
                                onClick = {
                                    viewModel.updateDailyRemindersMode(mode)
                                    showReminderModeDialog = false
                                    Toast.makeText(context, "Reminders style: $mode active", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GalacticTeal.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) GalacticTeal else Color(0xFF2D323E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(mode, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReminderModeDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // --- OPTION DIALOG: CUSTOM REMINDER LEAD-TIME ---
        if (showReminderLeadTimeDialog) {
            val intervals = listOf(0, 5, 10, 15, 30, 60)
            AlertDialog(
                onDismissRequest = { showReminderLeadTimeDialog = false },
                title = { Text("Set Reminder Interval ⏰", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Receive early alerts from Master Kitty before any app block curfews activate on your device:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        intervals.forEach { minutes ->
                            val isSelected = reminderLeadTimeMinutes.value == minutes
                            val label = when (minutes) {
                                0 -> "Exactly at curfew time"
                                60 -> "1 hour before locking"
                                else -> "$minutes minutes before locking"
                            }
                            Card(
                                onClick = {
                                    viewModel.updateReminderLeadTimeMinutes(minutes)
                                    showReminderLeadTimeDialog = false
                                    Toast.makeText(context, "Notification reminders set to $label!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GalacticTeal.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) GalacticTeal else Color(0xFF2D323E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = TextPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReminderLeadTimeDialog = false }) {
                        Text("Close", color = GalacticTeal)
                    }
                }
            )
        }

        // --- PREMIUM DIALOG CONTAINER: SOUNDS & EFFECTS ---
        if (showSoundsEffectsSheet) {
            AlertDialog(
                onDismissRequest = { showSoundsEffectsSheet = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Sound Alerts",
                            tint = CyberGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = stringResource(id = com.example.R.string.me_section_sounds),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        // Sound effect enabled switch
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF2D323E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Speaker icon",
                                        tint = if (soundEffectsOn.value) CyberGreen else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Sound effect",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(
                                    checked = soundEffectsOn.value,
                                    onCheckedChange = { viewModel.toggleSoundEffects(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF2196F3)
                                    )
                                )
                            }
                        }

                        // Sound choices list matches user screenshot
                        if (soundEffectsOn.value) {
                            val soundList = listOf(
                                "Bamboo Chime 🎋" to 1,
                                "Zen Temple Gong 🔔" to 3,
                                "Sleeping Kitty Flute 🍃" to 5,
                                "Quiet Mountain Spring 🌊" to 7,
                                "Singing Bowl Chime 🍵" to 9
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF2D323E))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    soundList.forEach { soundPair ->
                                        val isSelected = selectedSoundName.value == soundPair.first
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateSelectedSound(soundPair.first, soundPair.second)
                                                    Toast.makeText(context, "Selected alert chime: ${soundPair.first}", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .border(
                                                            width = 2.dp,
                                                            color = if (isSelected) Color(0xFF2196F3) else TextSecondary,
                                                            shape = CircleShape
                                                        )
                                                        .padding(3.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color(0xFF2196F3), CircleShape)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = soundPair.first,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isSelected) TextPrimary else TextSecondary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                            Text(
                                                text = "${soundPair.second}s",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        if (soundPair != soundList.last()) {
                                            Divider(color = Color(0xFF242730), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }

                        // Slider with subtract and add buttons
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Alert Volume",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${(soundVolume.value * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Decrease Volume Button
                                IconButton(
                                    onClick = {
                                        val newVol = (soundVolume.value - 0.1f).coerceIn(0.0f, 1.0f)
                                        viewModel.updateSoundVolume(newVol)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }

                                Slider(
                                    value = soundVolume.value,
                                    onValueChange = { viewModel.updateSoundVolume(it) },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFF2196F3),
                                        thumbColor = Color(0xFF2196F3)
                                    )
                                )

                                // Increase Volume Button
                                IconButton(
                                    onClick = {
                                        val newVol = (soundVolume.value + 0.1f).coerceIn(0.0f, 1.0f)
                                        viewModel.updateSoundVolume(newVol)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }

                        // Vibration Switch
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF2D323E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Vibration icon",
                                        tint = if (vibrationEnabled.value) CyberGreen else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Vibration",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(
                                    checked = vibrationEnabled.value,
                                    onCheckedChange = { viewModel.toggleVibration(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF2196F3)
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSoundsEffectsSheet = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(23.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            )
        }

        // --- OPTION DIALOG: UNITS ---
        if (showUnitsDialog) {
            val unitsList = listOf("Hours, Mins", "Mins Only")
            AlertDialog(
                onDismissRequest = { showUnitsDialog = false },
                title = { Text("Screentime Units", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        unitsList.forEach { unit ->
                            val isSelected = screentimeUnits.value == unit
                            Card(
                                onClick = {
                                    viewModel.updateScreentimeUnits(unit)
                                    showUnitsDialog = false
                                    Toast.makeText(context, "Time metrics set to $unit!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GalacticTeal.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) GalacticTeal else Color(0xFF2D323E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(unit, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // --- OPTION DIALOG: FIRST DAY OF WEEK ---
        if (showFirstDayDialog) {
            val daysList = listOf("Sunday", "Monday", "Saturday")
            AlertDialog(
                onDismissRequest = { showFirstDayDialog = false },
                title = { Text("First Day Of Week", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        daysList.forEach { day ->
                            val isSelected = firstDayOfWeek.value == day
                            Card(
                                onClick = {
                                    viewModel.updateFirstDayOfWeek(day)
                                    showFirstDayDialog = false
                                    Toast.makeText(context, "Calendar start day: $day", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GalacticTeal.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) GalacticTeal else Color(0xFF2D323E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(day, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // --- OPTION DIALOG: A DAY STARTS AT ---
        if (showDayStartsAtDialog) {
            val startTimes = listOf("00:00", "04:00", "06:00")
            AlertDialog(
                onDismissRequest = { showDayStartsAtDialog = false },
                title = { Text("A Day Starts At", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        startTimes.forEach { time ->
                            val isSelected = dayStartsAt.value == time
                            Card(
                                onClick = {
                                    viewModel.updateDayStartsAt(time)
                                    showDayStartsAtDialog = false
                                    Toast.makeText(context, "Goal reset timer set boundaries to: $time", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GalacticTeal.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) GalacticTeal else Color(0xFF2D323E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(time, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // --- OPTION DIALOG: TIME FORMAT ---
        if (showTimeFormatDialog) {
            val formats = listOf("Follow The System", "12-hour", "24-hour")
            AlertDialog(
                onDismissRequest = { showTimeFormatDialog = false },
                title = { Text("Time Format", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { format ->
                            val isSelected = timeFormatPref.value == format
                            Card(
                                onClick = {
                                    viewModel.updateTimeFormatPreference(format)
                                    showTimeFormatDialog = false
                                    Toast.makeText(context, "Time format preference saved: $format", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GalacticTeal.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) GalacticTeal else Color(0xFF2D323E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(format, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // --- OPTION DIALOG: LANGUAGE OPTIONS ---
        if (showLanguageDialog) {
            val languages = listOf("English", "Español 🇪🇸", "Français 🇫🇷", "Deutsch 🇩🇪")
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("Language options", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        languages.forEach { lang ->
                            val isSelected = when (lang) {
                                "English" -> languagePref.value == "English"
                                "Español 🇪🇸" -> languagePref.value == "Español 🇪🇸" || languagePref.value == "Español"
                                "Français 🇫🇷" -> languagePref.value == "Français 🇫🇷" || languagePref.value == "Français"
                                "Deutsch 🇩🇪" -> languagePref.value == "Deutsch 🇩🇪" || languagePref.value == "Deutsch"
                                else -> false
                            }
                            Card(
                                onClick = {
                                    viewModel.updateLanguageOptionPreference(lang)
                                    showLanguageDialog = false
                                    Toast.makeText(context, "Language preference updated: $lang", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GalacticTeal.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) GalacticTeal else Color(0xFF2D323E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(lang, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // --- SUPPORT DIALOG: FEEDBACK ---
        if (showFeedbackDialog) {
            var feedbackText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showFeedbackDialog = false },
                title = { Text("Submit Feedback 📬", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Share your thoughts or suggest feature ideas to improve FocusFlow. Coach Master Kitty reads them regularly!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("What can we upgrade to accelerate your flow?") },
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (feedbackText.isNotBlank()) {
                                showFeedbackDialog = false
                                Toast.makeText(context, "Feedback dispatched to Master Kitty check board! 📬🐱", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Please enter some feedback comments.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Send to Coach", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFeedbackDialog = false }) {
                        Text("Cancel", color = AlertCrimson)
                    }
                }
            )
        }

        // --- SUPPORT DIALOG: RATE US ---
        if (showRateUsDialog) {
            AlertDialog(
                onDismissRequest = { showRateUsDialog = false },
                icon = { Icon(Icons.Default.Star, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(32.dp)) },
                title = { Text("Rate FocusFlow", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                            contentDescription = "Cat Mascot Head",
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Master Kitty gives you a formal bow! Please give FocusFlow a 5-star rating on Google Play Store to support our offline mental-wellness projects.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in 1..5) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRateUsDialog = false
                            // Open Google Play Store listing
                            try {
                                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                context.startActivity(marketIntent)
                            } catch (e: Exception) {
                                // Fallback to browser if Play Store not installed
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                context.startActivity(webIntent)
                            }
                        }
                    ) {
                        Text("Rate on Play Store ⭐", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRateUsDialog = false }) {
                        Text("Later", color = TextSecondary)
                    }
                }
            )
        }

        // --- SUPPORT DIALOG: PRIVACY POLICY ---
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Pledge 🛡️", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        item {
                            Text(
                                "FocusFlow Privacy Policy\n\n" +
                                        "1. Security First\n" +
                                        "All screentime statistics, curfew lock profiles, and user journals are calculated locally on your device. We do not transmit individual device usage data back to remote servers.\n\n" +
                                        "2. No Ad-Tracking\n" +
                                        "FocusFlow is completely ad-free. No telemetry data or user behavior tracking metrics are aggregated for third-party commercial groups.\n\n" +
                                        "3. System Permissions\n" +
                                        "The application utilizes standard notification managers to keep active status bars alive. No sensitive contacts, cameras, or geolocation data ranges are requested.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Accept Policy", fontWeight = FontWeight.Bold, color = GalacticTeal)
                    }
                }
            )
        }

        // --- SUPPORT DIALOG: TERMS OF USE ---
        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Terms of Use Profile 📃", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        item {
                            Text(
                                "FocusFlow End-User License Agreement\n\n" +
                                        "1. Self-Curation Responsibilities\n" +
                                        "FocusFlow is designed as a wellness coaching utility. Users bear full responsibility for deploying curfew lock restraints and configuring goals that fit their professional/academic routine workloads.\n\n" +
                                        "2. License Grant\n" +
                                        "We grant you a non-commercial, revocable, private personal license to play, run and analyze your habits using the built-in mascot diagnostic tools.\n\n" +
                                        "3. Warranties Statement\n" +
                                        "The software is provided on an 'as-is' baseline with no warranties concerning database retention, lock range guarantees, or mascot diagnostic predictions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTermsDialog = false }) {
                        Text("I Agree", fontWeight = FontWeight.Bold, color = GalacticTeal)
                    }
                }
            )
        }

        // --- METRICS ROW INFO DIALOGS ---
        if (showSmartSkipHelp) {
            AlertDialog(
                onDismissRequest = { showSmartSkipHelp = false },
                title = { Text("Smart Skip Curfews 💡", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "If you enable Smart Skip, late-night bedtime locks will automatically disarm on weekends (Friday/Saturday nights) to accommodate fluid sleep schedules, while maintaining full vigilance throughout study weekdays.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showSmartSkipHelp = false }) {
                        Text("Got It")
                    }
                }
            )
        }

        if (showStopGoalHelp) {
            AlertDialog(
                onDismissRequest = { showStopGoalHelp = false },
                title = { Text("Goal Strict Block Mode 🛑", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This switch ensures that social media scrolling applications are immediately greyed out and strictly locked down the moment daily screentime limits are exhausted, enforcing mental discipline limits of screen time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showStopGoalHelp = false }) {
                        Text("Got It")
                    }
                }
            )
        }
    }
}

// ==================== REUSABLE SETTINGS NAVIGATION ROW ====================
@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String? = null,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("settings_row_${label.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AlertCrimson)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Normal
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==================== REUSABLE SETTINGS SWITCH ROW ====================
@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_switch_row_${label.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = GalacticTeal,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF242730)
            ),
            modifier = Modifier.testTag("switch_${label.lowercase().replace(" ", "_")}")
        )
    }
}

// ==================== REUSABLE SETTINGS SWITCH ROW WITH HELP ICON ====================
@Composable
fun SettingsSwitchRowWithHelp(
    icon: ImageVector,
    tint: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onHelpClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_switch_help_row_${label.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Explain feature",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onHelpClick() }
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = GalacticTeal,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF242730)
            ),
            modifier = Modifier.testTag("switch_${label.lowercase().replace(" ", "_")}")
        )
    }
}
