package com.gaitguard.app.ml

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.gaitguard.app.data.model.GaitResult
import com.gaitguard.app.sensor.ProcessedFeatures
import com.gaitguard.app.util.GaitConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil

private const val TAG = "GaitInferenceEngine"

class GaitInferenceEngine(private val context: Context) {

    private val _resultFlow = MutableStateFlow<GaitResult>(GaitResult())
    val resultFlow: StateFlow<GaitResult> = _resultFlow

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isModelLoaded = false
    var warmUpTimeMs = 0L
    var lastInferenceLatencyMs = 0L

    // Pre-allocated input / output buffers — allocated once, reused per inference
    private val inputA = Array(1) { Array(GaitConstants.WINDOW_SAMPLES) { FloatArray(6) } }
    private val inputB = Array(1) { FloatArray(12) }
    private val outputFatigue   = Array(1) { FloatArray(1) }
    private val outputFallRisk  = Array(1) { FloatArray(3) }
    private val outputSymmetry  = Array(1) { FloatArray(1) }

    private val handlerThread = HandlerThread("GaitInference").also { it.start() }
    private val handler = Handler(handlerThread.looper)

    fun initialize() {
        handler.post {
            val warmStart = System.currentTimeMillis()
            try {
                val modelBuffer = FileUtil.loadMappedFile(context, "gait_model.tflite")
                val options = Interpreter.Options()

                // GPU delegate — fall back gracefully.
                // GpuDelegate() can throw NoClassDefFoundError (an Error, not Exception)
                // when GpuDelegateFactory$Options is missing on the device, so we must
                // catch Throwable here rather than just Exception.
                try {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate!!)
                    Log.i(TAG, "GPU delegate enabled")
                } catch (e: Throwable) {
                    Log.w(TAG, "GPU delegate unavailable: ${e.message} — using CPU")
                    gpuDelegate = null
                    options.numThreads = 2
                }

                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                warmUpTimeMs = System.currentTimeMillis() - warmStart
                Log.i(TAG, "Model loaded in ${warmUpTimeMs}ms")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load TFLite model: ${e.message}")
                isModelLoaded = false
            }
        }
    }

    /**
     * Queue inference for the given processed features.
     * Runs on the dedicated HandlerThread; result posted to [resultFlow].
     */
    fun runInference(features: ProcessedFeatures) {
        if (!isModelLoaded) {
            // Post a degraded result driven only by signal features
            _resultFlow.value = buildResultFromSignalOnly(features)
            return
        }
        handler.post {
            val start = System.currentTimeMillis()
            try {
                // Fill input A: raw IMU window [1 x 200 x 6]
                val window = features.verticalAccWindow
                for (i in inputA[0].indices) {
                    // If window is smaller than 200, pad with zeros
                    inputA[0][i].fill(0f)
                    if (i < window.size) inputA[0][i][2] = window[i]
                }

                // Fill input B: Z-score normalized 12 features [1 x 12]
                features.featureVector.copyInto(inputB[0])

                // Run inference. Model input order is 0 = hand features, 1 = IMU window.
                val outputs = mutableMapOf<Int, Any>(
                    0 to outputFatigue,
                    1 to outputFallRisk,
                    2 to outputSymmetry
                )
                interpreter?.runForMultipleInputsOutputs(arrayOf(inputB, inputA), outputs)

                lastInferenceLatencyMs = System.currentTimeMillis() - start

                val result = GaitResult(
                    cadenceSpm          = features.cadenceSpm,
                    isiMean             = features.isiMean,
                    isiCvPercent        = features.isiCvPercent,
                    symmetryScore       = features.symmetryScore,
                    harmonicRatio       = features.harmonicRatio,
                    dominantFreqHz      = features.dominantFreqHz,
                    bandEnergy          = features.bandEnergy,
                    trunkTiltDeg        = features.trunkTiltDeg,
                    armSwingProxy       = features.armSwingProxy,
                    stanceSwingRatio    = features.stanceSwingRatio,
                    tcnFatigue          = outputFatigue[0][0],
                    fallRiskLow         = outputFallRisk[0][0],
                    fallRiskMedium      = outputFallRisk[0][1],
                    fallRiskHigh        = outputFallRisk[0][2],
                    verticalAccWindow   = features.verticalAccWindow,
                    stepTimestamps      = features.stepTimestamps,
                    inferenceLatencyMs  = lastInferenceLatencyMs,
                    isModelLoaded       = true
                )
                _resultFlow.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Inference error: ${e.message}")
                _resultFlow.value = buildResultFromSignalOnly(features)
            }
        }
    }

    private fun buildResultFromSignalOnly(f: ProcessedFeatures) = GaitResult(
        cadenceSpm       = f.cadenceSpm,
        isiMean          = f.isiMean,
        isiCvPercent     = f.isiCvPercent,
        symmetryScore    = f.symmetryScore,
        harmonicRatio    = f.harmonicRatio,
        dominantFreqHz   = f.dominantFreqHz,
        bandEnergy       = f.bandEnergy,
        trunkTiltDeg     = f.trunkTiltDeg,
        armSwingProxy    = f.armSwingProxy,
        verticalAccWindow = f.verticalAccWindow,
        stepTimestamps   = f.stepTimestamps,
        isModelLoaded    = false
    )

    fun release() {
        handler.post {
            interpreter?.close(); interpreter = null
            gpuDelegate?.close(); gpuDelegate = null
        }
        handlerThread.quitSafely()
    }
}
