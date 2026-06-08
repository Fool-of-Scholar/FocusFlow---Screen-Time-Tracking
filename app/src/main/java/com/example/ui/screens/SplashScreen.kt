package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.GalacticTeal
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }

    // Smooth progress simulation over 2.2 seconds
    LaunchedEffect(Unit) {
        val durationMs = 2200
        val steps = 100
        val delayPerStep = (durationMs / steps).toLong()
        for (i in 0..steps) {
            progress = i.toFloat() / steps
            delay(delayPerStep)
        }
        onFinished()
    }

    // Pulse animation for the glowing ring
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val ringGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onFinished() }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepMidnight,
                        DeepMidnight,
                        SlateCard,
                        DeepMidnight
                    )
                )
            )
            .systemBarsPadding()
    ) {
        // Subtle ambient concentric waves in background
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(340.dp)) {
                drawCircle(
                    color = GalacticTeal.copy(alpha = 0.05f),
                    radius = size.width * 0.45f,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = GalacticTeal.copy(alpha = 0.02f),
                    radius = size.width * 0.6f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Center Content Block (Emblem + Titles)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Visual Logo Container (copying the image layout of cup with hours ticker)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(SlateCard),
                contentAlignment = Alignment.Center
            ) {
                // Interactive glowing progress arc around mascot
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = GalacticTeal.copy(alpha = ringGlowAlpha * 0.25f),
                        style = Stroke(width = 10.dp.toPx())
                    )
                    drawArc(
                        color = GalacticTeal,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                // Mascot in the center
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cat_mascot_head_view),
                    contentDescription = "Cat Mascot Head",
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Main Space-Age spaced Bold Title
            Text(
                text = "FOCUSFLOW",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Premium supportive tag subtitle
            Text(
                text = "Reclaim your focus for a healthier mind",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        // Modern horizontal progress loader towards bottom of safe height (matches water tracker progress position)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Very thin minimalist loading bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = GalacticTeal,
                trackColor = SlateCard.copy(alpha = 0.6f)
            )

            Text(
                text = "tuning digital focus curfews... ${(progress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
