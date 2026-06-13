package com.example.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.utils.ThemePreferences
import androidx.compose.ui.platform.LocalLifecycleOwner

class BlockOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val blockedAppName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "App"
        val blockedPackageName = intent.getStringExtra("BLOCKED_PACKAGE_NAME") ?: ""

        // Fetch real app icon
        var appIcon: Drawable? = null
        if (blockedPackageName.isNotEmpty()) {
            try {
                appIcon = packageManager.getApplicationIcon(blockedPackageName)
            } catch (e: PackageManager.NameNotFoundException) {
                // Ignore
            }
        }

        setContent {
            MaterialTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                var bgPref by remember { mutableStateOf(ThemePreferences.getBackgroundColor(context)) }
                
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            bgPref = ThemePreferences.getBackgroundColor(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                BlockScreenContent(
                    blockedAppName = blockedAppName,
                    appIcon = appIcon,
                    bgPref = bgPref,
                    onGoHome = {
                        val intent = Intent(this@BlockOverlayActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onOpenTheme = {
                        startActivity(Intent(this@BlockOverlayActivity, ThemeSelectionActivity::class.java))
                    },
                    onOpenAnimation = {
                        startActivity(Intent(this@BlockOverlayActivity, AnimationSelectionActivity::class.java))
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Force recomposition by resetting content or state is handled by LaunchedEffect? 
        // In Compose, if we want to ensure it updates on resume, we might need a lifecycle observer, 
        // but for now relying on the standard state is okay.
    }
}

@Composable
fun BlockScreenContent(
    blockedAppName: String,
    appIcon: Drawable?,
    bgPref: String,
    onGoHome: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenAnimation: () -> Unit
) {
    var showReturnNavigation by remember { mutableStateOf(true) }
    val isUri = bgPref.startsWith("content://")

    // Parse background preference if it's a solid color or gradient
    val backgroundModifier = if (isUri) {
        Modifier // Handled by AsyncImage below
    } else if (bgPref.contains(",")) {
        val colors = bgPref.split(",")
        try {
            Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        Color(android.graphics.Color.parseColor(colors[0])),
                        Color(android.graphics.Color.parseColor(colors[1]))
                    )
                )
            )
        } catch (e: Exception) {
            Modifier.background(Color(0xFF1E1E2E))
        }
    } else {
        try {
            Modifier.background(Color(android.graphics.Color.parseColor(bgPref)))
        } catch (e: Exception) {
            Modifier.background(Color(0xFF1E1E2E))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(backgroundModifier)
    ) {
        if (isUri) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(bgPref)
                    .decoderFactory(ImageDecoderDecoder.Factory()) // Support GIFs
                    .build(),
                contentDescription = "Custom Lock Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp), // accommodate status bar
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Top Left: FocusFlow Return Navigation
            if (showReturnNavigation) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "FocusFlow",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onGoHome() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hide navigation",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showReturnNavigation = false }
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Top Right: Theme & Animation Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Theme (Image/Colors) Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onOpenTheme() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎨", fontSize = 20.sp)
                }
                
                // Animation Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onOpenAnimation() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 20.sp)
                }
            }
        }

        // CENTER CONTENT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
        ) {
            // Blocked App Icon
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.toBitmap().asImageBitmap(),
                    contentDescription = "Blocked App Icon",
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp))
                )
            } else {
                // Fallback to mascot
                Image(
                    painter = painterResource(id = com.example.R.drawable.cat_mascot_front_view),
                    contentDescription = "Fallback Icon",
                    modifier = Modifier.size(120.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "LOCKED",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$blockedAppName is currently inaccessible.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
