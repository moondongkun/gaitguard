package com.gaitguard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionRecord): Long

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<SessionRecord>>

    @Query("SELECT * FROM sessions WHERE peakFatigueScore >= :threshold ORDER BY date DESC")
    fun getHighFatigueSessions(threshold: Float = 40f): Flow<List<SessionRecord>>

    @Query("SELECT * FROM sessions WHERE date >= :cutoff ORDER BY date DESC")
    fun getSessionsAfter(cutoff: Long): Flow<List<SessionRecord>>

    @Query("SELECT * FROM sessions ORDER BY date DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 10): Flow<List<SessionRecord>>

    @Query("SELECT * FROM sessions ORDER BY date DESC LIMIT 1")
    suspend fun getLastSession(): SessionRecord?

    @Query("SELECT MAX(enduranceScore) FROM sessions")
    suspend fun getPersonalBestEndurance(): Float?

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    /** Blocking (suspend) version used by background Worker. */
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    suspend fun getAllSessionsSync(): List<SessionRecord>
}
