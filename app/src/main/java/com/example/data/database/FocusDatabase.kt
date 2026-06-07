package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.FocusDao
import com.example.data.model.AppUsage
import com.example.data.model.AppLockSchedule
import com.example.data.model.FocusTimelineEntry
import com.example.data.model.MascotChatMessage

@Database(
    entities = [
        AppUsage::class,
        AppLockSchedule::class,
        FocusTimelineEntry::class,
        MascotChatMessage::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun focusDao(): FocusDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null

        fun getDatabase(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focus_flow_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
