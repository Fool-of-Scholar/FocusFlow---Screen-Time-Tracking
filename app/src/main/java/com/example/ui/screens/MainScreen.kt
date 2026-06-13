package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.viewmodel.FocusViewModel

@Composable
fun MainScreen(viewModel: FocusViewModel) {
    var showSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    val showTutorial by viewModel.showTutorial.collectAsState()
    var tutorialStep by remember { mutableIntStateOf(0) }

    // Onboarding values
    var onboardingRole by remember { mutableStateOf("Academic 🎓") }
    var onboardingStruggle by remember { mutableStateOf("Social Media Scrolling 📱") }
    var onboardingCurrentScreentime by remember { mutableStateOf("Moderate (3-5 hours) 🟡") }
    var onboardingExerciseLevel by remember { mutableStateOf("Sometimes exercise 💦") }
    var onboardingSleepTimeByHour by remember { mutableStateOf("23:00") }
    var onboardingAdjustedMinutes by remember { mutableIntStateOf(150) }
    var onboardingGoalUnitByHours by remember { mutableStateOf(false) } // false = min, true = hr

    // App Navigation Pages
    var activePageIndex by remember { mutableIntStateOf(0) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
            if (!showTutorial) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple("Home", Icons.Default.Home, "home_nav_tab"),
                        Triple("Dashboard", Icons.Default.Info, "dashboard_nav_tab"),
                        Triple("Schedules", Icons.Default.Lock, "schedules_nav_tab"),
                        Triple("Timeline", Icons.Default.DateRange, "timeline_nav_tab"),
                        Triple("Me", Icons.Default.Person, "me_nav_tab")
                    )
                    items.forEachIndexed { index, pair ->
                        NavigationBarItem(
                            modifier = Modifier.testTag(pair.third),
                            icon = { Icon(pair.second, contentDescription = pair.first) },
                            label = { Text(pair.first, style = MaterialTheme.typography.labelMedium) },
                            selected = activePageIndex == index,
                            onClick = { activePageIndex = index }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Talk to Kitty FAB is hidden — AI analysis feature planned for a future release.
            // To re-enable, uncomment the block below:
            // if (!showTutorial) {
            //     ExtendedFloatingActionButton(
            //         modifier = Modifier.testTag("talk_to_kitty_fab"),
            //         onClick = { showChatDialog = true },
            //         icon = { Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) },
            //         text = { Text("Talk to Kitty", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
            //         containerColor = MaterialTheme.colorScheme.primary
            //     )
            // }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Screen router
            if (!showTutorial) {
                when (activePageIndex) {
                    0 -> HomeScreen(viewModel = viewModel, onNavigateToMe = { activePageIndex = 4 })
                    1 -> DashboardScreen(viewModel = viewModel)
                    2 -> LockSchedulesScreen(viewModel = viewModel)
                    3 -> HabitsCalendarScreen(viewModel = viewModel)
                    4 -> MeScreen(viewModel = viewModel, onNavigateToTab = { activePageIndex = it })
                }
            }

            // Onboarding Questionnaire Wizard Overlays
            if (showTutorial) {
                val calculatedMinutes = remember(onboardingRole, onboardingStruggle, onboardingCurrentScreentime, onboardingExerciseLevel) {
                    var base = when (onboardingCurrentScreentime) {
                        "Mild Scrolls (<3 hours) 🟢" -> 110
                        "Moderate (3-5 hours) 🟡" -> 170
                        "Heavy (5-8 hours) 🟠" -> 230
                        else -> 290
                    }
                    base = (base * 0.75f).toInt() // targeted reductions

                    if (onboardingRole.contains("Professional")) base += 30
                    if (onboardingRole.contains("Academic")) base -= 15
                    if (onboardingExerciseLevel.contains("Often") || onboardingExerciseLevel.contains("Regularly")) base += 15

                    base.coerceIn(60, 360)
                }

                LaunchedEffect(calculatedMinutes) {
                    onboardingAdjustedMinutes = calculatedMinutes
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // PROGRESS TOP BAR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (tutorialStep > 0) {
                                IconButton(
                                    modifier = Modifier.testTag("onboarding_back"),
                                    onClick = { tutorialStep-- }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Navigate back",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(48.dp))
                            }

                            Column(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val targetProgress = (tutorialStep + 1) / 7.0f
                                LinearProgressIndicator(
                                    progress = { targetProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Coach Check: ${((targetProgress) * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            if (tutorialStep < 6) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        modifier = Modifier.testTag("onboarding_skip"),
                                        onClick = {
                                            viewModel.saveOnboardingSelections(
                                                role = onboardingRole,
                                                struggle = onboardingStruggle,
                                                currentUsage = onboardingCurrentScreentime,
                                                exercise = onboardingExerciseLevel,
                                                sleepTime = onboardingSleepTimeByHour,
                                                calculatedGoalMinutes = onboardingAdjustedMinutes
                                            )
                                            showHowItWorksDialog = true
                                        }
                                    ) {
                                        Text(
                                            "Skip ↗️",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    TextButton(
                                        modifier = Modifier.testTag("onboarding_next"),
                                        onClick = { tutorialStep++ }
                                    ) {
                                        Text(
                                            "Next",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(48.dp))
                            }
                        }

                        // MASCOT SPEECH BUBBLE
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val expression = when (tutorialStep) {
                                    0, 1 -> "happy"
                                    2 -> "sad"
                                    3, 4 -> "focused"
                                    5 -> "sleepy"
                                    else -> "happy"
                                }
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                                    contentDescription = "Cat Mascot Head",
                                    modifier = Modifier.size(72.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    val tipText = when (tutorialStep) {
                                        0 -> "Welcome to FocusFlow! Let's tailor an achievable daily screen allowance to recover offline clarity."
                                        1 -> "Your active role lets us coordinate ideal lock ranges. Work demands deep bounds, and play calls for limits."
                                        2 -> "Choose your biggest distraction. FocusFlow uses 'Visual Curfews' to actively block these apps and issue warnings if opened during your Deep Work or Sleep routines!"
                                        3 -> "A higher score signals we should take a step-down focus pattern rather than cold turkey."
                                        4 -> "Moving or engaging in screen-free hobbies cuts down compulsive scrolling loops."
                                        5 -> "Sleep locks guard your circadian rest states. 1 hour of pre-sleep off-time unlocks 45% better rest!"
                                        else -> "I have configured your targeted habit blueprint! Fine tune your hours target below."
                                    }
                                    Text(
                                        text = tipText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // FORM SECTION
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            when (tutorialStep) {
                                0 -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Tailor Your Mental Focus\n& Screentime Target! 🐼",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Ditch endless algorithmic scrolling loops, set automatic lock curfews, and reclaim sensory wellness. Our active locks will instantly block access to Distraction apps during scheduled routines.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .testTag("welcome_start_button"),
                                            onClick = { tutorialStep = 1 },
                                            shape = RoundedCornerShape(28.dp)
                                        ) {
                                            Text(
                                                "Customize My Goal 🎯",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "What is your main daily role?",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val roles = listOf("Academic 🎓", "Professional / tech 💻", "Creative / freelancer 🎨", "General Mindfulness 🧘")
                                        roles.forEach { role ->
                                            val isSelected = onboardingRole == role
                                            Card(
                                                onClick = { onboardingRole = role },
                                                modifier = Modifier.fillMaxWidth().testTag("role_$role"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(selected = isSelected, onClick = { onboardingRole = role })
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(role, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "What is your hardest screen distraction?",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val struggles = listOf(
                                            "Social Media Scrolling 📱",
                                            "Video Streaming Bingeing 📺",
                                            "Work/Chat Distractions 💼",
                                            "Late Night Scrolling 🌙"
                                        )
                                        struggles.forEach { struggle ->
                                            val isSelected = onboardingStruggle == struggle
                                            Card(
                                                onClick = { onboardingStruggle = struggle },
                                                modifier = Modifier.fillMaxWidth().testTag("struggle_$struggle"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(struggle, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                                    if (isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Current estimated daily usage duration:",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val usages = listOf(
                                            "Mild Scrolls (<3 hours) 🟢",
                                            "Moderate (3-5 hours) 🟡",
                                            "Heavy (5-8 hours) 🟠",
                                            "Severe limit (8+ hours) 🔴"
                                        )
                                        usages.forEach { usage ->
                                            val isSelected = onboardingCurrentScreentime == usage
                                            Card(
                                                onClick = { onboardingCurrentScreentime = usage },
                                                modifier = Modifier.fillMaxWidth().testTag("usage_$usage"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(selected = isSelected, onClick = { onboardingCurrentScreentime = usage })
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(usage, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                                4 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "How much offline activity do you manage?",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val exercises = listOf(
                                            "Rarely exercise 🪑",
                                            "Sometimes exercise 💦",
                                            "Regularly exercise 💪",
                                            "Often exercise 🏋️"
                                        )
                                        exercises.forEach { exercise ->
                                            val isSelected = onboardingExerciseLevel == exercise
                                            Card(
                                                onClick = { onboardingExerciseLevel = exercise },
                                                modifier = Modifier.fillMaxWidth().testTag("exercise_$exercise"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(exercise, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                                5 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "When is your target bedtime?",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "I will pre-load an automatic bedtime lock 1 hour before this time.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        val bedtimeOptions = listOf("21:30", "22:00", "22:30", "23:00", "23:30", "00:00")
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(bedtimeOptions) { time ->
                                                val isSelected = onboardingSleepTimeByHour == time
                                                Card(
                                                    onClick = { onboardingSleepTimeByHour = time },
                                                    modifier = Modifier.fillMaxWidth().testTag("bedtime_$time"),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RadioButton(selected = isSelected, onClick = { onboardingSleepTimeByHour = time })
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("$time (Sleep curfew target)", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                6 -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Your Suggested Digital Blueprint",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // UNIT TABS (ml vs oz style)
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = " Minutes ",
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(if (!onboardingGoalUnitByHours) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    .clickable { onboardingGoalUnitByHours = false }
                                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (!onboardingGoalUnitByHours) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = " Hours ",
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(if (onboardingGoalUnitByHours) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    .clickable { onboardingGoalUnitByHours = true }
                                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (onboardingGoalUnitByHours) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val formattedValue = if (onboardingGoalUnitByHours) {
                                            "${(onboardingAdjustedMinutes / 60.0f * 10f).toInt() / 10f} hrs"
                                        } else {
                                            "$onboardingAdjustedMinutes mins"
                                        }

                                        Text(
                                            text = formattedValue,
                                            style = MaterialTheme.typography.displayLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // Adjustment buttons
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { if (onboardingAdjustedMinutes > 60) onboardingAdjustedMinutes -= 15 }
                                            ) {
                                                Text("-15m", fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                "FINE TUNE",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            OutlinedButton(
                                                onClick = { if (onboardingAdjustedMinutes < 360) onboardingAdjustedMinutes += 15 }
                                            ) {
                                                Text("+15m", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Details cards
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Limit: $onboardingAdjustedMinutes minutes of daily phone time",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Bedtime Lock: Automatic curfew pre-installed at bedtime.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom start button under step 6 blueprint
                        if (tutorialStep == 6) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("onboarding_complete_button"),
                                    onClick = {
                                        viewModel.saveOnboardingSelections(
                                            role = onboardingRole,
                                            struggle = onboardingStruggle,
                                            currentUsage = onboardingCurrentScreentime,
                                            exercise = onboardingExerciseLevel,
                                            sleepTime = onboardingSleepTimeByHour,
                                            calculatedGoalMinutes = onboardingAdjustedMinutes
                                        )
                                        showHowItWorksDialog = true
                                    },
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Text(
                                        "Activate Plan & Start Journey! 🚀",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHowItWorksDialog) {
        com.example.ui.components.HowItWorksDialog(onDismiss = { showHowItWorksDialog = false })
    }

    // Floating Chat conversation modal dialogue with Master Kitty
    if (showChatDialog) {
        val chatHistory by viewModel.chatMessages.collectAsState()
        var chatInputText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChatDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                        contentDescription = "Cat Mascot Head",
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Coach Master Kitty 🐱", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.sizeIn(maxHeight = 350.dp, maxWidth = 300.dp)) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatHistory) { msg ->
                            val bubbleColor = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            val align = if (msg.isUser) Alignment.End else Alignment.Start
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        modifier = Modifier.fillMaxWidth().testTag("chat_input_field"),
                        placeholder = { Text("Ask something...") },
                        maxLines = 2,
                        trailingIcon = {
                            IconButton(
                                modifier = Modifier.testTag("chat_send_button"),
                                onClick = {
                                    if (chatInputText.isNotBlank()) {
                                        viewModel.sendChatMessage(chatInputText)
                                        chatInputText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send text")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatDialog = false }) {
                    Text("Dismiss", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearChatHistory() }) {
                    Text("Clear Log", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    val showHowItWorks by viewModel.showHowItWorks.collectAsState()
    if (showHowItWorks) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowHowItWorks(false) },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                        contentDescription = "Cat Mascot Head",
                        modifier = Modifier.size(48.dp)
                    )
                    Text("How it Works 💡", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "FocusFlow helps you regain digital clarity and master self-discipline with these core steps:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("⏱️", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text("Daily Screen Budget", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text("We calculate and enforce an active daily screen time allowance customized to your daily role.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("🛡️", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text("Curfew Lockdowns", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Use the 'Schedules' tab to set locked windows (e.g. bedtime). Distraction apps will be completely blocked to preserve focus.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("🐱", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text("Coach Kitty", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Master Kitty checks your mental focus balance daily, logs streaking scores, and prompts offline exercises.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("📱", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text("Homescreen Widget", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Add the widget to keep your live minutes goal progression or shift comparison clearly visible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setShowHowItWorks(false) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("how_it_works_got_it")
                ) {
                    Text("Got it! 🚀", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
    }
}
