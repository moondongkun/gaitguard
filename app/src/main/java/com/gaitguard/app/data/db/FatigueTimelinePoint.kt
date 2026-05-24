package com.gaitguard.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One data-point in a session's fatigue timeline.
 * Stored every ~2 seconds (matching the inference cadence).
 */
@Entity(
    tableName = "fatigue_timeline",
    foreignKeys = [
        ForeignKey(
            entity      = SessionRecord::class,
            parentColumns = ["id"],
            childColumns  = ["sessionId"],
            onDelete    = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class FatigueTimelinePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val elapsedSeconds: Int,   // seconds since session start
    val fatigueScore: Float,
    val symmetryScore: Float,
    val cadenceSpm: Float,
    val isMarked: Boolean = false,
    val isStumble: Boolean = false
)
