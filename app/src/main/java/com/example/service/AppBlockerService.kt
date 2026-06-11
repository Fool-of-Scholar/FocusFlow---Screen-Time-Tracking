package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.data.database.FocusDatabase
import com.example.ui.screens.BlockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class AppBlockerService : AccessibilityService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var lastBlockedApp: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info
        Log.d("AppBlockerService", "Service Connected")
    }

    private var currentForegroundApp: String? = null
    private var appStartTime: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            val pm = packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
            
            val isFocusFlowOrSystem = packageName == applicationContext.packageName || 
                packageName == "com.example" || 
                packageName.contains("systemui", ignoreCase = true)

            // Dynamic Fast Update logic:
            val currentTime = System.currentTimeMillis()
            
            // If the user went back to FocusFlow or system UI, close the previous app's log instantly!
            if (isFocusFlowOrSystem) {
                if (currentForegroundApp != null) {
                    val durationMs = currentTime - appStartTime
                    val durationMinutes = if (durationMs > 2000) kotlin.math.max(1, kotlin.math.ceil(durationMs / 60000.0).toInt()) else 0
                    if (durationMinutes > 0) {
                        saveUsageToDatabase(currentForegroundApp!!, durationMinutes, currentTime)
                    }
                    currentForegroundApp = null
                }
                lastBlockedApp = null
                return // Stop here, we don't log FocusFlow itself
            }

            // System App Tracker Filter (blacklist + launchable check)
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val isLaunchable = launchIntent != null
            val isLauncher = try {
                val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply { addCategory(android.content.Intent.CATEGORY_HOME) }
                val resolveInfo = pm.resolveActivity(homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                resolveInfo?.activityInfo?.packageName == packageName
            } catch (e: Exception) { false }

            val knownSystemApps = listOf("smart panel", "zero screen", "app update", "app lock", "gboard", "digital wellbeing", "settings", "permission controller", "hios launcher", "launcher")
            val isKnownSystem = knownSystemApps.any { appName.lowercase().contains(it) }

            if (!isLaunchable || isLauncher || isKnownSystem) {
                lastBlockedApp = null
                return // Skip logging this system layer
            }

            // Real-Time Logging Logic
            if (currentForegroundApp != null && currentForegroundApp != appName) {
                val durationMs = currentTime - appStartTime
                val durationMinutes = if (durationMs > 2000) kotlin.math.max(1, kotlin.math.ceil(durationMs / 60000.0).toInt()) else 0
                if (durationMinutes > 0) {
                    saveUsageToDatabase(currentForegroundApp!!, durationMinutes, currentTime)
                }
            }

            if (appName != lastBlockedApp) {
                lastBlockedApp = null
            }

            currentForegroundApp = appName
            appStartTime = currentTime

            checkIfAppIsBlocked(appName)
        }
    }

    private fun saveUsageToDatabase(appToLog: String, durationMinutes: Int, currentTime: Long) {
        serviceScope.launch {
            val db = com.example.data.database.FocusDatabase.getDatabase(applicationContext)
            val existingUsages = db.focusDao().getAllUsagesFlow().firstOrNull() ?: emptyList()
            val existingRecord = existingUsages.firstOrNull { it.appName.equals(appToLog, ignoreCase = true) }
            
            if (existingRecord != null) {
                db.focusDao().updateUsage(existingRecord.copy(
                    usageMinutes = existingRecord.usageMinutes + durationMinutes,
                    timestamp = currentTime
                ))
            } else {
                db.focusDao().insertUsage(com.example.data.model.AppUsage(
                    appName = appToLog,
                    usageMinutes = durationMinutes,
                    category = "Productive", // Default
                    timestamp = currentTime
                ))
            }
        }
    }

    private fun checkIfAppIsBlocked(appName: String) {
        serviceScope.launch {
            try {
                // Check if Master Unlock (Bypass) is active
                val sharedPrefs = applicationContext.getSharedPreferences("focusflow_prefs_v5", android.content.Context.MODE_PRIVATE)
                val lockBypassEnabled = sharedPrefs.getBoolean("lock_bypass_enabled_v5", false)
                if (lockBypassEnabled) {
                    return@launch // Do not block if bypassed
                }

                val db = FocusDatabase.getDatabase(applicationContext)
                
                // 1. Is this app a Distraction?
                val usages = db.focusDao().getAllUsagesFlow().firstOrNull() ?: emptyList()
                val isDistraction = usages.any { 
                    (it.appName.equals(appName, ignoreCase = true) || appName.contains(it.appName, ignoreCase = true)) && 
                    it.category == "Distraction" 
                }

                // 2. Check Scheduled Curfews
                val schedules = db.focusDao().getAllSchedulesFlow().firstOrNull() ?: emptyList()
                
                var shouldBlock = false
                
                for (sched in schedules) {
                    if (!sched.isLocked) continue
                    if (!isTimeWithinSchedule(sched.startTime, sched.endTime)) continue
                    
                    val isManualLock = sched.todoWhileLocked == "Take a deep breath and step away from the screen."
                    
                    if (isManualLock) {
                        // Manual lock applies only to the specific app
                        if (sched.appName.equals(appName, ignoreCase = true) || appName.contains(sched.appName, ignoreCase = true)) {
                            shouldBlock = true
                            break
                        }
                    } else {
                        // Global schedule applies to ALL distractions
                        if (isDistraction) {
                            shouldBlock = true
                            break
                        }
                        // Also applies if they accidentally name the schedule exactly as the app name
                        if (sched.appName.equals(appName, ignoreCase = true) || appName.contains(sched.appName, ignoreCase = true)) {
                            shouldBlock = true
                            break
                        }
                    }
                }

                if (shouldBlock) {
                    showBlockOverlay(appName)
                }

            } catch (e: Exception) {
                Log.e("AppBlockerService", "Error checking block status", e)
            }
        }
    }

    private fun showBlockOverlay(appName: String) {
        if (appName == lastBlockedApp) return // Prevent infinite loop of launching overlay
        lastBlockedApp = appName
        
        Log.d("AppBlockerService", "Blocking app: $appName")
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            putExtra("BLOCKED_APP_NAME", appName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to launch lock screen due to Android 10+ background restrictions or missing Overlay permission.", e)
            lastBlockedApp = null // Reset so it can try again later if permission is granted
        }
    }

    private fun isTimeWithinSchedule(start: String, end: String): Boolean {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMin = calendar.get(Calendar.MINUTE)
            val currentTotalMins = currentHour * 60 + currentMin

            val partsStart = start.split(":")
            val startTotalMins = partsStart[0].toInt() * 60 + partsStart[1].toInt()

            val partsEnd = end.split(":")
            val endTotalMins = partsEnd[0].toInt() * 60 + partsEnd[1].toInt()

            return if (startTotalMins <= endTotalMins) {
                currentTotalMins in startTotalMins..endTotalMins
            } else {
                // Crosses midnight
                currentTotalMins >= startTotalMins || currentTotalMins <= endTotalMins
            }
        } catch (e: Exception) {
            return false
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
