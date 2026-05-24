package com.gaitguard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PostureSnapshot): Long

    @Query("SELECT * FROM posture_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSnapshots(limit: Int = 20): Flow<List<PostureSnapshot>>

    @Query("SELECT * FROM posture_snapshots WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getSnapshotsInRange(start: Long, end: Long): Flow<List<PostureSnapshot>>

    @Query("SELECT COUNT(*) FROM posture_snapshots")
    suspend fun getSnapshotCount(): Int

    @Query("DELETE FROM posture_snapshots WHERE id = :id")
    suspend fun deleteSnapshot(id: Long)

    @Query("DELETE FROM posture_snapshots")
    suspend fun deleteAllSnapshots()

    /** Returns only snapshots that came from gallery / captured photos. */
    @Query("SELECT * FROM posture_snapshots WHERE sourceUri IS NOT NULL ORDER BY timestamp DESC LIMIT :limit")
    fun getPhotoRecords(limit: Int = 30): Flow<List<PostureSnapshot>>

    /** Returns all snapshots regardless of source (live + gallery). */
    @Query("SELECT * FROM posture_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getAllRecords(limit: Int = 50): Flow<List<PostureSnapshot>>
}
