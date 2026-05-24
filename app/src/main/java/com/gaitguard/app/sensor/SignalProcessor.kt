package com.gaitguard.app.sensor

import com.gaitguard.app.util.ButterworthFilter
import com.gaitguard.app.util.FftProcessor
import com.gaitguard.app.util.GaitConstants
import kotlin.math.*

data class ProcessedFeatures(
    val cadenceSpm: Float,
    val isiMean: Float,
    val isiCvPercent: Float,
    val symmetryScore: Float,
    val dominantFreqHz: Float,
    val harmonicRatio: Float,
    val bandEnergy: Float,
    val symmetryIndex: Float,
    val trunkTiltDeg: Float,
    val armSwingProxy: Float,
    val stepThreshold: Float,
    val stanceSwingRatio: Float,
    val verticalAccWindow: FloatArray,
    val stepTimestamps: List<Long>,
    val featureVector: FloatArray   // normalized 12-element vector for TFLite
)

class SignalProcessor {

    private val lpf = ButterworthFilter(numChannels = 1)
    private val fft = FftProcessor()

    // Rolling peak history for dynamic threshold
    private val peakHistory = ArrayDeque<Float>(50)

    // Step detection state
    private var lastStepTimeMs = 0L
    private var prevSample = 0f
    private var wasAboveThreshold = false

    // Baseline for Z-score normalization (set from BaselineCalibrator)
    var baselineMean: FloatArray = FloatArray(12) { 0f }
    var baselineStd: FloatArray  = FloatArray(12) { 1f }

    fun reset() {
        lpf.reset()
        peakHistory.clear()
        lastStepTimeMs = 0L
        wasAboveThreshold = false
    }

    /**
     * Process a 200-sample window and extract all 12 features.
     * [window]: Array<FloatArray(6)> = [AccX, AccY, AccZ, GyroX, GyroY, GyroZ]
     */
    fun process(window: Array<FloatArray>, windowStartTimeMs: Long): ProcessedFeatures {
        val vertAcc = FloatArray(window.size) { window[it][2] }   // body-frame vertical
        val latAcc  = FloatArray(window.size) { window[it][0] }   // lateral
        val pitchGy = FloatArray(window.size) { window[it][3] }   // pitch gyro
        val yawGy   = FloatArray(window.size) { window[it][5] }   // yaw gyro

        // 1) Low-pass filter vertical acc
        val filteredVert = lpf.filterArray(vertAcc)

        // 2) Step detection
        val dynamicThreshold = computeDynamicThreshold(filteredVert)
        val stepTimestamps = detectSteps(filteredVert, dynamicThreshold, windowStartTimeMs)
        val stepIntervals = computeStepIntervals(stepTimestamps)

        // 3) Gait rhythm features
        val cadence = if (stepIntervals.isNotEmpty())
            60_000f / stepIntervals.average().toFloat() else 0f
        val isiMean = if (stepIntervals.isNotEmpty())
            stepIntervals.average().toFloat() else 0f
        val isiCv = if (isiMean > 0f && stepIntervals.size > 1)
            (stepIntervals.std() / isiMean * 100f) else 0f

        // 4) Symmetry index
        val si = max(
            computeSymmetryIndex(stepIntervals),
            computeLateralSymmetryIndex(latAcc)
        )
        val symmetryScore = (100f - si).coerceIn(0f, 100f)

        // 5) FFT features
        fft.compute(filteredVert)
        val domFreq      = fft.dominantFrequency(0.5f, 3f)
        val harmonicRatio = fft.harmonicRatio(0.5f, 3f)
        val bandEnergy   = fft.bandEnergy(0.5f, 5f)

        // 6) Trunk tilt: integrate pitch gyro per step cycle
        val trunkTilt = computeTrunkTilt(pitchGy, stepTimestamps, windowStartTimeMs)

        // 7) Arm swing proxy: peak-to-peak yaw per stride
        val armSwing = computeArmSwing(yawGy)

        // 8) Stance/swing ratio (simplified from step intervals + signal shape)
        val stanceSwing = 1.5f  // default; refined via signal zero-crossings if steps available

        // 9) Build 12-feature vector
        val raw = floatArrayOf(
            cadence, isiMean, isiCv, si, domFreq, harmonicRatio,
            bandEnergy, symmetryScore, trunkTilt, armSwing, dynamicThreshold, stanceSwing
        )

        // 10) Z-score normalize against personal baseline
        val normalized = FloatArray(12) { i ->
            val std = if (baselineStd[i] > 1e-6f) baselineStd[i] else 1f
            (raw[i] - baselineMean[i]) / std
        }

        return ProcessedFeatures(
            cadenceSpm       = cadence,
            isiMean          = isiMean,
            isiCvPercent     = isiCv,
            symmetryScore    = symmetryScore,
            dominantFreqHz   = domFreq,
            harmonicRatio    = harmonicRatio,
            bandEnergy       = bandEnergy,
            symmetryIndex    = si,
            trunkTiltDeg     = trunkTilt,
            armSwingProxy    = armSwing,
            stepThreshold    = dynamicThreshold,
            stanceSwingRatio = stanceSwing,
            verticalAccWindow = filteredVert,
            stepTimestamps   = stepTimestamps,
            featureVector    = normalized
        )
    }

