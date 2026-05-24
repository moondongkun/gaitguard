package com.gaitguard.app.sensor

import com.gaitguard.app.data.model.TerrainType
import com.gaitguard.app.util.GaitConstants
import kotlin.math.abs
import kotlin.math.sqrt

class TerrainClassifier {

    private val pitchHistory = ArrayDeque<Float>(100)

    /**
     * Classify terrain from the last [window] of sensor data.
     * Called every 8 seconds from GaitSensorManager.
     */
    fun classify(window: Array<FloatArray>): TerrainType {
        if (window.isEmpty()) return TerrainType.FLAT

        val vertAcc  = FloatArray(window.size) { window[it][2] }  // body-frame vertical
        val latAcc   = FloatArray(window.size) { window[it][0] }  // lateral
        val pitchGy  = FloatArray(window.size) { window[it][3] }  // pitch gyro

        val vertVariance = variance(vertAcc)
        val latStdDev    = stdDev(latAcc)
        val pitchOffset  = pitchGy.average().toFloat()

        // Accumulate pitch for sustained incline detection
        pitchHistory.addAll(pitchGy.toList())
        while (pitchHistory.size > 400) pitchHistory.removeFirst()
        val sustainedPitch = if (pitchHistory.isNotEmpty()) pitchHistory.average().toFloat() else 0f

        return when {
            // High vertical variance + positive sustained pitch → stairs ascending
            vertVariance > 8f && pitchOffset > 0.15f -> TerrainType.STAIRS_ASC

            // High vertical variance + negative sustained pitch → stairs descending
            vertVariance > 8f && pitchOffset < -0.15f -> TerrainType.STAIRS_DESC

            // High lateral std → uneven terrain
            latStdDev > 1.5f -> TerrainType.UNEVEN

            // Sustained pitch, low vertical variance → incline
            abs(sustainedPitch) > 0.1f && vertVariance < 5f -> TerrainType.INCLINE

            else -> TerrainType.FLAT
        }
    }

    private fun variance(data: FloatArray): Float {
        val mean = data.average().toFloat()
        return data.sumOf { ((it - mean) * (it - mean)).toDouble() }.toFloat() / data.size
    }

    private fun stdDev(data: FloatArray): Float = sqrt(variance(data))
}
