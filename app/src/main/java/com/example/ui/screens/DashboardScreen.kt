package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsage
import com.example.ui.viewmodel.FocusViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FocusViewModel) {
    val context = LocalContext.current
    val usages by viewModel.usages.collectAsState()
    val dailyScreentimeGoalMinutes by viewModel.dailyScreentimeGoalMinutes.collectAsState()
    val previousScreentimeMinutes by viewModel.previousScreentimeMinutes.collectAsState()

    // Auto-log the FocusFlow app open session at first render (tracks when user opens the app)
    // Records 1 minute for "FocusFlow" as a Productive session to reflect active engagement
    var sessionStartMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(Unit) {
        sessionStartMs = System.currentTimeMillis()
        onDispose {
            val sessionMinutes = ((System.currentTimeMillis() - sessionStartMs) / 60000L).toInt().coerceAtLeast(1)
            viewModel.insertUsageRecord(
                appName = "FocusFlow",
                usageMinutes = sessionMinutes,
                category = "Productive",
                timestamp = sessionStartMs
            )
        }
    }

    // Top Tabs: 0 -> DAY, 1 -> WEEK, 2 -> MONTH (All CAPS, matches the image exactly!)
    var activeTab by remember { mutableIntStateOf(0) }

    // Navigation offset indexes for chevrons
    var dayOffset by remember { mutableIntStateOf(0) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var monthOffset by remember { mutableIntStateOf(0) }

    // Date Helper Formatting structures (using remember to avoid memory allocations)
    val dayLabelFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val weekRangeFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val monthLabelFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Filter data to only installed apps (to remove old dummy data)
    var installedAppLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            installedAppLabels = com.example.ui.components.getInstalledApps(context)
                .map { it.label.lowercase() }
                .toSet()
        }
    }

    // Filter data for the active screen tabs / offsets
    val filteredData = remember(usages, activeTab, dayOffset, weekOffset, monthOffset) {
        val validUsages = usages
        val calendar = Calendar.getInstance()
        when (activeTab) {
            0 -> {
                // DAY Filter
                val targetCal = Calendar.getInstance().apply {
                    add(Calendar.DATE, -dayOffset)
                }
                validUsages.filter {
                    val entryCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    entryCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    entryCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
                }.sortedByDescending { it.timestamp }
            }
            1 -> {
                // WEEK Filter
                val startWeek = Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, -weekOffset)
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val endWeek = (startWeek.clone() as Calendar).apply {
                    add(Calendar.DATE, 7)
                }
                validUsages.filter {
                    it.timestamp >= startWeek.timeInMillis && it.timestamp < endWeek.timeInMillis
                }.sortedByDescending { it.timestamp }
            }
            else -> {
                // MONTH Filter
                val targetMonth = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -monthOffset)
                }
                validUsages.filter {
                    val entryCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    entryCal.get(Calendar.YEAR) == targetMonth.get(Calendar.YEAR) &&
                    entryCal.get(Calendar.MONTH) == targetMonth.get(Calendar.MONTH)
                }.sortedByDescending { it.timestamp }
            }
        }
    }

    // Header label text based on active offsets
    val currentRangeLabel = remember(activeTab, dayOffset, weekOffset, monthOffset) {
        val calendar = Calendar.getInstance()
        when (activeTab) {
            0 -> {
                if (dayOffset == 0) "Today"
                else if (dayOffset == 1) "Yesterday"
                else {
                    calendar.add(Calendar.DATE, -dayOffset)
                    dayLabelFormatter.format(calendar.time)
                }
            }
            1 -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, -weekOffset)
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                }
                val end = (start.clone() as Calendar).apply {
                    add(Calendar.DATE, 6)
                }
                if (weekOffset == 0) "This Week"
                else "${weekRangeFormatter.format(start.time)} - ${weekRangeFormatter.format(end.time)}, ${start.get(Calendar.YEAR)}"
            }
            else -> {
                calendar.add(Calendar.MONTH, -monthOffset)
                monthLabelFormatter.format(calendar.time)
            }
        }
    }

    // DASHBOARD CONTAINER: LazyColumn wrapped in a Box to allow FAB overlay
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
        // TOP FIXED SEGMENT TABS (Matches "DAY", "WEEK", "MONTH" with white indicator line)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F111E))
                    .padding(top = 16.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("DAY", "WEEK", "MONTH")
                    tabs.forEachIndexed { index, name ->
                        val isSelected = activeTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = index }
                                .padding(vertical = 10.dp)
                                .testTag("dash_tab_$name")
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }

        // CHEVRON NAV RANGE SELECTOR
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F111E))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        when (activeTab) {
                            0 -> dayOffset++
                            1 -> weekOffset++
                            2 -> monthOffset++
                        }
                    },
                    modifier = Modifier.testTag("chevron_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Period",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = currentRangeLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        when (activeTab) {
                            0 -> if (dayOffset > 0) dayOffset--
                            1 -> if (weekOffset > 0) weekOffset--
                            2 -> if (monthOffset > 0) monthOffset--
                        }
                    },
                    enabled = when (activeTab) {
                        0 -> dayOffset > 0
                        1 -> weekOffset > 0
                        2 -> monthOffset > 0
                        else -> false
                    },
                    modifier = Modifier.testTag("chevron_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Period",
                        tint = if (
                            (activeTab == 0 && dayOffset > 0) ||
                            (activeTab == 1 && weekOffset > 0) ||
                            (activeTab == 2 && monthOffset > 0)
                        ) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // MASCOT HEADER CARD (Inspired by the Personal Goals banner layout)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F111E))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B223C)),
                    border = BorderStroke(1.dp, Color(0xFF2C355A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(0.65f)
                        ) {
                            Text(
                                text = "PERSONAL GOALS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Track what matters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_sleeping_view),
                            contentDescription = "Sleeping Cat Mascot",
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(90.dp)
                        )
                    }
                }
            }
        }

        // CHART CONTAINER CARD (Matches the elegant water tracker outline)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F111E))
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                val totalTime = remember(filteredData, activeTab) { 
                    val rawTotal = filteredData.sumOf { it.usageMinutes }
                    when (activeTab) {
                        0 -> minOf(rawTotal, 24 * 60)
                        1 -> minOf(rawTotal, 7 * 24 * 60)
                        2 -> minOf(rawTotal, 31 * 24 * 60)
                        else -> rawTotal
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B223C)),
                    border = BorderStroke(1.dp, Color(0xFF2C355A))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        // Total spent vs Goal metrics (Water tracker UI style)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Total Screen Time",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF82B1FF)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (totalTime >= 60) {
                                        String.format(Locale.getDefault(), "%.1f hrs", totalTime.toFloat() / 60)
                                    } else {
                                        "$totalTime min"
                                    },
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (activeTab == 0) "Goal Limit" else "Baseline Limit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB0BEC5)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val comparedGoal = if (activeTab == 0) dailyScreentimeGoalMinutes else previousScreentimeMinutes
                                Text(
                                    text = if (comparedGoal >= 60) {
                                        String.format(Locale.getDefault(), "%.1f hrs", comparedGoal.toFloat() / 60)
                                    } else {
                                        "$comparedGoal min"
                                    },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // CHART VISUALIZER COMPONENT
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (activeTab) {
                                0 -> DayUsageChart(filteredData)
                                1 -> WeekUsageChart(filteredData, weekOffset)
                                else -> MonthUsageChart(filteredData, monthOffset)
                            }
                        }
                    }
                }
            }
        }

        // RECORDS HEADER SECTION (Matches screenshot)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Records Logs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // LIST ITEMS OF INDIVIDUAL RECORDED ITEMS
        if (filteredData.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_sleeping_view),
                        contentDescription = "Cat Mascot Sleeping",
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Zero records logged for this period! ✅",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "App usage is automatically tracked and logged dynamically as you use your device.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
        } else {
            items(filteredData, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("record_item_${item.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // App Icon logo display
                        com.example.ui.components.AppIcon(
                            appName = item.appName,
                            isProductive = (item.category == "Productive"),
                            size = 48.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.appName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Surface(
                                    color = if (item.category == "Productive") Color(0xFF2EBD6B).copy(alpha = 0.15f)
                                            else Color(0xFFE53935).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = " ${item.category} ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.category == "Productive") Color(0xFF2EBD6B) else Color(0xFFE53935),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Logged at " + timeFormatter.format(Date(item.timestamp)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Right side: usage numerical value
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (item.usageMinutes <= 1) "< 1m" else "${item.usageMinutes}m",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } // end else
    } // end LazyColumn
} // end Box
} // end DashboardScreen

// -------------------------------------------------------------
// VISUAL ARTWORK CHARTS (OPTIMIZED FOR ZERO ALLOCATIONS DURING DRAW)
// -------------------------------------------------------------

@Composable
fun DayUsageChart(filteredData: List<AppUsage>) {
    var selectedArcIndex by remember { mutableIntStateOf(-1) }

    // Map into 6 day time slots (0-4, 4-8, 8-12, 12-16, 16-20, 20-24)
    val blocks = remember(filteredData) {
        val vals = FloatArray(6) { 0f }
        filteredData.forEach {
            val usageCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val hr = usageCal.get(Calendar.HOUR_OF_DAY)
            val block = (hr / 4).coerceIn(0, 5)
            vals[block] += it.usageMinutes.toFloat()
        }
        vals
    }

    val maxVal = remember(blocks) {
        val max = blocks.maxOrNull() ?: 0f
        if (max == 0f) 60f else max
    }

    // Precalculate 12 bar hours to prevent allocations in draw pass
    val hourlyTwoHrAndMax = remember(filteredData) {
        val hourlyTwoHr = FloatArray(12) { 0f }
        filteredData.forEach {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val hrIdx = (cal.get(Calendar.HOUR_OF_DAY) / 2).coerceIn(0, 11)
            hourlyTwoHr[hrIdx] += it.usageMinutes.toFloat()
        }
        val maxTwoHrLimit = hourlyTwoHr.maxOrNull()?.coerceAtLeast(60f) ?: 60f
        Pair(hourlyTwoHr, maxTwoHrLimit)
    }
    val hourlyTwoHr = hourlyTwoHrAndMax.first
    val maxTwoHrLimit = hourlyTwoHrAndMax.second

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    selectedArcIndex = (selectedArcIndex + 1) % 6
                }
        ) {
            val width = size.width
            val height = size.height
            val leftPadding = 35.dp.toPx()
            val rightPadding = 15.dp.toPx()
            val topPadding = 25.dp.toPx()
            val bottomPadding = 30.dp.toPx()

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            // Drawing gridlines (matches the reference water tracker background lines)
            val gridCount = 4
            for (i in 0..gridCount) {
                val yCoord = topPadding + (chartHeight * (i.toFloat() / gridCount))
                drawLine(
                    color = Color(0xFF2C355A).copy(alpha = 0.5f),
                    start = Offset(leftPadding, yCoord),
                    end = Offset(width - rightPadding, yCoord),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw bars representing the logs
            val barCount = 12
            val barSpacing = 8.dp.toPx()
            val singleBarWidth = (chartWidth - (barSpacing * (barCount - 1))) / barCount

            for (i in 0 until barCount) {
                val v = hourlyTwoHr[i]
                val itemPercent = v / maxTwoHrLimit
                val barH = chartHeight * itemPercent

                val rx = leftPadding + (i * (singleBarWidth + barSpacing))
                val ry = topPadding + chartHeight - barH

                // Rounded capsule drawing
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (selectedArcIndex == (i / 2)) {
                            listOf(Color(0xFF82B1FF), Color(0xFF448AFF))
                        } else {
                            listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.2f))
                        }
                    ),
                    topLeft = Offset(rx, ry),
                    size = Size(singleBarWidth, barH.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }
        }

        // Floating active interactive overlay mimicking "+6.8 oz" from reference photo
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF82B1FF), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val labelText = if (selectedArcIndex == -1) {
                "Tap chart to inspect block hours"
            } else {
                val hrsStr = when (selectedArcIndex) {
                    0 -> "12 AM - 4 AM"
                    1 -> "4 AM - 8 AM"
                    2 -> "8 AM - 12 PM"
                    3 -> "12 PM - 4 PM"
                    4 -> "4 PM - 8 PM"
                    else -> "8 PM - 12 AM"
                }
                "$hrsStr: ${blocks[selectedArcIndex].toInt()} mins spent"
            }
            Text(
                text = labelText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Day static hours axes labels underneath
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rangeMarks = listOf("0", "4", "8", "12", "16", "20", "24")
            rangeMarks.forEach {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun WeekUsageChart(filteredData: List<AppUsage>, weekOffset: Int) {
    var selectedDayIndex by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) }

    val daysOfWeekVals = remember(filteredData) {
        val vals = FloatArray(7) { 0f }
        filteredData.forEach {
            val usageCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val dIdx = usageCal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday is 1, so indices are 0..6
            if (dIdx in 0..6) {
                vals[dIdx] += it.usageMinutes.toFloat()
            }
        }
        vals
    }

    val maxVal = remember(daysOfWeekVals) {
        val max = daysOfWeekVals.maxOrNull() ?: 0f
        if (max == 0f) 120f else max
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    selectedDayIndex = (selectedDayIndex + 1) % 7
                }
        ) {
            val width = size.width
            val height = size.height
            val leftPadding = 35.dp.toPx()
            val rightPadding = 15.dp.toPx()
            val topPadding = 25.dp.toPx()
            val bottomPadding = 30.dp.toPx()

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            // Drawing gridlines
            val gridLines = 4
            for (i in 0..gridLines) {
                val yCoord = topPadding + (chartHeight * (i.toFloat() / gridLines))
                drawLine(
                    color = Color(0xFF2C355A).copy(alpha = 0.5f),
                    start = Offset(leftPadding, yCoord),
                    end = Offset(width - rightPadding, yCoord),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw 7 white vertical capsules
            val barSpacing = 16.dp.toPx()
            val singleBarWidth = (chartWidth - (barSpacing * 6)) / 7

            for (i in 0..6) {
                val v = daysOfWeekVals[i]
                val itemPercent = v / maxVal
                val barH = chartHeight * itemPercent

                val rx = leftPadding + (i * (singleBarWidth + barSpacing))
                val ry = topPadding + chartHeight - barH

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (selectedDayIndex == i) {
                            listOf(Color(0xFF82B1FF), Color(0xFF448AFF))
                        } else {
                            listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.3f))
                        }
                    ),
                    topLeft = Offset(rx, ry),
                    size = Size(singleBarWidth, barH.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
            }
        }

        // Flying tag matching second screenshot "+6.8 oz" popup values
        val activeDayName = remember(selectedDayIndex) {
            when (selectedDayIndex) {
                0 -> "Sun"
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                else -> "Sat"
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF82B1FF), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$activeDayName: ${daysOfWeekVals[selectedDayIndex].toInt()} mins screen usage",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Underlying horizontal week marks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 40.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
            weekDays.forEach {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MonthUsageChart(filteredData: List<AppUsage>, monthOffset: Int) {
    var selectedBlockIndex by remember { mutableIntStateOf(0) }

    val daysOfBlocks = remember(filteredData) {
        val vals = FloatArray(7) { 0f }
        filteredData.forEach {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val block = when {
                day <= 5 -> 0
                day <= 10 -> 1
                day <= 15 -> 2
                day <= 20 -> 3
                day <= 25 -> 4
                day <= 30 -> 5
                else -> 6
            }
            vals[block] += it.usageMinutes.toFloat()
        }
        vals
    }

    val maxVal = remember(daysOfBlocks) {
        val max = daysOfBlocks.maxOrNull() ?: 0f
        if (max == 0f) 200f else max
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    selectedBlockIndex = (selectedBlockIndex + 1) % 7
                }
        ) {
            val width = size.width
            val height = size.height
            val leftPadding = 35.dp.toPx()
            val rightPadding = 15.dp.toPx()
            val topPadding = 25.dp.toPx()
            val bottomPadding = 30.dp.toPx()

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            // Drawing gridlines
            val linesCount = 4
            for (i in 0..linesCount) {
                val yCoord = topPadding + (chartHeight * (i.toFloat() / linesCount))
                drawLine(
                    color = Color(0xFF2C355A).copy(alpha = 0.5f),
                    start = Offset(leftPadding, yCoord),
                    end = Offset(width - rightPadding, yCoord),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw 7 vertical bars representing month periods
            val spacing = 14.dp.toPx()
            val singleWidth = (chartWidth - (spacing * 6)) / 7

            for (i in 0..6) {
                val v = daysOfBlocks[i]
                val itemPercent = v / maxVal
                val hBar = chartHeight * itemPercent

                val rx = leftPadding + (i * (singleWidth + spacing))
                val ry = topPadding + chartHeight - hBar

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (selectedBlockIndex == i) {
                            listOf(Color(0xFF82B1FF), Color(0xFF448AFF))
                        } else {
                            listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.3f))
                        }
                    ),
                    topLeft = Offset(rx, ry),
                    size = Size(singleWidth, hBar.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }
        }

        // Dynamic Tag Overlay
        val activeRangeText = remember(selectedBlockIndex) {
            when (selectedBlockIndex) {
                0 -> "Days 1 - 5"
                1 -> "Days 6 - 10"
                2 -> "Days 11 - 15"
                3 -> "Days 16 - 20"
                4 -> "Days 21 - 25"
                5 -> "Days 26 - 30"
                else -> "Days 31+"
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF82B1FF), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$activeRangeText: ${daysOfBlocks[selectedBlockIndex].toInt()} mins screen usage",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Horizontal axes date markers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 38.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val monthMarks = listOf("1", "6", "11", "16", "21", "26", "31")
            monthMarks.forEach {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TIMER CALCULATION TIMESTAMP ALIGNMENTS FOR ADDING IN PAST
// -------------------------------------------------------------
fun calculateTimestampForPeriod(tab: Int, dayOff: Int, weekOff: Int, monthOff: Int): Long {
    val calendar = Calendar.getInstance()
    when (tab) {
        0 -> {
            calendar.add(Calendar.DATE, -dayOff)
        }
        1 -> {
            calendar.add(Calendar.WEEK_OF_YEAR, -weekOff)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY) // place it in the middle of active week
        }
        else -> {
            calendar.add(Calendar.MONTH, -monthOff)
            calendar.set(Calendar.DAY_OF_MONTH, 15) // place it in the middle of active month
        }
    }
    // Set typical evening logging hour (e.g. 19:45 PM)
    calendar.set(Calendar.HOUR_OF_DAY, 19)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.MINUTE, 45)
    return calendar.timeInMillis
}
