package com.gaitguard.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val durationMinutes: Int,
    val peakFatigueScore: Float,
    val fatigueOnsetMinute: Int,
    val avgSymmetryScore: Float,
    val fallRiskEventCount: Int,
    val microStumbleCount: Int,
    val enduranceScore: Float
)
