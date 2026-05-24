package com.gaitguard.app.sensor

import com.gaitguard.app.data.model.MetricSeverity
import com.gaitguard.app.data.model.PostureResult
import kotlin.math.abs

/**
 * Rule-based posture coaching for instant, camera-time feedback.
 */
object PostureCoach {

    data class Cue(
        val severity: MetricSeverity,
        val headline: String,
        val detail: String
    )

    fun evaluate(result: PostureResult): List<Cue> {
        val cues = mutableListOf<Cue>()

        when (result.kyphosisSeverity) {
            MetricSeverity.ALERT -> cues.add(
                Cue(
                    MetricSeverity.ALERT,
                    "Forward stoop detected (${result.kyphosisAngleDeg.toInt()} deg)",
                    "Lift your chin slightly, lengthen your neck, and pull your shoulder blades gently back."
                )
            )
            MetricSeverity.WATCH -> cues.add(
                Cue(
                    MetricSeverity.WATCH,
                    "Mild forward lean (${result.kyphosisAngleDeg.toInt()} deg)",
                    "Imagine a string lifting the crown of your head toward the ceiling."
                )
            )
            MetricSeverity.NORMAL -> Unit
        }

        when (result.shoulderSeverity) {
            MetricSeverity.ALERT -> cues.add(
                Cue(
                    MetricSeverity.ALERT,
                    "Shoulder imbalance (${abs(result.shoulderLevelDeg).toInt()} deg)",
                    "Lower the higher shoulder, relax both shoulders away from your ears, and square your chest."
                )
            )
            MetricSeverity.WATCH -> cues.add(
                Cue(
                    MetricSeverity.WATCH,
                    "Slight shoulder unevenness",
                    "Relax both shoulders downward and keep your collarbones level."
                )
            )
            MetricSeverity.NORMAL -> Unit
        }

        when (result.pelvicSeverity) {
            MetricSeverity.ALERT -> cues.add(
                Cue(
                    MetricSeverity.ALERT,
                    "Hip tilt detected (${abs(result.pelvicTiltDeg).toInt()} deg)",
                    "Engage your core and shift your weight evenly across both feet."
                )
            )
            MetricSeverity.WATCH -> cues.add(
                Cue(
                    MetricSeverity.WATCH,
                    "Minor pelvic tilt",
                    "Gently tuck your pelvis and activate your abdominal muscles."
                )
            )
            MetricSeverity.NORMAL -> Unit
        }

        if (!result.axialAligned && result.avgConfidence > 0.6f) {
            cues.add(
                Cue(
                    MetricSeverity.WATCH,
                    "Spine drifting off center",
                    "Stack your head over shoulders, shoulders over hips, and hips over ankles."
                )
            )
        }

        if (result.asymmetryIndex > 70f) {
            cues.add(
                Cue(
                    MetricSeverity.ALERT,
                    "High body asymmetry (${result.asymmetryIndex.toInt()}%)",
                    "Stand on flat ground and focus on equal left-right weight distribution."
                )
            )
        }

        if (cues.isEmpty()) {
            cues.add(
                Cue(
                    MetricSeverity.NORMAL,
                    "Posture looks great",
                    "Keep your head level and maintain a natural, relaxed shoulder position."
                )
            )
        }

        return cues.sortedByDescending { it.severity.ordinal }
    }
}