    // ── Step detection ──────────────────────────────────────────────────────

    private fun computeDynamicThreshold(signal: FloatArray): Float {
        val peaks = signal.filter { it > 0f }.sortedDescending().take(10)
        if (peaks.isNotEmpty()) {
            val avgPeak = peaks.average().toFloat()
            peakHistory.addLast(avgPeak)
            if (peakHistory.size > 30) peakHistory.removeFirst()
        }
        val rollingMean = if (peakHistory.isNotEmpty()) peakHistory.average().toFloat() else 9.8f
        return rollingMean * GaitConstants.PEAK_THRESHOLD_MULT
    }

    private fun detectSteps(
        signal: FloatArray,
        threshold: Float,
        windowStartMs: Long
    ): List<Long> {
        val steps = mutableListOf<Long>()
        val sampleMs = 1000L / GaitConstants.SAMPLE_RATE_HZ

        for (i in 1 until signal.size) {
            val t = windowStartMs + i * sampleMs
            val above = signal[i] > threshold
            // Upward threshold crossing
            if (above && !wasAboveThreshold) {
                val elapsed = t - lastStepTimeMs
                if (elapsed >= GaitConstants.MIN_STEP_INTERVAL_MS) {
                    steps.add(t)
                    lastStepTimeMs = t
                }
            }
            wasAboveThreshold = above
            prevSample = signal[i]
        }
        return steps
    }

    private fun computeStepIntervals(timestamps: List<Long>): List<Float> {
        if (timestamps.size < 2) return emptyList()
        return (1 until timestamps.size).map { (timestamps[it] - timestamps[it - 1]).toFloat() }
    }

    private fun computeSymmetryIndex(intervals: List<Float>): Float {
        if (intervals.size < 2) return 0f
        val odd  = intervals.filterIndexed { i, _ -> i % 2 == 0 }
        val even = intervals.filterIndexed { i, _ -> i % 2 != 0 }
        if (odd.isEmpty() || even.isEmpty()) return 0f
        val meanOdd  = odd.average().toFloat()
        val meanEven = even.average().toFloat()
        val meanAll  = intervals.average().toFloat()
        return if (meanAll > 0f) abs(meanOdd - meanEven) / meanAll * 100f else 0f
    }

    private fun computeLateralSymmetryIndex(latAcc: FloatArray): Float {
        if (latAcc.isEmpty()) return 0f
        val leftEnergy = latAcc.filter { it < 0f }.map { abs(it) }.averageOrNull() ?: 0f
        val rightEnergy = latAcc.filter { it > 0f }.map { abs(it) }.averageOrNull() ?: 0f
        val mean = (leftEnergy + rightEnergy) / 2f
        return if (mean > 1e-3f) (abs(leftEnergy - rightEnergy) / mean * 100f).coerceIn(0f, 100f) else 0f
    }

    // ── Trunk / arm features ────────────────────────────────────────────────

    private fun computeTrunkTilt(
        pitchGyro: FloatArray,
        steps: List<Long>,
        windowStartMs: Long
    ): Float {
        if (steps.size < 2) return 0f
        val sampleMs = 1000.0 / GaitConstants.SAMPLE_RATE_HZ
        val strideMs = (steps.last() - steps.first()).toFloat() / (steps.size - 1)
        val strideSamples = (strideMs / sampleMs).toInt().coerceAtLeast(1)
        // Integrate pitch gyro over one stride (trapezoidal)
        var integral = 0f
        for (i in 1 until minOf(strideSamples, pitchGyro.size)) {
            integral += (pitchGyro[i] + pitchGyro[i - 1]) * 0.5f * (1f / GaitConstants.SAMPLE_RATE_HZ)
        }
        return Math.toDegrees(integral.toDouble()).toFloat()
    }

    private fun computeArmSwing(yawGyro: FloatArray): Float {
        return if (yawGyro.isEmpty()) 0f
        else yawGyro.max() - yawGyro.min()
    }

    // ── Statistics helpers ──────────────────────────────────────────────────

    private fun List<Float>.std(): Float {
        val mean = average().toFloat()
        return sqrt(sumOf { ((it - mean) * (it - mean)).toDouble() }.toFloat() / size)
    }

    private fun List<Float>.averageOrNull(): Float? =
        if (isEmpty()) null else average().toFloat()
}
