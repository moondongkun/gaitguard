package com.gaitguard.app.util

import android.content.Context
import android.util.Log
import com.gaitguard.app.data.db.FatigueTimelinePoint
import com.gaitguard.app.data.db.GaitDatabase
import com.gaitguard.app.data.db.PostureSnapshot
import com.gaitguard.app.data.db.SessionRecord
import kotlin.math.roundToInt

object DemoDataSeeder {
    private const val TAG = "DemoDataSeeder"

    // Keep enabled while you need screenshot data. It only inserts when tables are empty.
    private const val ENABLE_DEMO_DATA = true

    suspend fun seedIfEmpty(context: Context) {
        if (!ENABLE_DEMO_DATA) return

        val db = GaitDatabase.getInstance(context)
        seedGaitSessionsIfEmpty(db)
        seedPostureSnapshotsIfEmpty(db)
    }

    private suspend fun seedGaitSessionsIfEmpty(db: GaitDatabase) {
        if (db.sessionDao().getSessionCount() > 0) return

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val samples = listOf(
            DemoSession(0, 36, 46f, 18, 88f, 0, 2, 74f),
            DemoSession(1, 32, 39f, 20, 91f, 0, 1, 78f),
            DemoSession(2, 41, 58f, 16, 84f, 1, 3, 66f),
            DemoSession(3, 28, 34f, 19, 94f, 0, 0, 82f),
            DemoSession(4, 38, 52f, 17, 87f, 0, 2, 70f),
            DemoSession(5, 30, 42f, 21, 92f, 0, 1, 77f),
            DemoSession(6, 35, 48f, 18, 89f, 0, 2, 73f)
        )

        samples.forEach { demo ->
            val date = now - demo.daysAgo * dayMs - 90L * 60L * 1000L
            val sessionId = db.sessionDao().insertSession(
                SessionRecord(
                    date = date,
                    durationMinutes = demo.durationMinutes,
                    peakFatigueScore = demo.peakFatigueScore,
                    fatigueOnsetMinute = demo.fatigueOnsetMinute,
                    avgSymmetryScore = demo.avgSymmetryScore,
                    fallRiskEventCount = demo.fallRiskEventCount,
                    microStumbleCount = demo.microStumbleCount,
                    enduranceScore = demo.enduranceScore
                )
            )
            db.timelineDao().insertPoints(buildTimeline(sessionId, demo))
        }

        Log.i(TAG, "Inserted demo gait sessions for screenshots")
    }

    private fun buildTimeline(sessionId: Long, demo: DemoSession): List<FatigueTimelinePoint> {
        val pointCount = 18
        val maxSeconds = demo.durationMinutes * 60
        return (0 until pointCount).map { index ->
            val progress = index / (pointCount - 1).toFloat()
            val elapsed = (progress * maxSeconds).roundToInt()
            val fatigue = (18f + demo.peakFatigueScore * progress + wave(index, 6f))
                .coerceIn(8f, demo.peakFatigueScore)
            val symmetry = (demo.avgSymmetryScore + wave(index + 2, 3.5f) - progress * 2f)
                .coerceIn(72f, 99f)
            val cadence = (101f + wave(index + demo.daysAgo, 7f) - progress * 4f)
                .coerceIn(84f, 116f)

            FatigueTimelinePoint(
                sessionId = sessionId,
                elapsedSeconds = elapsed,
                fatigueScore = fatigue,
                symmetryScore = symmetry,
                cadenceSpm = cadence,
                isMarked = index == 5 || index == 12,
                isStumble = demo.microStumbleCount > 0 && (index == 9 || demo.microStumbleCount > 1 && index == 14)
            )
        }
    }

    private suspend fun seedPostureSnapshotsIfEmpty(db: GaitDatabase) {
        if (db.postureDao().getSnapshotCount() > 0) return

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val scores = listOf(82, 78, 85, 74, 88, 91, 86)

        scores.forEachIndexed { index, score ->
            val daysAgo = 6 - index
            val baseTime = now - daysAgo * dayMs
            val shoulder = when {
                score >= 88 -> 1.2f
                score >= 82 -> 2.4f
                else -> 4.1f
            }
            val kyphosis = when {
                score >= 88 -> 37f
                score >= 82 -> 42f
                else -> 47f
            }
            val pelvic = when {
                score >= 88 -> 1.4f
                score >= 82 -> 2.5f
                else -> 3.6f
            }

            repeat(2) { sample ->
                db.postureDao().insertSnapshot(
                    PostureSnapshot(
                        timestamp = baseTime - (8L + sample * 5L) * 60L * 60L * 1000L,
                        shoulderLevelDeg = shoulder + sample * 0.3f,
                        kyphosisAngleDeg = kyphosis + sample,
                        pelvicTiltDeg = pelvic + sample * 0.2f,
                        asymmetryIndex = (100 - score) / 100f,
                        axialAligned = score >= 80,
                        spinalHealthScore = score,
                        clinicalGrade = when {
                            score >= 90 -> "Excellent Alignment"
                            score >= 82 -> "Good Alignment"
                            score >= 74 -> "Mild Forward Posture"
                            else -> "Needs Attention"
                        },
                        sourceUri = null
                    )
                )
            }
        }

        Log.i(TAG, "Inserted demo posture snapshots for screenshots")
    }

    private fun wave(index: Int, amplitude: Float): Float {
        val pattern = intArrayOf(-2, 1, 3, -1, 2, -3)
        return pattern[index % pattern.size] * amplitude / 3f
    }

    private data class DemoSession(
        val daysAgo: Int,
        val durationMinutes: Int,
        val peakFatigueScore: Float,
        val fatigueOnsetMinute: Int,
        val avgSymmetryScore: Float,
        val fallRiskEventCount: Int,
        val microStumbleCount: Int,
        val enduranceScore: Float
    )
}
