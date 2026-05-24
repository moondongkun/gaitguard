package com.gaitguard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TimelineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<FatigueTimelinePoint>)

    @Query("SELECT * FROM fatigue_timeline WHERE sessionId = :sessionId ORDER BY elapsedSeconds ASC")
    suspend fun getTimelineForSession(sessionId: Long): List<FatigueTimelinePoint>

    @Query("DELETE FROM fatigue_timeline WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("DELETE FROM fatigue_timeline")
    suspend fun deleteAll()
}
