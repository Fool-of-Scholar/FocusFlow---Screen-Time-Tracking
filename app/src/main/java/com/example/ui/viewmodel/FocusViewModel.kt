package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FocusDatabase
import com.example.data.model.AppUsage
import com.example.data.model.AppLockSchedule
import com.example.data.model.FocusTimelineEntry
import com.example.data.model.MascotChatMessage
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.media.MediaPlayer
import kotlinx.coroutines.delay
import android.os.Vibrator
import android.os.VibrationEffect
import android.app.AlarmManager

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("focusflow_prefs_v5", Context.MODE_PRIVATE)
    private val repository: FocusRepository

    // Onboarding tutorial state
    private val _showTutorial = MutableStateFlow(sharedPrefs.getBoolean("show_onboarding_tutorial_v5", true))
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val _showHowItWorks = MutableStateFlow(false)
    val showHowItWorks: StateFlow<Boolean> = _showHowItWorks.asStateFlow()

    fun setShowHowItWorks(show: Boolean) {
        _showHowItWorks.value = show
    }



    // Daily screen budget minutes (default calculated from questionnaire: 150m)
    private val _dailyScreentimeGoalMinutes = MutableStateFlow(sharedPrefs.getInt("daily_screentime_goal_minutes_v5", 150))
    val dailyScreentimeGoalMinutes: StateFlow<Int> = _dailyScreentimeGoalMinutes.asStateFlow()

    // Option widget notification mode: "goal" (goal progression) or "comparison" (comparison to previous and current)
    private val _widgetDisplayOption = MutableStateFlow(sharedPrefs.getString("widget_display_option_v5", "goal") ?: "goal")
    val widgetDisplayOption: StateFlow<String> = _widgetDisplayOption.asStateFlow()

    // Previous day screen time computed dynamically from DB
    val previousScreentimeMinutes: StateFlow<Int>
    
    // Dynamic streak calculation from timeline entries
    val currentStreak: StateFlow<Int>

    // Persistent Preferences from Me/Settings screenshots
    private val _accessibilityPermissionGranted = MutableStateFlow(sharedPrefs.getBoolean("accessibility_granted_v5", false))
    val accessibilityPermissionGranted: StateFlow<Boolean> = _accessibilityPermissionGranted.asStateFlow()

    private val _usageAccessPermissionGranted = MutableStateFlow(sharedPrefs.getBoolean("usage_access_granted_v5", false))
    val usageAccessPermissionGranted: StateFlow<Boolean> = _usageAccessPermissionGranted.asStateFlow()

    fun setAccessibilityPermissionGranted(granted: Boolean) {
        _accessibilityPermissionGranted.value = granted
        sharedPrefs.edit().putBoolean("accessibility_granted_v5", granted).apply()
    }

    fun setUsageAccessPermissionGranted(granted: Boolean) {
        _usageAccessPermissionGranted.value = granted
        sharedPrefs.edit().putBoolean("usage_access_granted_v5", granted).apply()
    }

    private val _dailyRemindersMode = MutableStateFlow(sharedPrefs.getString("daily_reminders_mode_v5", "Standard Mode") ?: "Standard Mode")
    val dailyRemindersMode: StateFlow<String> = _dailyRemindersMode.asStateFlow()

    private val _soundEffectsOn = MutableStateFlow(sharedPrefs.getBoolean("sound_effects_on_v5", true))
    val soundEffectsOn: StateFlow<Boolean> = _soundEffectsOn.asStateFlow()

    private val _smartSkipOn = MutableStateFlow(sharedPrefs.getBoolean("smart_skip_on_v5", true))
    val smartSkipOn: StateFlow<Boolean> = _smartSkipOn.asStateFlow()

    private val _stopWhenGoalAchieved = MutableStateFlow(sharedPrefs.getBoolean("stop_when_goal_achieved_v5", true))
    val stopWhenGoalAchieved: StateFlow<Boolean> = _stopWhenGoalAchieved.asStateFlow()

    private val _lockBypassEnabled = MutableStateFlow(sharedPrefs.getBoolean("lock_bypass_enabled_v5", false))
    val lockBypassEnabled: StateFlow<Boolean> = _lockBypassEnabled.asStateFlow()

    fun toggleLockBypass(enabled: Boolean) {
        _lockBypassEnabled.value = enabled
        sharedPrefs.edit().putBoolean("lock_bypass_enabled_v5", enabled).apply()
    }

    // Custom reminder lead-time (e.g., 5, 15, 30 mins before)
    private val _reminderLeadTimeMinutes = MutableStateFlow(sharedPrefs.getInt("reminder_lead_time_minutes_v5", 15))
    val reminderLeadTimeMinutes: StateFlow<Int> = _reminderLeadTimeMinutes.asStateFlow()

    fun updateReminderLeadTimeMinutes(minutes: Int) {
        _reminderLeadTimeMinutes.value = minutes
        sharedPrefs.edit().putInt("reminder_lead_time_minutes_v5", minutes).apply()
        addNotificationLog("Config Updated ⚙️", "Reminder lead-time set to $minutes minutes before curfew locking.", "System")
        scheduleAlarms()
    }

    // Alert frequency level (1-5 scale, persisted so dialog doesn't forget on reopen)
    private val _alertFrequencyLevel = MutableStateFlow(sharedPrefs.getFloat("alert_frequency_level_v5", 3f))
    val alertFrequencyLevel: StateFlow<Float> = _alertFrequencyLevel.asStateFlow()

    fun updateAlertFrequencyLevel(level: Float) {
        _alertFrequencyLevel.value = level
        sharedPrefs.edit().putFloat("alert_frequency_level_v5", level).apply()
    }

    // Sounds & Effects settings
    private val _selectedSoundName = MutableStateFlow(sharedPrefs.getString("selected_sound_name_v5", "Zen Temple Gong 🔔") ?: "Zen Temple Gong 🔔")
    val selectedSoundName: StateFlow<String> = _selectedSoundName.asStateFlow()

    private val _selectedSoundDuration = MutableStateFlow(sharedPrefs.getInt("selected_sound_duration_v5", 3))
    val selectedSoundDuration: StateFlow<Int> = _selectedSoundDuration.asStateFlow()

    private val _soundVolume = MutableStateFlow(sharedPrefs.getFloat("sound_volume_v5", 0.7f))
    val soundVolume: StateFlow<Float> = _soundVolume.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(sharedPrefs.getBoolean("sound_vibration_enabled_v5", true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun playAlertSound() {
        if (!_soundEffectsOn.value) return
        
        val soundResId = when (_selectedSoundName.value) {
            "Bamboo Chime 🎋" -> com.example.R.raw.bamboo_chime
            "Zen Temple Gong 🔔" -> com.example.R.raw.zen_temple_gong
            "Sleeping Kitty Flute 🍃" -> com.example.R.raw.sleeping_kitty_flute
            "Quiet Mountain Spring 🌊" -> com.example.R.raw.quiet_mountain_spring
            "Singing Bowl Chime 🍵" -> com.example.R.raw.singing_bowl_chime
            else -> com.example.R.raw.zen_temple_gong
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(getApplication(), soundResId)
            mediaPlayer?.setVolume(_soundVolume.value, _soundVolume.value)
            
            if (_vibrationEnabled.value) {
                val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }

            mediaPlayer?.start()

            viewModelScope.launch {
                delay(_selectedSoundDuration.value * 1000L)
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.stop()
                    }
                    mediaPlayer?.release()
                    mediaPlayer = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSelectedSound(name: String, duration: Int) {
        _selectedSoundName.value = name
        _selectedSoundDuration.value = duration
        sharedPrefs.edit()
            .putString("selected_sound_name_v5", name)
            .putInt("selected_sound_duration_v5", duration)
            .apply()
        addNotificationLog("Sound Profile Updated 🎵", "System focus alert profile is now configured to $name.", "Sound")
        playAlertSound()
    }

    private fun scheduleAlarms() {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val context = getApplication<Application>()

        val activeSchedules = schedules.value.filter { it.isLocked }
        val leadTimeMinutes = _reminderLeadTimeMinutes.value

        activeSchedules.forEach { schedule ->
            try {
                // Parse start time
                val startParts = schedule.startTime.split(":")
                val startHour = startParts[0].toIntOrNull() ?: 22
                val startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0

                // Parse end time to build subtitle
                val endParts = schedule.endTime.split(":")
                val endHour = endParts[0].toIntOrNull() ?: 23
                val endMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0
                
                // Helper to format AM/PM
                fun formatAmPm(h: Int, m: Int): String {
                    val ampm = if (h >= 12) "PM" else "AM"
                    val hour12 = if (h % 12 == 0) 12 else h % 12
                    return String.format("%d:%02d %s", hour12, m, ampm)
                }
                val subtitleText = "${formatAmPm(startHour, startMinute)} - ${formatAmPm(endHour, endMinute)}"
                
                // Setup Curfew Start Calendar
                val startCal = java.util.Calendar.getInstance()
                startCal.set(java.util.Calendar.HOUR_OF_DAY, startHour)
                startCal.set(java.util.Calendar.MINUTE, startMinute)
                startCal.set(java.util.Calendar.SECOND, 0)
                startCal.set(java.util.Calendar.MILLISECOND, 0)
                
                // If the start time has already passed today, don't trigger it again today
                val now = System.currentTimeMillis()
                val isStartInFuture = startCal.timeInMillis > now

                val startIntent = Intent(context, com.example.service.ReminderReceiver::class.java).apply {
                    putExtra("title", schedule.appName)
                    putExtra("subtitle", subtitleText)
                    putExtra("message", schedule.todoWhileLocked.ifBlank { "Time to focus!" })
                    putExtra("smsMessage", schedule.customAlertSms)
                    putExtra("playSound", true)
                    putExtra("notificationId", schedule.id * 100 + 1)
                }
                val startPendingIntent = PendingIntent.getBroadcast(
                    context,
                    schedule.id * 100 + 1,
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (isStartInFuture) {
                    val triggerTime = startCal.timeInMillis
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, startPendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, startPendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, startPendingIntent)
                    }
                }

                if (leadTimeMinutes > 0) {
                    val leadCal = java.util.Calendar.getInstance()
                    leadCal.timeInMillis = startCal.timeInMillis
                    leadCal.add(java.util.Calendar.MINUTE, -leadTimeMinutes)
                    
                    val isLeadInFuture = leadCal.timeInMillis > now

                    if (isLeadInFuture && isStartInFuture) {
                        val leadIntent = Intent(context, com.example.service.ReminderReceiver::class.java).apply {
                            putExtra("title", "${schedule.appName} (Upcoming)")
                            putExtra("subtitle", subtitleText)
                            putExtra("message", "Locks in $leadTimeMinutes minutes. ${schedule.todoWhileLocked}")
                            putExtra("smsMessage", schedule.customAlertSms)
                            putExtra("playSound", false)
                            putExtra("notificationId", schedule.id * 100 + 2)
                        }
                        val leadPendingIntent = PendingIntent.getBroadcast(
                            context,
                            schedule.id * 100 + 2,
                            leadIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val triggerTime = leadCal.timeInMillis
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, leadPendingIntent)
                            } else {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, leadPendingIntent)
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, leadPendingIntent)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSoundVolume(volume: Float) {
        _soundVolume.value = volume
        sharedPrefs.edit().putFloat("sound_volume_v5", volume).apply()
    }

    fun toggleVibration(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        sharedPrefs.edit().putBoolean("sound_vibration_enabled_v5", enabled).apply()
    }

    // Reactive Notification Log state
    private val _notificationLogs = MutableStateFlow<List<NotificationLog>>(emptyList())
    val notificationLogs: StateFlow<List<NotificationLog>> = _notificationLogs.asStateFlow()

    fun addNotificationLog(title: String, message: String, category: String) {
        val newLog = NotificationLog(
            title = title,
            message = message,
            category = category
        )
        _notificationLogs.value = listOf(newLog) + _notificationLogs.value
    }

    fun clearAllNotificationLogs() {
        _notificationLogs.value = emptyList()
    }

    // General preferences
    private val _screentimeUnits = MutableStateFlow(sharedPrefs.getString("screw_units_v5", "Hours, Mins") ?: "Hours, Mins")
    val screentimeUnits: StateFlow<String> = _screentimeUnits.asStateFlow()

    private val _firstDayOfWeek = MutableStateFlow(sharedPrefs.getString("first_day_of_week_v5", "Sunday") ?: "Sunday")
    val firstDayOfWeek: StateFlow<String> = _firstDayOfWeek.asStateFlow()

    private val _dayStartsAt = MutableStateFlow(sharedPrefs.getString("day_starts_at_v5", "00:00") ?: "00:00")
    val dayStartsAt: StateFlow<String> = _dayStartsAt.asStateFlow()

    private val _timeFormatPreference = MutableStateFlow(sharedPrefs.getString("time_format_preference_v5", "Follow The System") ?: "Follow The System")
    val timeFormatPreference: StateFlow<String> = _timeFormatPreference.asStateFlow()

    private val _languageOptionPreference = MutableStateFlow(sharedPrefs.getString("language_option_pref_v5", "English") ?: "English")
    val languageOptionPreference: StateFlow<String> = _languageOptionPreference.asStateFlow()

    fun updateDailyRemindersMode(mode: String) {
        _dailyRemindersMode.value = mode
        sharedPrefs.edit().putString("daily_reminders_mode_v5", mode).apply()
    }

    fun toggleSoundEffects(enabled: Boolean) {
        _soundEffectsOn.value = enabled
        sharedPrefs.edit().putBoolean("sound_effects_on_v5", enabled).apply()
    }

    fun toggleSmartSkip(enabled: Boolean) {
        _smartSkipOn.value = enabled
        sharedPrefs.edit().putBoolean("smart_skip_on_v5", enabled).apply()
    }

    fun toggleStopWhenGoalAchieved(enabled: Boolean) {
        _stopWhenGoalAchieved.value = enabled
        sharedPrefs.edit().putBoolean("stop_when_goal_achieved_v5", enabled).apply()
    }

    fun updateScreentimeUnits(units: String) {
        _screentimeUnits.value = units
        sharedPrefs.edit().putString("screw_units_v5", units).apply()
    }

    fun updateFirstDayOfWeek(day: String) {
        _firstDayOfWeek.value = day
        sharedPrefs.edit().putString("first_day_of_week_v5", day).apply()
    }

    fun updateDayStartsAt(time: String) {
        _dayStartsAt.value = time
        sharedPrefs.edit().putString("day_starts_at_v5", time).apply()
    }

    fun updateTimeFormatPreference(format: String) {
        _timeFormatPreference.value = format
        sharedPrefs.edit().putString("time_format_preference_v5", format).apply()
    }

    fun updateLanguageOptionPreference(lang: String) {
        _languageOptionPreference.value = lang
        sharedPrefs.edit().putString("language_option_pref_v5", lang).apply()
    }

    // Feed streams from Room database
    val usages: StateFlow<List<AppUsage>>
    val schedules: StateFlow<List<AppLockSchedule>>
    val timelineEntries: StateFlow<List<FocusTimelineEntry>>
    val chatMessages: StateFlow<List<MascotChatMessage>>

    // Sidebar filter
    private val _appFilter = MutableStateFlow("All")
    val appFilter: StateFlow<String> = _appFilter.asStateFlow()

    // Preset / Custom preserved routines
    private val _userPreservedDecks = MutableStateFlow<List<AppLockSchedule>>(emptyList())
    val userPreservedDecks: StateFlow<List<AppLockSchedule>> = _userPreservedDecks.asStateFlow()

    init {
        val database = FocusDatabase.getDatabase(application)
        repository = FocusRepository(database.focusDao())

        // Setup reactive Room flow pipes
        usages = repository.allUsagesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        schedules = repository.allSchedulesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        timelineEntries = repository.allTimelineEntriesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        chatMessages = repository.allChatMessagesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        previousScreentimeMinutes = usages.map { list ->
            val yesterdayDateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
                java.util.Date(System.currentTimeMillis() - 86400000L)
            )
            list.filter { it.dateString == yesterdayDateString }.sumOf { it.usageMinutes }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

        currentStreak = timelineEntries.map { entries ->
            if (entries.isEmpty()) return@map 0
            
            val dates = entries.map {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))
            }.distinct().sortedDescending()
            
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val yesterdayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis() - 86400000L))
            
            if (dates.isEmpty() || (dates.first() != todayStr && dates.first() != yesterdayStr)) {
                return@map 0
            }
            
            var streak = 1
            var currDateMs = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dates.first())?.time ?: return@map 0
            
            for (i in 1 until dates.size) {
                val nextDateMs = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dates[i])?.time ?: continue
                val diffDays = (currDateMs - nextDateMs) / 86400000L
                if (diffDays == 1L) {
                    streak++
                    currDateMs = nextDateMs
                } else {
                    break
                }
            }
            streak
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

        // Ingest natural baseline starting values if DB is freshly generated
        viewModelScope.launch {
            usages.first().let { currentList ->
                if (currentList.isEmpty()) {
                    presetDefaultUsages()
                }
                
                // Clear existing dummy usages
                currentList.forEach {
                    if (it.appName == "TikTok" || it.appName == "Instagram" || it.appName == "Notion") {
                        if (it.usageMinutes == 120 || it.usageMinutes == 90 || it.usageMinutes == 45) {
                            repository.deleteUsageById(it.id)
                        }
                    }
                }
            }
            schedules.first().let { currentScheds ->
                if (currentScheds.isEmpty()) {
                    presetDefaultSchedules()
                }
                
                // Clear existing dummy "TikTok" schedules from previous app versions
                currentScheds.forEach {
                    if (it.appName == "TikTok" && it.startTime == "21:00" && it.endTime == "23:00" && it.todoWhileLocked == "Physical reading & deep restorative sleep prep") {
                        repository.deleteScheduleById(it.id)
                    }
                }
            }
            timelineEntries.first().let { currentTimeline ->
                if (currentTimeline.isEmpty()) {
                    presetDefaultTimeline()
                }
                
                // Clear existing dummy timeline entries
                currentTimeline.forEach {
                    if (it.journalText.contains("deep focus block") || it.journalText.contains("Mindful warning")) {
                        repository.deleteTimelineEntry(it.id)
                    }
                }
            }
            chatMessages.first().let { currentHistory ->
                if (currentHistory.isEmpty()) {
                    presetDefaultChat()
                }
            }
        }

        // Keep system status notification live & reactive
        viewModelScope.launch {
            usages.collect {
                triggerAndroidNotification()
            }
        }

        // Schedule background alarms whenever schedules change
        viewModelScope.launch {
            schedules.collect {
                scheduleAlarms()
            }
        }

        // Pre-populate premium realistic notification logs
        val now = System.currentTimeMillis()
        _notificationLogs.value = listOf(
            NotificationLog(
                title = "Sleep Curfew Impending ⚠️",
                message = "Bedtime curfew starts in minutes. FocusFlow will shield TikTok and Instagram automatically.",
                timestamp = now - (15 * 60 * 1000), // 15m ago
                category = "Curfew"
            ),
            NotificationLog(
                title = "3-Day Streak Maintained 🔥",
                message = "Congratulations! Your consistent self-discipline habits have preserved your 3-day focus streak.",
                timestamp = now - (2 * 3600 * 1000), // 2h ago
                category = "Streak"
            ),
            NotificationLog(
                title = "High Scrolling Screen Alert 📱",
                message = "YouTube scrolling has reached 45 mins. Master Kitty recommends a quick 1-minute breathing focus.",
                timestamp = now - (5 * 3600 * 1000), // 5h ago
                category = "Dopamine"
            ),
            NotificationLog(
                title = "Curfew Block Enabled 🛡️",
                message = "Instagram and Reddit are locked until 07:00 tomorrow under the Work Routine schedule.",
                timestamp = now - (24 * 3600 * 1000), // 24h ago
                category = "System"
            )
        )
    }

    fun triggerAndroidNotification() {
        val context = getApplication<Application>()
        try {
            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val totalTime = usages.value.filter { it.timestamp >= startOfDay }.sumOf { it.usageMinutes }
            val goal = dailyScreentimeGoalMinutes.value
            val widgetOption = widgetDisplayOption.value
            val prevTime = previousScreentimeMinutes.value

            // Cache current and previous screentime for widget to display instantly
            sharedPrefs.edit()
                .putInt("current_screentime_minutes_v5", totalTime)
                .putInt("previous_screentime_minutes_v5", prevTime)
                .apply()

            // Broadcast update to homescreen widget
            val updateIntent = Intent(context, com.example.service.FocusFlowWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(updateIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "focusflow_activity_v5"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "FocusFlow Interactive Screen Progress",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Displays live-updated screentime and lock metrics on your system notification panel."
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, Class.forName("com.example.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIcon = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.example.R.drawable.cat_mascot_head_view
            )

            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val percent = if (goal > 0) (totalTime * 100) / goal else 100

            val titleText = when {
                totalTime >= goal -> "Screentime Limit Exceeded! ⚠️"
                percent >= 80 -> "Approaching Screen Limit! ⏳"
                hour >= 21 -> "Sleep Curfew Impending! 🌙"
                else -> "Coach Master Kitty 🐾"
            }

            val textContent = when {
                totalTime >= goal -> "You used $totalTime mins ($percent% of daily budget). Close the scrolls, focus now!"
                percent >= 80 -> "Spent: $totalTime mins ($percent% of daily $goal min budget). Guard your balance!"
                hour >= 21 -> "It's late ($totalTime mins used). Protect your rest, stay offline!"
                else -> if (widgetOption == "goal") {
                    "Spent: $totalTime mins ($percent% of daily $goal min goal)"
                } else {
                    val diff = totalTime - prevTime
                    val diffText = if (diff < 0) "${-diff}m less than yesterday! 🎉" else "${diff}m more than yesterday. ⚠️"
                    "Spent today: $totalTime mins vs Yesterday average: $prevTime mins. ($diffText)"
                }
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setLargeIcon(largeIcon)
                .setContentTitle(titleText)
                .setContentText(textContent)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            notificationManager.notify(777, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateDailyScreentimeGoal(minutes: Int) {
        sharedPrefs.edit().putInt("daily_screentime_goal_minutes_v5", minutes).apply()
        _dailyScreentimeGoalMinutes.value = minutes
        triggerAndroidNotification()
        addNotificationLog("Screentime Goal Updated 🎯", "Your daily screen budget is now set to $minutes minutes.", "Goal")
    }

    fun updateWidgetDisplayOption(option: String) {
        sharedPrefs.edit().putString("widget_display_option_v5", option).apply()
        _widgetDisplayOption.value = option
        triggerAndroidNotification()
        addNotificationLog("Widget Display Configured 📱", "Display preference updated to show '$option' configuration.", "Widget")
    }

    // fun updatePreviousScreentime(minutes: Int) removed because previousScreentimeMinutes is now dynamic


    fun setAppFilter(filter: String) {
        _appFilter.value = filter
    }

    fun getOnboardingSelection(key: String, default: String): String {
        return sharedPrefs.getString("user_onboarding_$key", default) ?: default
    }

    fun saveOnboardingSelections(
        role: String,
        struggle: String,
        currentUsage: String,
        exercise: String,
        sleepTime: String,
        calculatedGoalMinutes: Int
    ) {
        sharedPrefs.edit().apply {
            putString("user_onboarding_role", role)
            putString("user_onboarding_struggle", struggle)
            putString("user_onboarding_current_usage", currentUsage)
            putString("user_onboarding_exercise", exercise)
            putString("user_onboarding_sleep_time", sleepTime)
            putInt("daily_screentime_goal_minutes_v5", calculatedGoalMinutes)
            putBoolean("show_onboarding_tutorial_v5", false)
            apply()
        }
        _dailyScreentimeGoalMinutes.value = calculatedGoalMinutes
        _showTutorial.value = false
        _showHowItWorks.value = true

        // Automatically setup a customized Bedtime / Curfew lockdown block based on sleepHour selection!
        val startHour = try {
            val parts = sleepTime.split(":")
            val h = parts[0].toInt()
            val prevHour = if (h == 0) 23 else h - 1
            val formattedHour = if (prevHour < 10) "0$prevHour" else "$prevHour"
            "$formattedHour:00"
        } catch (e: Exception) {
            "22:00"
        }

        val appToLock = when {
            struggle.contains("Social", ignoreCase = true) -> "TikTok"
            struggle.contains("Video", ignoreCase = true) -> "YouTube"
            struggle.contains("Work", ignoreCase = true) -> "Slack"
            else -> "Instagram"
        }

        viewModelScope.launch {
            addLockSchedule(
                appName = appToLock,
                startTime = startHour,
                endTime = sleepTime,
                days = "Daily",
                todo = "Wind down screen-free: offline paper book, light stretching, deep breaths.",
                smsMsg = "FocusFlow Notification: Late night sleep lockdown active! Protect your bedtime rest."
            )
        }
    }

    fun dismissTutorial() {
        _showTutorial.value = false
        sharedPrefs.edit().putBoolean("show_onboarding_tutorial_v5", false).apply()
    }

    fun restartTutorial() {
        _showTutorial.value = true
        sharedPrefs.edit().putBoolean("show_onboarding_tutorial_v5", true).apply()
    }

    // Usages Actions
    fun insertUsageRecord(appName: String, usageMinutes: Int, category: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertUsage(AppUsage(appName = appName, usageMinutes = usageMinutes, category = category, timestamp = timestamp))
        }
    }

    fun deleteUsageRecord(usage: AppUsage) {
        viewModelScope.launch {
            repository.deleteUsage(usage)
        }
    }

    fun updateUsageRecord(usage: AppUsage) {
        viewModelScope.launch {
            repository.updateUsage(usage)
        }
    }

    fun clearAllUsages() {
        viewModelScope.launch {
            repository.deleteAllUsages()
        }
    }

    // Schedules Actions
    fun addLockSchedule(appName: String, startTime: String, endTime: String, days: String, todo: String, smsMsg: String, cooldownMinutes: Int = 0, usageThresholdMinutes: Int = 0) {
        viewModelScope.launch {
            repository.insertSchedule(
                AppLockSchedule(
                    appName = appName,
                    startTime = startTime,
                    endTime = endTime,
                    daysOfWeek = days,
                    todoWhileLocked = todo,
                    customAlertSms = smsMsg,
                    isLocked = true,
                    cooldownMinutes = cooldownMinutes,
                    usageThresholdMinutes = usageThresholdMinutes
                )
            )
            addNotificationLog(
                title = "Curfew Block Enabled 🛡️",
                message = "The routine schedule '$appName' ($startTime - $endTime, $days) is armed. All distraction apps will lock during this period.",
                category = "Curfew"
            )
        }
    }

    fun toggleScheduleLock(schedule: AppLockSchedule) {
        if (schedule.isLocked) { // About to be disabled
            cancelAlarmsForSchedule(schedule.id)
        }
        viewModelScope.launch {
            val updated = schedule.copy(isLocked = !schedule.isLocked)
            repository.insertSchedule(updated)
            val actionText = if (updated.isLocked) "Armed 🔒" else "Disarmed 🔓"
            addNotificationLog(
                title = "Schedule Toggled 🔄",
                message = "Routine schedule '${schedule.appName}' has been $actionText.",
                category = "Curfew"
            )
        }
    }

    private fun cancelAlarmsForSchedule(scheduleId: Int) {
        try {
            val context = getApplication<Application>()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val startIntent = Intent(context, com.example.service.ReminderReceiver::class.java)
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId * 100 + 1,
                startIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (startPendingIntent != null) {
                alarmManager.cancel(startPendingIntent)
                startPendingIntent.cancel()
            }

            val leadIntent = Intent(context, com.example.service.ReminderReceiver::class.java)
            val leadPendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId * 100 + 2,
                leadIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (leadPendingIntent != null) {
                alarmManager.cancel(leadPendingIntent)
                leadPendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteSchedule(id: Int) {
        cancelAlarmsForSchedule(id)
        viewModelScope.launch {
            repository.deleteScheduleById(id)
            addNotificationLog(
                title = "Schedule Deleted ❌",
                message = "A curfew routine schedule was removed from the active lock registry.",
                category = "System"
            )
        }
    }

    // Preserved Preset Decks Map
    fun saveScheduleToPresets(appName: String, startTime: String, endTime: String, days: String, todo: String, smsMsg: String, cooldownMinutes: Int = 0, usageThresholdMinutes: Int = 0) {
        val currentList = _userPreservedDecks.value.toMutableList()
        currentList.add(
            AppLockSchedule(
                appName = appName,
                startTime = startTime,
                endTime = endTime,
                daysOfWeek = days,
                todoWhileLocked = todo,
                customAlertSms = smsMsg,
                isLocked = true,
                cooldownMinutes = cooldownMinutes,
                usageThresholdMinutes = usageThresholdMinutes
            )
        )
        _userPreservedDecks.value = currentList
    }

    fun removeSavedPresetDeck(appName: String, startTime: String) {
        val currentList = _userPreservedDecks.value.toMutableList()
        currentList.removeAll { it.appName == appName && it.startTime == startTime }
        _userPreservedDecks.value = currentList
    }

    // Timeline journal Actions
    fun addJournalEntry(stars: Int, journalText: String, feelingTags: String, dateString: String) {
        viewModelScope.launch {
            val feedback = when (stars) {
                5 -> "Sensational zen state! Zero distraction slips. Your morning routine is doing wonders 🌟."
                4 -> "Outstanding focus block. Slipping only briefly. Keep cultivating those deep intervals!"
                3 -> "Decent balance, but let's seal late night lock gaps. Put down secondary screens 1h earlier."
                2 -> "Tough scrolling waves today! Do not judge yourself, just leverage curfew locks to assist recovery."
                else -> "Mindful warning: High scrolling loops logged. Let's install a proactive 30-min active lock limit."
            }

            repository.insertTimelineEntry(
                FocusTimelineEntry(
                    dateString = dateString,
                    pulseScore = stars,
                    journalText = journalText,
                    feelingTags = feelingTags,
                    coachFeedback = feedback
                )
            )
            addNotificationLog(
                title = "Focus Reflection Logged 📝",
                message = "Progress pulse rating ($stars stars) recorded successfully. Master Kitty feedback: $feedback",
                category = "Streak"
            )
        }
    }

    // AI Mascot Helper Chat Actions
    fun sendChatMessage(text: String) {
        viewModelScope.launch {
            // 1. Insert user message in DB
            repository.insertChatMessage(MascotChatMessage(text = text, isUser = true))

            // 2. Generate custom responsive chatbot suggestions from Mentor Master Kitty!
            val trimmed = text.trim().lowercase()
            val (answer, expr) = when {
                trimmed.contains("lock") || trimmed.contains("block") -> {
                    Pair("Understood! If you want to clamp down on distraction, go to the 'Schedules' tab to deploy a persistent lock with automatic SMS nudges! Or ask me anytime 🔒.", "focused")
                }
                trimmed.contains("tired") || trimmed.contains("sleep") || trimmed.contains("night") -> {
                    Pair("Ah, screen brightness strains eye sensory cells. I suggest turning on your nocturnal Curfew scheduler to protect mental energy recharge!", "sleepy")
                }
                trimmed.contains("stress") || trimmed.contains("anxious") || trimmed.contains("hard") -> {
                    Pair("Breathe with me: Inhale for 4s, hold for 4s, exhale for 4s. Digital scroll storms try to capture dopamine loops. You are in command 🧘.", "sad")
                }
                trimmed.contains("hi") || trimmed.contains("hello") || trimmed.contains("panda") || trimmed.contains("kitty") || trimmed.contains("cat") -> {
                    Pair("Hello there! I'm Master Kitty, your personal mental coaching companion. Let's conquer digital habits together today! How can I support you?", "happy")
                }
                else -> {
                    Pair("I hear you! Cultivating conscious attention takes systematic practice. Try setting up a curated deep study lock, or log your pulse score in our Today journal logs so we can design active progress metrics together!", "happy")
                }
            }

            repository.insertChatMessage(MascotChatMessage(text = answer, isUser = false, expression = expr))
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.deleteChatHistory()
            presetDefaultChat()
        }
    }

    // Presets
    private suspend fun presetDefaultUsages() {
        // No dummy data by default. 
        // Actual installed apps will be logged by the system dynamically.
    }

    private suspend fun presetDefaultSchedules() {
        // No dummy data by default.
    }

    private suspend fun presetDefaultTimeline() {
        // No dummy data by default.
    }

    private suspend fun presetDefaultChat() {
        repository.insertChatMessage(
            MascotChatMessage(
                text = "Greetings! I'm Master Kitty, your personal smart focus mentor. Tap here or ask me anything to coordinate lockdowns, mindfulness pauses, and habits!",
                isUser = false,
                expression = "happy"
            )
        )
    }
}

data class NotificationLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // "Curfew", "Streak", "Dopamine", "System"
    val isUnread: Boolean = true
)

fun isTimeInSchedule(startTime: String, endTime: String): Boolean {
    try {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentMinutes = currentHour * 60 + currentMinute

        val startParts = startTime.split(":")
        val startHour = startParts[0].toIntOrNull() ?: 22
        val startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0
        val startMinutes = startHour * 60 + startMinute

        val endParts = endTime.split(":")
        val endHour = endParts[0].toIntOrNull() ?: 7
        val endMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    } catch (e: Exception) {
        return true
    }
}
