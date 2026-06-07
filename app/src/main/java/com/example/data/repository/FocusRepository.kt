package com.example.data.repository

import com.example.data.dao.FocusDao
import com.example.data.model.AppUsage
import com.example.data.model.AppLockSchedule
import com.example.data.model.FocusTimelineEntry
import com.example.data.model.MascotChatMessage
import kotlinx.coroutines.flow.Flow

class FocusRepository(private val focusDao: FocusDao) {
    val allUsagesFlow: Flow<List<AppUsage>> = focusDao.getAllUsagesFlow()
    val allSchedulesFlow: Flow<List<AppLockSchedule>> = focusDao.getAllSchedulesFlow()
    val allTimelineEntriesFlow: Flow<List<FocusTimelineEntry>> = focusDao.getAllTimelineEntriesFlow()
    val allChatMessagesFlow: Flow<List<MascotChatMessage>> = focusDao.getAllChatMessagesFlow()

    suspend fun insertUsage(usage: AppUsage) {
        focusDao.insertUsage(usage)
    }

    suspend fun updateUsage(usage: AppUsage) {
        focusDao.updateUsage(usage)
    }

    suspend fun deleteUsage(usage: AppUsage) {
        focusDao.deleteUsage(usage)
    }

    suspend fun deleteAllUsages() {
        focusDao.deleteAllUsages()
    }

    suspend fun insertSchedule(schedule: AppLockSchedule) {
        focusDao.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: AppLockSchedule) {
        focusDao.updateSchedule(schedule)
    }

    suspend fun deleteScheduleById(id: Int) {
        focusDao.deleteScheduleById(id)
    }

    suspend fun insertTimelineEntry(entry: FocusTimelineEntry) {
        focusDao.insertTimelineEntry(entry)
    }

    suspend fun insertChatMessage(message: MascotChatMessage) {
        focusDao.insertChatMessage(message)
    }

    suspend fun deleteChatHistory() {
        focusDao.deleteChatHistory()
    }
}
