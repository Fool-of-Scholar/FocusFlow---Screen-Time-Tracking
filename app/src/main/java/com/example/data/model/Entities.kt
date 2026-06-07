package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "app_usages")
@Serializable
data class AppUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val usageMinutes: Int,
    val category: String, // "Distraction" or "Productive"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lock_schedules")
@Serializable
data class AppLockSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val startTime: String, // e.g., "22:00"
    val endTime: String, // e.g., "23:00"
    val daysOfWeek: String, // e.g., "Daily", "Mon, Wed, Fri"
    val todoWhileLocked: String,
    val customAlertSms: String,
    val isLocked: Boolean = true,
    val cooldownMinutes: Int = 0,
    val usageThresholdMinutes: Int = 0
)

@Entity(tableName = "timeline_entries")
@Serializable
data class FocusTimelineEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // e.g., "June 4"
    val pulseScore: Int, // 1 to 5 stars
    val journalText: String,
    val feelingTags: String, // comma-separated e.g., "focused, tired"
    val coachFeedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mascot_chat_messages")
@Serializable
data class MascotChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val expression: String = "happy", // happy, sad, focused, sleepy
    val timestamp: Long = System.currentTimeMillis()
)
