package com.gaitguard.app.data.model

import com.google.mlkit.vision.pose.PoseLandmark

data class PostureResult(
    val shoulderLevelDeg: Float = 0f,
    val kyphosisAngleDeg: Float = 0f,
    val pelvicTiltDeg: Float = 0f,
    val asymmetryIndex: Float = 0f,
    val axialAligned: Boolean = true,
    val avgConfidence: Float = 0f,
    val landmarks: List<LandmarkPoint> = emptyList(),
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val shoulderSeverity: MetricSeverity get() = when {
        kotlin.math.abs(shoulderLevelDeg) > 5f -> MetricSeverity.ALERT
        kotlin.math.abs(shoulderLevelDeg) > 2f -> MetricSeverity.WATCH
        else                                    -> MetricSeverity.NORMAL
    }
    val kyphosisSeverity: MetricSeverity get() = when {
        kyphosisAngleDeg > 50f -> MetricSeverity.ALERT
        kyphosisAngleDeg > 40f -> MetricSeverity.WATCH
        else                   -> MetricSeverity.NORMAL
    }
    val pelvicSeverity: MetricSeverity get() = when {
        kotlin.math.abs(pelvicTiltDeg) > 4f -> MetricSeverity.ALERT
        kotlin.math.abs(pelvicTiltDeg) > 2f -> MetricSeverity.WATCH
        else                                 -> MetricSeverity.NORMAL
    }

    /**
     * Composite Spinal Health Score 0–100.
     * Weights: kyphosis 35%, shoulder 25%, pelvic 25%, axial alignment 15%
     */
    val spinalHealthScore: Int get() {
        val k = when (kyphosisSeverity)  { MetricSeverity.NORMAL->100f; MetricSeverity.WATCH->65f; MetricSeverity.ALERT->30f }
        val s = when (shoulderSeverity)  { MetricSeverity.NORMAL->100f; MetricSeverity.WATCH->70f; MetricSeverity.ALERT->35f }
        val p = when (pelvicSeverity)    { MetricSeverity.NORMAL->100f; MetricSeverity.WATCH->70f; MetricSeverity.ALERT->35f }
        val a = if (axialAligned) 100f else 50f
        return (k * 0.35f + s * 0.25f + p * 0.25f + a * 0.15f).toInt().coerceIn(0, 100)
    }

    val clinicalGrade: String get() = when {
        spinalHealthScore >= 85 -> "Excellent ROM"
        spinalHealthScore >= 70 -> "Good Alignment"
        spinalHealthScore >= 55 -> "Fair Posture"
        spinalHealthScore >= 40 -> "Below Average"
        else                    -> "Needs Attention"
    }
}

data class LandmarkPoint(
    val id: Int,
    val x: Float,          // normalized [0, 1]
    val y: Float,          // normalized [0, 1]
    val confidence: Float
)

enum class MetricSeverity { NORMAL, WATCH, ALERT }
