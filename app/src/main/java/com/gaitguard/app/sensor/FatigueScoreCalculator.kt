package com.gaitguard.app.sensor

import com.gaitguard.app.data.model.AlertLevel
import com.gaitguard.app.util.GaitConstants

data class FatigueComponents(
    val composite: Float,        // 0–100
    val tcnComponent: Float,
    val isiCvComponent: Float,
    val harmonicComponent: Float,
    val trunkComponent: Float,
    val alertLevel: AlertLevel
)

object FatigueScoreCalculator {

    /**
     * Compute composite fatigue score [0–100]:
     *   TCN fatigue output:           55%
     *   ISI coefficient of variation: 20%
     *   Harmonic ratio decline:       15%
     *   Trunk tilt deviation:         10%
     */
    fun compute(
        tcnFatigue: Float,       // TFLite output [0, 1]
        isiCvPercent: Float,     // % — healthy <3%, fatigued >8%
        harmonicRatio: Float,    // healthy >5, fatigued <3
        trunkTiltDeg: Float      // deviation in degrees
    ): FatigueComponents {
        val tcnScore      = (tcnFatigue * 100f).coerceIn(0f, 100f)
        val isiScore      = mapIsiCvToScore(isiCvPercent)
        val harmonicScore = mapHarmonicToScore(harmonicRatio)
        val trunkScore    = mapTrunkTiltToScore(trunkTiltDeg)

        val composite = (tcnScore * GaitConstants.WEIGHT_TCN_FATIGUE +
                         isiScore * GaitConstants.WEIGHT_ISI_CV +
                         harmonicScore * GaitConstants.WEIGHT_HARMONIC +
                         trunkScore * GaitConstants.WEIGHT_TRUNK_TILT)
                        .coerceIn(0f, 100f)

        val alertLevel = when {
            composite >= GaitConstants.FATIGUE_RED   -> AlertLevel.RED
            composite >= GaitConstants.FATIGUE_AMBER -> AlertLevel.AMBER
            else                                     -> AlertLevel.SAFE
        }

        return FatigueComponents(composite, tcnScore, isiScore, harmonicScore, trunkScore, alertLevel)
    }

    // ISI CV: 0% → score 0, 8%+ → score 100
    private fun mapIsiCvToScore(cv: Float): Float =
        ((cv - GaitConstants.ISI_CV_HEALTHY) /
         (GaitConstants.ISI_CV_FATIGUED - GaitConstants.ISI_CV_HEALTHY) * 100f)
            .coerceIn(0f, 100f)

    // Harmonic ratio: >5 = healthy (score 0), <3 = fatigued (score 100)
    private fun mapHarmonicToScore(hr: Float): Float =
        ((GaitConstants.HR_HEALTHY - hr) /
         (GaitConstants.HR_HEALTHY - GaitConstants.HR_FATIGUED) * 100f)
            .coerceIn(0f, 100f)

    // Trunk tilt: 0° = score 0, 15°+ = score 100
    private fun mapTrunkTiltToScore(deg: Float): Float =
        (kotlin.math.abs(deg) / 15f * 100f).coerceIn(0f, 100f)
}
