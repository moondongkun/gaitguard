package com.gaitguard.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.gaitguard.app.data.model.TerrainType
import com.gaitguard.app.util.GaitConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val TAG = "GaitSensorManager"

class GaitSensorManager(context: Context) : DefaultLifecycleObserver {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val ringBuffer      = RingBuffer()
    val aligner         = CoordinateAligner()
    val signalProcessor = SignalProcessor()
    val terrainClassifier = TerrainClassifier()
    val fallDetector    = FallDetector { event -> _fallEventFlow.tryEmit(event) }

    private val _terrainFlow = MutableStateFlow<TerrainType>(TerrainType.FLAT)
    val terrainFlow: StateFlow<TerrainType> = _terrainFlow

    // Task #2 fix: SharedFlow never drops events (StateFlow would coalesce identical events)
    private val _fallEventFlow = MutableSharedFlow<FallEvent>(extraBufferCapacity = 16)
    val fallEventFlow: SharedFlow<FallEvent> = _fallEventFlow.asSharedFlow()

    // Task #6 fix: dedicated thread so signal processing never blocks the sensor callback
    private val processingThread = HandlerThread("GaitProcessing").also { it.start() }
    private val processingHandler = Handler(processingThread.looper)

    // Raw sample queues for timestamp alignment
    private val accQueue  = ArrayDeque<Pair<Long, FloatArray>>()  // (nanos, [x,y,z])
    private val gyroQueue = ArrayDeque<Pair<Long, FloatArray>>()

    private var samplesSinceLastProcess = 0

    // Callback invoked every 2 seconds with the processed window
    var onWindowProcessed: ((ProcessedFeatures) -> Unit)? = null

    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val terrainRunnable = object : Runnable {
        override fun run() {
            val window = ringBuffer.copyWindow(GaitConstants.RING_BUFFER_CAPACITY)
            if (window.isNotEmpty()) {
                _terrainFlow.value = terrainClassifier.classify(window)
            }
            if (isRunning) mainHandler.postDelayed(this, GaitConstants.TERRAIN_UPDATE_SEC * 1000L)
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            val ts = event.timestamp  // nanoseconds
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    aligner.updateGravity(event.values)
                    accQueue.addLast(Pair(ts, event.values.copyOf()))
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroQueue.addLast(Pair(ts, event.values.copyOf()))
                }
            }
            // Try to match and push a combined sample
            tryAlignAndPush()
        }
    }

    private fun tryAlignAndPush() {
        // Simple nearest-timestamp matching
        while (accQueue.isNotEmpty() && gyroQueue.isNotEmpty()) {
            val (accTs, accVals) = accQueue.first()
            val (gyroTs, gyroVals) = gyroQueue.first()

            // If timestamps are within 10ms, pair them
            val diffMs = Math.abs(accTs - gyroTs) / 1_000_000L
            when {
                diffMs <= 10L -> {
                    accQueue.removeFirst()
                    gyroQueue.removeFirst()
                    val aligned = aligner.align(accVals, gyroVals)
                    ringBuffer.push(aligned)
                    fallDetector.processSample(aligned, System.currentTimeMillis())

                    samplesSinceLastProcess++
                    if (samplesSinceLastProcess >= GaitConstants.WINDOW_OVERLAP &&
                        ringBuffer.count >= GaitConstants.WINDOW_SAMPLES) {
                        triggerProcessing()
                        samplesSinceLastProcess = 0
                    }
                }
                accTs < gyroTs -> accQueue.removeFirst()   // acc too old
                else           -> gyroQueue.removeFirst()   // gyro too old
            }
        }
        // Prevent unbounded queue growth
        while (accQueue.size > 100)  accQueue.removeFirst()
        while (gyroQueue.size > 100) gyroQueue.removeFirst()
    }

    private fun triggerProcessing() {
        // Snapshot the ring buffer on the sensor thread (fast array copy), then
        // do the heavy signal processing on the dedicated background thread.
        val window = ringBuffer.copyWindow(GaitConstants.WINDOW_SAMPLES)
        if (window.size < GaitConstants.WINDOW_SAMPLES) return
        processingHandler.post {
            val features = signalProcessor.process(window, System.currentTimeMillis())
            onWindowProcessed?.invoke(features)
        }
    }

    fun startSession() {
        if (isRunning) return
        isRunning = true
        ringBuffer.clear()
        signalProcessor.reset()
        fallDetector.reset()
        samplesSinceLastProcess = 0
        registerSensors()
        mainHandler.postDelayed(terrainRunnable, GaitConstants.TERRAIN_UPDATE_SEC * 1000L)
        Log.i(TAG, "Session started")
    }

    fun stopSession() {
        isRunning = false
        sensorManager.unregisterListener(sensorListener)
        mainHandler.removeCallbacks(terrainRunnable)
        Log.i(TAG, "Session stopped")
    }

    private fun registerSensors() {
        accSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: Log.w(TAG, "Accelerometer not available")
        gyroSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: Log.w(TAG, "Gyroscope not available")
    }

    fun release() {
        stopSession()
        processingThread.quitSafely()
    }

    override fun onResume(owner: LifecycleOwner) { /* startSession called explicitly */ }
    override fun onPause(owner: LifecycleOwner) { if (isRunning) stopSession() }
}
