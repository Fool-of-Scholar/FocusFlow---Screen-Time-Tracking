package com.example.data.dao

import androidx.room.*
import com.example.data.model.AppUsage
import com.example.data.model.AppLockSchedule
import com.example.data.model.FocusTimelineEntry
import com.example.data.model.MascotChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    // Usages
    @Query("SELECT * FROM app_usages ORDER BY usageMinutes DESC")
    fun getAllUsagesFlow(): Flow<List<AppUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AppUsage)

    @Update
    suspend fun updateUsage(usage: AppUsage)

    @Delete
    suspend fun deleteUsage(usage: AppUsage)

    @Query("DELETE FROM app_usages")
    suspend fun deleteAllUsages()

    // Lock Schedules
    @Query("SELECT * FROM lock_schedules ORDER BY id DESC")
    fun getAllSchedulesFlow(): Flow<List<AppLockSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: AppLockSchedule)

    @Update
    suspend fun updateSchedule(schedule: AppLockSchedule)

    @Query("DELETE FROM lock_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Int)

    // Timeline journal entries
    @Query("SELECT * FROM timeline_entries ORDER BY timestamp DESC")
    fun getAllTimelineEntriesFlow(): Flow<List<FocusTimelineEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEntry(entry: FocusTimelineEntry)

    // Chat history
    @Query("SELECT * FROM mascot_chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessagesFlow(): Flow<List<MascotChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: MascotChatMessage)

    @Query("DELETE FROM mascot_chat_messages")
    suspend fun deleteChatHistory()
}
