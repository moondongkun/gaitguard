package com.gaitguard.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posture_snapshots")
data class PostureSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val shoulderLevelDeg: Float,
    val kyphosisAngleDeg: Float,
    val pelvicTiltDeg: Float,
    val asymmetryIndex: Float,
    val axialAligned: Boolean,
    // v4 additions ─────────────────────────────────────────────────────────────
    /** Pre-computed Spinal Health Score (0-100). */
    val spinalHealthScore: Int = 0,
    /** Pre-computed clinical grade string (e.g. "Good Alignment"). */
    val clinicalGrade: String = "",
    /**
     * Non-null when the snapshot was produced from a gallery / captured photo.
     * Null = live-camera capture.
     */
    val sourceUri: String? = null
)
