package com.example.ui.components

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Data model for an installed app
// ---------------------------------------------------------------------------
data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

// ---------------------------------------------------------------------------
// Global in-memory cache (per process) so we only load once
// ---------------------------------------------------------------------------
private var _cachedApps: List<InstalledAppInfo>? = null

suspend fun getInstalledApps(context: Context): List<InstalledAppInfo> {
    _cachedApps?.let { return it }
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val result = pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                try {
                    val appInfo: ApplicationInfo = resolveInfo.activityInfo.applicationInfo
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo.packageName)
                    InstalledAppInfo(label = label, packageName = appInfo.packageName, icon = icon)
                } catch (e: Exception) {
                    null
                }
            }
            .filter { it.label.isNotBlank() }
            .distinctBy { it.label.lowercase() }
            .sortedBy { it.label.lowercase() }
        _cachedApps = result
        result
    }
}

// ---------------------------------------------------------------------------
// Composable: AppIcon — shows real icon or colored initial fallback
// ---------------------------------------------------------------------------
@Composable
fun AppIcon(
    appName: String,
    packageName: String? = null,
    isProductive: Boolean = true,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName ?: appName) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(packageName ?: appName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val drawable: Drawable? = when {
                    packageName != null -> try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null }
                    else -> {
                        val apps = getInstalledApps(context)
                        apps.firstOrNull { it.label.equals(appName, ignoreCase = true) }?.icon
                            ?: apps.firstOrNull { it.label.contains(appName, ignoreCase = true) }?.icon
                    }
                }
                // Convert to bitmap on IO thread (no composable involved)
                iconBitmap = drawable?.toBitmap()
            } catch (_: Exception) {
                iconBitmap = null
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = iconBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "$appName icon",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AppIconFallback(appName, isProductive, size)
        }
    }
}

@Composable
private fun AppIconFallback(appName: String, isProductive: Boolean, size: Dp) {
    val bgColor = when (appName.lowercase().trim()) {
        "tiktok" -> Color(0xFF010101)
        "instagram" -> Color(0xFFE1306C)
        "youtube", "youtube music" -> Color(0xFFFF0000)
        "facebook", "fb" -> Color(0xFF1877F2)
        "twitter", "x" -> Color(0xFF1DA1F2)
        "chrome" -> Color(0xFF4285F4)
        "slack" -> Color(0xFF4A154B)
        "notion" -> Color(0xFF191919)
        "gmail" -> Color(0xFFEA4335)
        "whatsapp" -> Color(0xFF25D366)
        "spotify" -> Color(0xFF1DB954)
        "netflix" -> Color(0xFFE50914)
        "reddit" -> Color(0xFFFF4500)
        "discord" -> Color(0xFF5865F2)
        else -> if (isProductive) Color(0xFF1A6B3E) else Color(0xFF8B1F1F)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (appName.firstOrNull()?.uppercaseChar() ?: '?').toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}
