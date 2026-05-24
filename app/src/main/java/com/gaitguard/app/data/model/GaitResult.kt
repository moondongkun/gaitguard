package com.gaitguard.app.data.model

data class GaitResult(
    val cadenceSpm: Float = 0f,
    val isiMean: Float = 0f,
    val isiCvPercent: Float = 0f,
    val symmetryScore: Float = 100f,
    val harmonicRatio: Float = GaitResult.HR_HEALTHY_DEFAULT,
    val dominantFreqHz: Float = 0f,
    val bandEnergy: Float = 0f,
    val trunkTiltDeg: Float = 0f,
    val armSwingProxy: Float = 0f,
    val stanceSwingRatio: Float = 1.5f,
    val fatigueScore: Float = 0f,        // composite 0–100
    val tcnFatigue: Float = 0f,          // raw TFLite output 0–1
    val fallRiskLow: Float = 1f,
    val fallRiskMedium: Float = 0f,
    val fallRiskHigh: Float = 0f,
    val terrain: TerrainType = TerrainType.FLAT,
    val alertLevel: AlertLevel = AlertLevel.SAFE,
    val microStumbleCount: Int = 0,
    val inferenceLatencyMs: Long = 0L,
    val isModelLoaded: Boolean = false,
    val verticalAccWindow: FloatArray = FloatArray(0),   // for waveform chart
    val stepTimestamps: List<Long> = emptyList()
) {
    companion object {
        const val HR_HEALTHY_DEFAULT = 5f
    }
}

enum class AlertLevel { SAFE, AMBER, RED }

enum class FallRiskLevel { LOW, MEDIUM, HIGH }

fun GaitResult.fallRiskLevel(): FallRiskLevel = when {
    fallRiskHigh > 0.5f   -> FallRiskLevel.HIGH
    fallRiskMedium > 0.5f -> FallRiskLevel.MEDIUM
    else                  -> FallRiskLevel.LOW
}
