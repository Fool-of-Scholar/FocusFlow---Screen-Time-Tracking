package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.provider.Settings
import android.content.Intent
import com.example.ui.viewmodel.FocusViewModel
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.example.service.FocusFlowWidgetProvider
import android.os.Build
import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FocusViewModel,
    onNavigateToMe: () -> Unit
) {
    val usages by viewModel.usages.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val dailyScreentimeGoalMinutes by viewModel.dailyScreentimeGoalMinutes.collectAsState()
    val widgetDisplayOption by viewModel.widgetDisplayOption.collectAsState()
    val previousScreentimeMinutes by viewModel.previousScreentimeMinutes.collectAsState()

    val accessibilityGranted by viewModel.accessibilityPermissionGranted.collectAsState()
    val usageAccessGranted by viewModel.usageAccessPermissionGranted.collectAsState()
    val soundEffectsOn by viewModel.soundEffectsOn.collectAsState()
    val screentimeUnits by viewModel.screentimeUnits.collectAsState()
    val timeFormatPreference by viewModel.timeFormatPreference.collectAsState()
    val context = LocalContext.current

    // Alert sound & notifications states
    val selectedSoundName by viewModel.selectedSoundName.collectAsState()
    val selectedSoundDuration by viewModel.selectedSoundDuration.collectAsState()
    val soundVolume by viewModel.soundVolume.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val notificationLogs by viewModel.notificationLogs.collectAsState()

    var showNotificationLogsSheet by remember { mutableStateOf(false) }
    var showSoundsEffectsSheet by remember { mutableStateOf(false) }
    var logsTrackingEnabled by remember { mutableStateOf(true) }

    var showLocksFaq by remember { mutableStateOf(false) }
    var customAppName by remember { mutableStateOf("") }
    var isNewAppProductive by remember { mutableStateOf(false) }
    var showPermissionGuideDialog by remember { mutableStateOf(!accessibilityGranted || !usageAccessGranted) }

    var homeActiveTab by remember { mutableIntStateOf(0) } // 0 = Active Locks, 1 = Screen Time Breakdown, 2 = All App Manager

    // Modal state for adding simulated test usage
    var showAddUsageDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val totalTime = remember(usages) { usages.sumOf { it.usageMinutes } }
    val isOverLimit = remember(totalTime, dailyScreentimeGoalMinutes) { totalTime > dailyScreentimeGoalMinutes }
    val progressColor = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    // Streak from timeline entries (1 entry = 1 focus day)
    val timelineEntries by viewModel.timelineEntries.collectAsState()
    val currentStreak = remember(timelineEntries) { timelineEntries.size.coerceAtMost(999) }

    // Persisted alert frequency level
    val alertFrequencyLevel by viewModel.alertFrequencyLevel.collectAsState()

    val scrollState = rememberScrollState()

    // GOOGLE PLAY IN-APP REVIEW TRIGGER (3-Day Streak)
    val sharedPrefs = remember { context.getSharedPreferences("focusflow_prefs_v5", android.content.Context.MODE_PRIVATE) }
    LaunchedEffect(currentStreak) {
        if (currentStreak >= 3) {
            val hasPrompted = sharedPrefs.getBoolean("has_prompted_review_v5", false)
            if (!hasPrompted) {
                try {
                    val reviewManager = ReviewManagerFactory.create(context)
                    val request = reviewManager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val activity = context as? Activity
                            if (activity != null) {
                                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                                flow.addOnCompleteListener { _ ->
                                    sharedPrefs.edit().putBoolean("has_prompted_review_v5", true).apply()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // TOP BANNER (Streak indicator + round controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: STREAK CAPSULE / PILL
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = com.example.R.string.home_streak, currentStreak),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: ROUND CONTROLS (Notification & Audio Speaker)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bell alert button (Opens Notification Logs)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            showNotificationLogsSheet = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Trigger alert notification",
                        tint = if (logsTrackingEnabled && notificationLogs.isNotEmpty()) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Audio Speaker Toggle (Opens Sounds & Effects Customizer Card)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            showSoundsEffectsSheet = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (soundEffectsOn) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = "Toggle sound effects",
                        tint = if (soundEffectsOn) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // BIG CENTRED DISPLAY: Screentime today (Format: 1.9 HR)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isMinsOnly = screentimeUnits == "Mins Only"
            val displayValue = if (isMinsOnly) {
                totalTime.toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", totalTime / 60.0)
            }
            val displayUnit = if (isMinsOnly) "MINS" else "HRS"
            
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayUnit,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Text(
                text = stringResource(id = com.example.R.string.spent_on_phone, totalTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // HORIZONTAL SIDE-BY-SIDE SUMMARY CARDS (TARGET & NEXT LOCK)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Card: Daily screentime target (Limit)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(82.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Blue vertical status bar accent
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = com.example.R.string.limit_target),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Goal",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { showSettingsDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val percent = if (dailyScreentimeGoalMinutes > 0) (totalTime * 100) / dailyScreentimeGoalMinutes else 0
                        Text(
                            text = "${dailyScreentimeGoalMinutes}m ($percent%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Right Card: Next Schedule / Lockdown Countdown
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(82.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Orange vertical status bar accent
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFFF9800))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = com.example.R.string.next_lock),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // Dynamically look up next schedule curfew
                        val nextSched = schedules.firstOrNull { it.isLocked }
                        val nextLockText = if (nextSched != null) {
                            val timeStr = nextSched.startTime
                            val formattedTime = if (timeFormatPreference == "12-hour") {
                                val parts = timeStr.split(":")
                                if (parts.size == 2) {
                                    val h24 = parts[0].toIntOrNull() ?: 0
                                    val m = parts[1]
                                    val amPm = if (h24 >= 12) "PM" else "AM"
                                    var h12 = h24 % 12
                                    if (h12 == 0) h12 = 12
                                    "$h12:$m $amPm"
                                } else timeStr
                            } else timeStr
                            "${nextSched.appName}: $formattedTime"
                        } else {
                            stringResource(id = com.example.R.string.no_active_curfews)
                        }
                        Text(
                            text = nextLockText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. LIVE PHONE WIDGET & NOTIFICATION PREVIEW CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("home_widget_preview_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (widgetDisplayOption == "goal") Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LIVE PHONE WIDGET & NOTIFICATION ACTIVE PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Text(
                                text = "Pin to Home Screen",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 4.dp, start = 22.dp)
                                    .clickable {
                                        val appWidgetManager = AppWidgetManager.getInstance(context)
                                        val myProvider = ComponentName(context, FocusFlowWidgetProvider::class.java)
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                        } else {
                                            Toast.makeText(context, "Launcher does not support pinning.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                            .clickable {
                                val nextOption = if (widgetDisplayOption == "goal") "comparison" else "goal"
                                viewModel.updateWidgetDisplayOption(nextOption)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Toggle Mode",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = widgetDisplayOption,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "widget_switcher"
                ) { targetOption ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        if (targetOption == "goal") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 85.dp)
                                    .align(Alignment.Center),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "FocusFlow Widget 📱",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${((totalTime.toFloat() / dailyScreentimeGoalMinutes * 100).toInt()).coerceAtLeast(0)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = progressColor
                                        )
                                    }
                                    
                                    LinearProgressIndicator(
                                        progress = { (totalTime.toFloat() / dailyScreentimeGoalMinutes).coerceAtMost(1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = progressColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        text = "$totalTime mins used out of $dailyScreentimeGoalMinutes goal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            val diff = totalTime - previousScreentimeMinutes
                            val percentShift = if (previousScreentimeMinutes > 0) (diff.toFloat() / previousScreentimeMinutes * 100).toInt() else 0
                            val improvement = diff <= 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 85.dp)
                                    .align(Alignment.Center),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "FocusFlow Widget: Shift Tracker 📊",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Today: $totalTime m",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Prev Day: $previousScreentimeMinutes m",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        Surface(
                                            color = if (improvement) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (improvement) " ${-diff} min decline! " else " +$diff min increase! ",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = if (improvement) Color(0xFF2EBD6B) else Color(0xFFE53935),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Cat Mascot standing on the left side of the widget card
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_full_view),
                            contentDescription = "Cat Mascot",
                            modifier = Modifier
                                .size(110.dp)
                                .align(Alignment.CenterStart)
                        )
                    }
                }
            }
        }

        // TABS CHIPS CHANGER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                stringResource(id = com.example.R.string.tab_active_locks),
                stringResource(id = com.example.R.string.tab_visualizer),
                stringResource(id = com.example.R.string.tab_classifier)
            )
            tabs.forEachIndexed { index, title ->
                val isSelected = homeActiveTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { homeActiveTab = index }
                        .padding(vertical = 10.dp)
                        .testTag("home_segment_tab_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // VIEW RENDERING SWITCHER
        when (homeActiveTab) {
            0 -> {
                // ACTIVE LOCKS VIEW WITH FAQS, PERMISSION CHANNELS & CURFEWS
                Column(modifier = Modifier.fillMaxWidth()) {
                    // FAQ expandable card for explanation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("lock_faq_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLocksFaq = !showLocksFaq },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "How Do App Locks Work? & Purpose",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Icon(
                                    imageVector = if (showLocksFaq) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (showLocksFaq) "Collapse" else "Expand"
                                )
                            }
                            
                            AnimatedVisibility(visible = showLocksFaq) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Text(
                                        text = "🎯 Purpose of App Locks:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• Disrupt Dopamine Loops: Introduce friction (locks) to prevent subconscious tap habits on high-scroll applications (social/media).\n" +
                                               "• Constructive Redirection: Instead of leaving you empty, locks redirect you to a preset bedtime list of wholesome off-screen exercises (like reading paper books or stretches).",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "⚙️ How It Works on Android:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• Localized Screen Shield: By listening to background window changes via Accessibility APIs, FocusFlow instantly creates secure overlays on locked apps during designated curfew hours.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // ⚠️ SYSTEM ACCESSIBILITY & USAGE STATUS
                    if (!accessibilityGranted || !usageAccessGranted) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { showPermissionGuideDialog = true }
                                .testTag("permissions_warning_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = MaterialTheme.colorScheme.error)
                                    Text(
                                        text = stringResource(id = com.example.R.string.action_needed),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "To securely lock distracting apps and log screen time, Android OS requires dynamic settings permissions to overlay interventions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        modifier = Modifier.weight(1f).testTag("sim_grant_perms"),
                                        onClick = {
                                            viewModel.setAccessibilityPermissionGranted(true)
                                            viewModel.setUsageAccessPermissionGranted(true)
                                            Toast.makeText(context, "✨ Permissions Auto-Granted (Simulation Approved)!", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Auto-Grant ✨", fontWeight = FontWeight.Bold)
                                    }
                                    
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f).testTag("system_settings_perms"),
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Opening accessibility settings...", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer)
                                    ) {
                                        Text("Open Settings ⚙️", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    } else {
                        // Permissions successfully connected badge
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color(0xFF2EBD6B))
                                Text(
                                    text = "FocusFlow Locker Service Running (Simulated OS Settings Active)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }

                    // RESTRICTIONS HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Lock Curation 🛡️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val distractionApps = remember(usages) { usages.filter { it.category == "Distraction" } }

                    if (distractionApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                                    contentDescription = "Cat Mascot Head",
                                    modifier = Modifier.size(90.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No distracting apps flagged yet!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Head to the 'Classifier' section to categorize focus-blocking apps so we can deploy shields.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { homeActiveTab = 2 },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Go to Classifier 🏷️", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    } else {
                        val lockBypassEnabled by viewModel.lockBypassEnabled.collectAsState()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            distractionApps.forEach { app ->
                                val activeScheduleRunning = schedules.firstOrNull { it.isLocked && com.example.ui.viewmodel.isTimeInSchedule(it.startTime, it.endTime) }
                                val isLocked = activeScheduleRunning != null && !lockBypassEnabled

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("distraction_lock_card_${app.appName}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant
                                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = BorderStroke(1.dp, if (isLocked) Color(0xFFEF5350).copy(alpha = 0.4f) else Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                AppLogoIcon(appName = app.appName, isProductive = false, modifier = Modifier.size(40.dp))
                                                Column {
                                                    Text(
                                                        text = app.appName.uppercase(),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Text(
                                                        text = if (isLocked) "🚫 STRICTLY LOCKED DOWN" 
                                                               else if (lockBypassEnabled && activeScheduleRunning != null) "🔓 TEMPORARILY BYPASSED (Active in Me)" 
                                                               else "🟢 SHIELD ARMED (Passive monitoring)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isLocked) Color(0xFFEF5350) else if (lockBypassEnabled && activeScheduleRunning != null) Color(0xFFFF9100) else Color(0xFF2EBD6B)
                                                    )
                                                }
                                            }
                                            
                                            // Status badge — shows live lock/shield state
                                            Surface(
                                                color = if (isLocked) Color(0xFFEF5350).copy(alpha = 0.15f)
                                                        else Color(0xFF2EBD6B).copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text(
                                                    text = if (isLocked) "🚫 Locked" else "🛡️ Armed",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isLocked) Color(0xFFEF5350) else Color(0xFF2EBD6B),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        if (isLocked) {
                                            val activeSched = activeScheduleRunning
                                            val startTime = activeSched?.startTime ?: "22:00"
                                            val endTime = activeSched?.endTime ?: "07:00"

                                            val formattedStart = if (timeFormatPreference == "12-hour") {
                                                val p = startTime.split(":")
                                                if (p.size == 2) {
                                                    val h24 = p[0].toIntOrNull() ?: 0
                                                    val amPm = if (h24 >= 12) "PM" else "AM"
                                                    var h12 = h24 % 12
                                                    if (h12 == 0) h12 = 12
                                                    "$h12:${p[1]} $amPm"
                                                } else startTime
                                            } else startTime

                                            val formattedEnd = if (timeFormatPreference == "12-hour") {
                                                val p = endTime.split(":")
                                                if (p.size == 2) {
                                                    val h24 = p[0].toIntOrNull() ?: 0
                                                    val amPm = if (h24 >= 12) "PM" else "AM"
                                                    var h12 = h24 % 12
                                                    if (h12 == 0) h12 = 12
                                                    "$h12:${p[1]} $amPm"
                                                } else endTime
                                            } else endTime

                                            val schedName = activeSched?.appName ?: "Active Curfew"
                                            val todoStr = activeSched?.todoWhileLocked ?: "Engage in substitution screen-free routines."

                                            Surface(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = stringResource(id = com.example.R.string.active_lockdown, schedName, formattedStart, formattedEnd),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "💡 Substitution Task:\n$todoStr",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        } else if (lockBypassEnabled && activeScheduleRunning != null) {
                                            Surface(
                                                color = Color(0xFFFFECEB),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "⚠️ You have turned on the 'Master Unlock Switch' in your profile settings. Take care of your deep focus goals!",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFD32F2F),
                                                    modifier = Modifier.padding(10.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "🟢 This distraction app is armed. It will lock down automatically during any of your active curfew schedules and stay locked until the routines finish.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // SCREEN TIME BREAKDOWN VISUALIZER VIEW + INDIVIDUAL BLOCK Toggles
                val totalUsed = usages.sumOf { it.usageMinutes }
                val productiveTotal = usages.filter { it.category == "Productive" }.sumOf { it.usageMinutes }
                val distractingTotal = usages.filter { it.category == "Distraction" }.sumOf { it.usageMinutes }
                val lockBypassEnabled by viewModel.lockBypassEnabled.collectAsState()
                
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "DIGITAL RATIO CIRCLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Circular visual ring meter drawing
                            Box(
                                modifier = Modifier.size(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(140.dp)) {
                                    val strokeWidth = 14.dp.toPx()
                                    val ratioDistract = if (totalUsed > 0) distractingTotal.toFloat() / totalUsed else 0f
                                    val angleDistract = ratioDistract * 360f

                                    // Inactive gray track
                                    drawCircle(
                                        color = Color(0xFF2C3240),
                                        style = Stroke(width = strokeWidth)
                                    )

                                    // Productive Sweep (Cyber green)
                                    drawArc(
                                        color = Color(0xFF2EBD6B),
                                        startAngle = -90f,
                                        sweepAngle = 360f - angleDistract,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )

                                    // Distracting Sweep (Crimson red)
                                    drawArc(
                                        color = Color(0xFFE53935),
                                        startAngle = -90f + (360f - angleDistract),
                                        sweepAngle = angleDistract,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$totalUsed",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "mins logged",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Legend indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF2EBD6B)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Productive: ${productiveTotal}m", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFE53935)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Distracting: ${distractingTotal}m", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // BREAKDOWN INTRO
                    Column {
                        Text(
                            "App Usage Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Toggle the locker switch to instantly block distracting apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (usages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No logged usage logs yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        usages.forEach { appRecord ->
                            val isProductive = appRecord.category == "Productive"
                            val activeScheduleRunning = schedules.firstOrNull { it.isLocked && com.example.ui.viewmodel.isTimeInSchedule(it.startTime, it.endTime) }
                            val isLockedForThisApp = !isProductive && activeScheduleRunning != null && !lockBypassEnabled

                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("visualizer_app_card_${appRecord.appName}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // App Circular Icon with customizable brand logo fallback
                                        AppLogoIcon(appName = appRecord.appName, isProductive = isProductive)

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = appRecord.appName,
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Surface(
                                                    color = if (isProductive) Color(0xFF2EBD6B).copy(alpha = 0.15f) else Color(0xFFE53935).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isProductive) "Productive" else "Distraction",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isProductive) Color(0xFF2EBD6B) else Color(0xFFE53935),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${appRecord.usageMinutes} minutes logged",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Touch target interactive lock toggle switcher
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (isLockedForThisApp) "Locked Down 🚫" 
                                                       else if (!isProductive) "Armed 🛡️" 
                                                       else "Allowed ✅",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = if (isLockedForThisApp) Color(0xFFE53935) else if (!isProductive) Color(0xFFFF9100) else Color(0xFF2EBD6B)
                                            )
                                            Text(
                                                text = "Distraction App",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Switch(
                                            checked = !isProductive,
                                            onCheckedChange = { isDistraction ->
                                                val newCategory = if (isDistraction) "Distraction" else "Productive"
                                                viewModel.insertUsageRecord(
                                                    appName = appRecord.appName,
                                                    usageMinutes = appRecord.usageMinutes,
                                                    category = newCategory
                                                )
                                                if (isDistraction) {
                                                    Toast.makeText(context, "🚫 ${appRecord.appName} configured as Distraction (automatically locks when curfew schedules are active)!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "✅ ${appRecord.appName} configured as Productive.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.testTag("app_instant_block_switch_${appRecord.appName}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Habit Simulator hidden from end users — dev tool only
                    // (showAddUsageDialog state is still available via ViewModel for testing)
                }
            }
            2 -> {
                // ALL APP CLASSIFIER PANEL FOR HABITS WITH FULL APP CATALOG & SEARCH
                val deviceApps = remember {
                    kotlin.runCatching {
                        val pm = context.packageManager
                        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                        }
                        pm.queryIntentActivities(intent, 0).map { info ->
                            info.loadLabel(pm).toString()
                        }.filter { it.isNotBlank() }.distinct()
                    }.getOrDefault(emptyList())
                }

                val mergedAllApps = remember(deviceApps) {
                    if (deviceApps.isEmpty()) {
                        listOf(
                            "TikTok", "YouTube", "Instagram", "Facebook", "Twitter", "Slack",
                            "Notion", "WhatsApp", "Spotify", "Chrome", "Gmail", "Netflix", "Reddit"
                        )
                    } else {
                        (listOf("TikTok", "YouTube", "Instagram", "Facebook", "Twitter", "Slack", "Notion", "WhatsApp", "Spotify", "Netflix", "Reddit") + deviceApps)
                            .distinct()
                            .sortedBy { it.lowercase() }
                    }
                }

                var classifierSearchQuery by remember { mutableStateOf("") }
                
                val filteredApps = remember(mergedAllApps, classifierSearchQuery) {
                    if (classifierSearchQuery.isBlank()) {
                        mergedAllApps
                    } else {
                        mergedAllApps.filter { it.contains(classifierSearchQuery, ignoreCase = true) }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    // Purpose Card for classification
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🧠 Select & Categorize Distractions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Set any app below as a Distraction ⚠️ to lock it completely during focus hours, or Productive ✅ to exempt it. FocusFlow shields active locks immediately.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                     // SEARCH FILTER FIELD
                    OutlinedTextField(
                        value = classifierSearchQuery,
                        onValueChange = { classifierSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        placeholder = { Text("Search phone apps to classify (e.g., YouTube)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // CLASSIFIED HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Focus Classification Manifest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.clearAllUsages() },
                            modifier = Modifier.testTag("app_manager_clear_all")
                        ) {
                            Text("Reset All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No apps match \"$classifierSearchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredApps.forEach { appItem ->
                                val existingRecord = usages.firstOrNull { it.appName.equals(appItem, ignoreCase = true) }
                                val currentCategory = existingRecord?.category // "Distraction", "Productive", or null

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("app_manager_item_${appItem}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (currentCategory == "Distraction") Color(0xFFFFEBEE).copy(alpha = 0.25f)
                                                         else if (currentCategory == "Productive") Color(0xFFE8F5E9).copy(alpha = 0.25f)
                                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AppLogoIcon(appName = appItem, isProductive = currentCategory == "Productive")
                                            Column {
                                                Text(appItem, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    text = when (currentCategory) {
                                                        "Distraction" -> "Distraction App ⚠️ (Blocked)"
                                                        "Productive" -> "Productive App ✅ (Allowed)"
                                                        else -> "Unclassified ⚪ (Active monitor)"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (currentCategory) {
                                                        "Distraction" -> Color(0xFFE53935)
                                                        "Productive" -> Color(0xFF2EBD6B)
                                                        else -> MaterialTheme.colorScheme.outline
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        val activeLockSchedule = schedules.firstOrNull { it.appName.equals(appItem, ignoreCase = true) }
                                        val isBlocked = activeLockSchedule?.isLocked == true

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                // Distraction Button
                                                Button(
                                                    onClick = {
                                                        viewModel.insertUsageRecord(
                                                            appName = appItem,
                                                            usageMinutes = existingRecord?.usageMinutes ?: 0,
                                                            category = "Distraction"
                                                        )
                                                        Toast.makeText(context, "⚠️ $appItem set as Distraction!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (currentCategory == "Distraction") Color(0xFFE53935) else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(
                                                        text = "Distraction ⚠️",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (currentCategory == "Distraction") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // Productive Button
                                                Button(
                                                    onClick = {
                                                        viewModel.insertUsageRecord(
                                                            appName = appItem,
                                                            usageMinutes = existingRecord?.usageMinutes ?: 0,
                                                            category = "Productive"
                                                        )
                                                        Toast.makeText(context, "✅ $appItem set as Productive!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (currentCategory == "Productive") Color(0xFF2EBD6B) else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(
                                                        text = "Productive ✅",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (currentCategory == "Productive") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Block App layout Switch (at the bottom of the buttons)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Block App",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isBlocked) Color(0xFFE53935) else MaterialTheme.colorScheme.outline,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Switch(
                                                    checked = isBlocked,
                                                    onCheckedChange = { active ->
                                                        if (active) {
                                                            viewModel.addLockSchedule(
                                                                appName = appItem,
                                                                startTime = "22:00",
                                                                endTime = "07:00",
                                                                days = "Daily",
                                                                todo = "Wind down screen-free: offline paper book, light stretching, deep breaths.",
                                                                smsMsg = "FocusFlow Block alert: $appItem curfew lock active!"
                                                            )
                                                            Toast.makeText(context, "🚫 App lock active for $appItem!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            schedules.filter { it.appName.equals(appItem, ignoreCase = true) }.forEach {
                                                                viewModel.deleteSchedule(it.id)
                                                            }
                                                            Toast.makeText(context, "✅ App lock disabled for $appItem.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("app_manager_block_switch_${appItem}")
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
        }
    }

    // --- PREMIUM OVERLAY CARD: SOUNDS & EFFECTS CUSTOMIZER ---
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
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Sounds & Effects",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    // Sound effect enabled switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                                    tint = if (soundEffectsOn) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Sound effect",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Switch(
                                checked = soundEffectsOn,
                                onCheckedChange = { viewModel.toggleSoundEffects(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2196F3)
                                )
                            )
                        }
                    }

                    // Sound choices list matches user screenshot
                    if (soundEffectsOn) {
                        val soundList = listOf(
                            "Bamboo Chime 🎋" to 1,
                            "Zen Temple Gong 🔔" to 3,
                            "Sleeping Kitty Flute 🍃" to 5,
                            "Quiet Mountain Spring 🌊" to 7,
                            "Singing Bowl Chime 🍵" to 9
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                soundList.forEach { soundPair ->
                                    val isSelected = selectedSoundName == soundPair.first
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
                                                        color = if (isSelected) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline,
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
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                        Text(
                                            text = "${soundPair.second}s",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (soundPair != soundList.last()) {
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(soundVolume * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newVol = (soundVolume - 0.1f).coerceIn(0.0f, 1.0f)
                                    viewModel.updateSoundVolume(newVol)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }

                            Slider(
                                value = soundVolume,
                                onValueChange = { viewModel.updateSoundVolume(it) },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFF2196F3),
                                    thumbColor = Color(0xFF2196F3)
                                )
                            )

                            IconButton(
                                onClick = {
                                    val newVol = (soundVolume + 0.1f).coerceIn(0.0f, 1.0f)
                                    viewModel.updateSoundVolume(newVol)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }

                    // Vibration Switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                                    tint = if (vibrationEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Vibration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Switch(
                                checked = vibrationEnabled,
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
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }

    // --- PREMIUM OVERLAY CARD: NOTIFICATION LOGS ---
    if (showNotificationLogsSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationLogsSheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Log History",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Notification Logs",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    // Monitor active log tracker switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "List icon",
                                    tint = if (logsTrackingEnabled) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Interactive logging",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Switch(
                                checked = logsTrackingEnabled,
                                onCheckedChange = { logsTrackingEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2196F3)
                                )
                            )
                        }
                    }

                    // Notification log item rows
                    if (logsTrackingEnabled) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            if (notificationLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No notification logs captured yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(modifier = Modifier.padding(6.dp)) {
                                    items(notificationLogs) { log ->
                                        val indicatorColor = when (log.category) {
                                            "Curfew" -> Color(0xFFFF5252)
                                            "Streak" -> Color(0xFF4CAF50)
                                            "Dopamine" -> Color(0xFF2196F3)
                                            else -> Color(0xFFFFC107)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Indicator Circle
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .size(8.dp)
                                                    .background(indicatorColor, CircleShape)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = log.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = log.message,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Active",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF2196F3),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .background(Color(0xFF2196F3).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Divider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Slider representation for Alert Intensity
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Logging Interval limit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (alertFrequencyLevel.toInt()) {
                                    1 -> "Low"
                                    2 -> "Moderate"
                                    3 -> "Standard"
                                    4 -> "Aggressive"
                                    else -> "Extreme ⚡"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (alertFrequencyLevel > 1) viewModel.updateAlertFrequencyLevel(alertFrequencyLevel - 1f)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }

                            Slider(
                                value = alertFrequencyLevel,
                                onValueChange = { viewModel.updateAlertFrequencyLevel(it) },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFF2196F3),
                                    thumbColor = Color(0xFF2196F3)
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (alertFrequencyLevel < 5) viewModel.updateAlertFrequencyLevel(alertFrequencyLevel + 1f)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationLogsSheet = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }

    // SIMULATED HARNESS USAGE INJECT DIALOGUE
    if (showAddUsageDialog) {
        var minutesToInject by remember { mutableFloatStateOf(45f) }
        var appNameSelection by remember { mutableStateOf("TikTok") }
        var isProductiveChoice by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddUsageDialog = false },
            title = { Text("🔌 Inject Screentime Scroll", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Test how FocusFlow handles limit spikes dynamically by pushing usages past your daily target.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = appNameSelection,
                        onValueChange = { appNameSelection = it },
                        modifier = Modifier.fillMaxWidth().testTag("inject_app_textfield"),
                        label = { Text("App Name") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Duration: ${minutesToInject.toInt()} mins", fontWeight = FontWeight.Bold)
                    Slider(
                        value = minutesToInject,
                        onValueChange = { minutesToInject = it },
                        valueRange = 5f..150f,
                        steps = 29
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !isProductiveChoice, onClick = { isProductiveChoice = false })
                        Text("Distraction Scroll", modifier = Modifier.padding(end = 16.dp))
                        RadioButton(selected = isProductiveChoice, onClick = { isProductiveChoice = true })
                        Text("Productive Work")
                    }
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("inject_confirm_button"),
                    onClick = {
                        viewModel.insertUsageRecord(
                            appName = appNameSelection,
                            usageMinutes = minutesToInject.toInt(),
                            category = if (isProductiveChoice) "Productive" else "Distraction"
                        )
                        showAddUsageDialog = false
                    }
                ) {
                    Text("Inject", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUsageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // FOCUS PREFERENCES / SETTINGS DIALOG
    if (showSettingsDialog) {
        var tempGoal by remember { mutableStateOf(dailyScreentimeGoalMinutes.toString()) }
        var tempPrevTime by remember { mutableStateOf(previousScreentimeMinutes.toString()) }

        LaunchedEffect(dailyScreentimeGoalMinutes) {
            tempGoal = dailyScreentimeGoalMinutes.toString()
        }
        LaunchedEffect(previousScreentimeMinutes) {
            tempPrevTime = previousScreentimeMinutes.toString()
        }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Focus Preferences ⚙️", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "OPTION WIDGET LAYOUT STYLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure how screen metrics display on your phone notification bar and homescreen widget.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Ratio Choice 1: Goal Progress
                        Card(
                            onClick = { viewModel.updateWidgetDisplayOption("goal") },
                            modifier = Modifier.fillMaxWidth().testTag("widget_option_goal"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (widgetDisplayOption == "goal") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (widgetDisplayOption == "goal") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = widgetDisplayOption == "goal", onClick = { viewModel.updateWidgetDisplayOption("goal") })
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Goal screen time progression", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Show current spent time bar out of the onboarding goal.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Ratio Choice 2: Today vs Yesterday Comparison
                        Card(
                            onClick = { viewModel.updateWidgetDisplayOption("comparison") },
                            modifier = Modifier.fillMaxWidth().testTag("widget_option_comparison"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (widgetDisplayOption == "comparison") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (widgetDisplayOption == "comparison") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = widgetDisplayOption == "comparison", onClick = { viewModel.updateWidgetDisplayOption("comparison") })
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Comparison comparison metric", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Show Today's performance vs yesterday average metrics.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CUSTOMIZE METRICS BASES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Daily Goal minutes field
                        OutlinedTextField(
                            value = tempGoal,
                            onValueChange = { 
                                tempGoal = it
                                it.toIntOrNull()?.let { minutes ->
                                    if (minutes in 10..480) viewModel.updateDailyScreentimeGoal(minutes)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("settings_goal_input"),
                            label = { Text("Daily Screentime Goal (minutes)") },
                            trailingIcon = { Text("min") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Yesterday average minutes field
                        OutlinedTextField(
                            value = tempPrevTime,
                            onValueChange = { 
                                tempPrevTime = it
                                it.toIntOrNull()?.let { minutes ->
                                    if (minutes in 0..480) viewModel.updatePreviousScreentime(minutes)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("settings_yesterday_input"),
                            label = { Text("Yesterday Screentime Baseline (minutes)") },
                            trailingIcon = { Text("min") }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ONBOARDING SURVEY RETAKE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Relaunch the coaching setup to recalculate base digital limits based on user roles and bedtime schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showSettingsDialog = false
                                viewModel.restartTutorial()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("settings_reset_survey_button")
                        ) {
                            Text("Reset & Rerun Survey 🎯", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    modifier = Modifier.testTag("settings_confirm_button")
                ) {
                    Text("Apply & Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Permission Guide pop-up Suggestion UI
    if (showPermissionGuideDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionGuideDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Shield Guard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("FocusFlow Setup Suggestions 🛡️", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "To securely lock distraction applications and maintain your streaks, Android OS requires two essential configurations:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "1. Accessibility Service Overlay ⚙️",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Go to 'Settings > Accessibility > FocusFlow' and activate the service. This allows FocusFlow to intercept high-scroll distraction screens and construct lock intervention shields during curfew schedules.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "2. Device Administrator Protocol 🛡️",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Accept when prompted by Android to enable device administrator permissions. This protects curfew locks from easy deletion and strengthens habit redirection triggers.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionGuideDialog = false
                        viewModel.setAccessibilityPermissionGranted(true)
                        viewModel.setUsageAccessPermissionGranted(true)
                        Toast.makeText(context, "✨ Permissions Auto-Granted (Simulation Approved)!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.testTag("pop_grant_perms")
                ) {
                    Text("Grant / Allow ✨", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPermissionGuideDialog = false },
                    modifier = Modifier.testTag("pop_dismiss_guide")
                ) {
                    Text("Decline / Close")
                }
            }
        )
    }
}

@Composable
fun AppLogoIcon(
    appName: String,
    modifier: Modifier = Modifier,
    isProductive: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var appIconDrawable by remember(appName) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    
    LaunchedEffect(appName) {
        kotlin.runCatching {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            val matchedApp = apps.firstOrNull { app ->
                val label = pm.getApplicationLabel(app).toString()
                label.equals(appName, ignoreCase = true) || label.contains(appName, ignoreCase = true)
            }
            if (matchedApp != null) {
                appIconDrawable = pm.getApplicationIcon(matchedApp)
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (isProductive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
        contentAlignment = Alignment.Center
    ) {
        if (appIconDrawable != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(appIconDrawable)
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(appIconDrawable)
                },
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        } else {
            val logoColor = when (appName.lowercase()) {
                "tiktok" -> Color(0xFF000000)
                "instagram", "ig" -> Color(0xFFE1306C)
                "youtube", "yt" -> Color(0xFFFF0000)
                "facebook", "fb" -> Color(0xFF1877F2)
                "twitter", "x" -> Color(0xFF000000)
                "chrome" -> Color(0xFF4285F4)
                "slack" -> Color(0xFF4A154B)
                "notion" -> Color(0xFF111111)
                "gmail" -> Color(0xFFEA4335)
                "whatsapp" -> Color(0xFF25D366)
                "spotify" -> Color(0xFF1DB954)
                else -> if (isProductive) Color(0xFF24A25A) else Color(0xFFD63C3C)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(logoColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

