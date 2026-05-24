package com.gaitguard.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionRecord::class, PostureSnapshot::class, FatigueTimelinePoint::class],
    version = 5,
    exportSchema = false
)
abstract class GaitDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun postureDao(): PostureDao
    abstract fun timelineDao(): TimelineDao

    companion object {
        @Volatile private var INSTANCE: GaitDatabase? = null

        fun getInstance(context: Context): GaitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GaitDatabase::class.java,
                    "gait_guard.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
