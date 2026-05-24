package com.gaitguard.app.data.repository

import android.content.Context
import com.gaitguard.app.data.db.FatigueTimelinePoint
import com.gaitguard.app.data.db.GaitDatabase
import com.gaitguard.app.data.db.SessionRecord
import com.gaitguard.app.data.model.AlertLevel
import com.gaitguard.app.data.model.GaitResult
import com.gaitguard.app.data.model.TerrainType
import com.gaitguard.app.ml.GaitInferenceEngine
import com.gaitguard.app.sensor.BaselineCalibrator
import com.gaitguard.app.sensor.FallEvent
import com.gaitguard.app.sensor.FatigueScoreCalculator
import com.gaitguard.app.sensor.GaitSensorManager
import com.gaitguard.app.sensor.PersonalAdaptationEngine
import com.gaitguard.app.sensor.ProcessedFeatures
import com.gaitguard.app.util.GaitConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.abs

private data class TimelineSample(
    val timestampMs: Long,
    val result: GaitResult
)

class GaitRepository(private val context: Context) {

    private val db = GaitDatabase.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sensorManager    = GaitSensorManager(context)
    val inferenceEngine  = GaitInferenceEngine(context)
    val baselineCalibrator = BaselineCalibrator(context)

    private val _gaitResultFlow = MutableStateFlow<GaitResult>(GaitResult())
    val gaitResultFlow: StateFlow<GaitResult> = _gaitResultFlow

    private val _fallEventFlow = MutableSharedFlow<FallEvent>(extraBufferCapacity = 10)
    val fallEventFlow: SharedFlow<FallEvent> = _fallEventFlow.asSharedFlow()

    val terrainFlow: StateFlow<TerrainType> = sensorManager.terrainFlow

    // Session state
    private var sessionStartMs = 0L
    private var isSessionActive = false
    private var peakFatigue = 0f
    private var fatigueOnsetMs = -1L
    private var symScoreSum = 0f
    private var symSampleCount = 0
    private var fallRiskEventCount = 0
    private var microStumbleCount = 0

    // Fatigue onset detection state
    private var onsetWindowCount = 0
    private var onsetTriggered = false

    // Session timeline (for analytics)
    private val fatigueTimeline = mutableListOf<TimelineSample>()
    private val markedEventTimes = mutableListOf<Long>()
    private val stumbleEventTimes = mutableListOf<Long>()

    val allSessions: Flow<List<SessionRecord>> = db.sessionDao().getAllSessions()
    val highFatigueSessions: Flow<List<SessionRecord>> = db.sessionDao().getHighFatigueSessions(40f)
    val recentSessions: Flow<List<SessionRecord>> = db.sessionDao().getRecentSessions(10)

    init {
        inferenceEngine.initialize()

        // Wire sensor → inference → composite fatigue
        sensorManager.onWindowProcessed = { features ->
            onFeaturesReady(features)
        }

        // Wire fall events — SharedFlow on sensorManager side means no event is dropped
        scope.launch {
            sensorManager.fallEventFlow.collect { event ->
                _fallEventFlow.emit(event)
            }
        }

        // Observe inference results and compute composite score
        scope.launch {
            inferenceEngine.resultFlow.collect { inferenceResult ->
                val baseline = baselineCalibrator.getProfile()
                val components = FatigueScoreCalculator.compute(
                    tcnFatigue   = inferenceResult.tcnFatigue,
                    isiCvPercent = inferenceResult.isiCvPercent,
                    harmonicRatio = inferenceResult.harmonicRatio,
                    trunkTiltDeg  = inferenceResult.trunkTiltDeg
                )
                val finalResult = inferenceResult.copy(
                    fatigueScore = components.composite,
                    alertLevel   = components.alertLevel
                )
                _gaitResultFlow.value = finalResult
                if (isSessionActive) updateSessionStats(finalResult)
            }
        }
    }

    private fun onFeaturesReady(features: ProcessedFeatures) {
        // Update signal processor baseline
        val profile = baselineCalibrator.getProfile()
        sensorManager.signalProcessor.baselineMean = profile.mean
        sensorManager.signalProcessor.baselineStd  = profile.std

        // Feed baseline samples during calibration
        if (isCalibrating) {
            baselineCalibrator.addSample(features.featureVector)
        }

        inferenceEngine.runInference(features)
    }

    // ── Calibration ─────────────────────────────────────────────────────────

    private var isCalibrating = false

    fun startCalibration() {
        isCalibrating = true
        sensorManager.startSession()
    }

    fun completeCalibration() {
        isCalibrating = false
        sensorManager.stopSession()
        baselineCalibrator.complete()
    }

    fun isCalibrated() = baselineCalibrator.isCalibrated()
    fun resetBaseline() = baselineCalibrator.reset()

    // ── Session control ──────────────────────────────────────────────────────

    fun startSession() {
        if (isSessionActive) return
        isSessionActive = true
        sessionStartMs = System.currentTimeMillis()
        peakFatigue = 0f
        fatigueOnsetMs = -1L
        symScoreSum = 0f
        symSampleCount = 0
        fallRiskEventCount = 0
        microStumbleCount = 0
        fatigueTimeline.clear()
        markedEventTimes.clear()
        stumbleEventTimes.clear()
        onsetWindowCount = 0
        onsetTriggered = false
        sensorManager.startSession()
    }

    suspend fun stopSession(): SessionRecord? {
        if (!isSessionActive) return null
        isSessionActive = false
        sensorManager.stopSession()

        val durationMs = System.currentTimeMillis() - sessionStartMs
        val durationMin = (durationMs / 60_000).toInt()
        val avgSym = if (symSampleCount > 0) symScoreSum / symSampleCount else 100f
        val onsetMin = if (fatigueOnsetMs > 0)
            ((fatigueOnsetMs - sessionStartMs) / 60_000).toInt() else -1

        val personalBest = db.sessionDao().getPersonalBestEndurance() ?: 60f
        val endurance = if (onsetMin > 0)
            (onsetMin.toFloat() / max(personalBest, 1f) * 100f).coerceIn(0f, 100f) else 100f

        val record = SessionRecord(
            date               = sessionStartMs,
            durationMinutes    = durationMin,
            peakFatigueScore   = peakFatigue,
            fatigueOnsetMinute = onsetMin,
            avgSymmetryScore   = avgSym,
            fallRiskEventCount = fallRiskEventCount,
            microStumbleCount  = microStumbleCount,
            enduranceScore     = endurance
        )
        val sessionId = db.sessionDao().insertSession(record)

        // Persist the fatigue timeline for Session Replay
        if (fatigueTimeline.isNotEmpty()) {
            val points = fatigueTimeline.map { sample ->
                val ts = sample.timestampMs
                val result = sample.result
                val elapsed = ((ts - sessionStartMs) / 1000L).toInt()
                FatigueTimelinePoint(
                    sessionId      = sessionId,
                    elapsedSeconds = elapsed,
                    fatigueScore   = result.fatigueScore,
                    symmetryScore  = result.symmetryScore,
                    cadenceSpm     = result.cadenceSpm,
                    isMarked       = markedEventTimes.any { abs(it - ts) <= 2_500L },
                    isStumble      = stumbleEventTimes.any { abs(it - ts) <= 2_500L }
                )
            }
            db.timelineDao().insertPoints(points)
        }

        // Run personalized adaptation with all sessions
        val allSessions = db.sessionDao().getAllSessionsSync()
        PersonalAdaptationEngine.adapt(context, allSessions)

        return record.copy(id = sessionId)
    }

    private fun updateSessionStats(result: GaitResult) {
        val now = System.currentTimeMillis()
        peakFatigue = max(peakFatigue, result.fatigueScore)
        symScoreSum += result.symmetryScore
        symSampleCount++

        fatigueTimeline.add(TimelineSample(now, result))

        // Fatigue onset detection: score > 40 for 30 consecutive seconds
        if (!onsetTriggered) {
            if (result.fatigueScore >= GaitConstants.FATIGUE_ONSET_THRESH) {
                onsetWindowCount++
                if (onsetWindowCount >= GaitConstants.FATIGUE_ONSET_WINDOW / 2) {
                    fatigueOnsetMs = now
                    onsetTriggered = true
                }
            } else if (result.fatigueScore < GaitConstants.FATIGUE_ONSET_HYSTER) {
                onsetWindowCount = 0
            }
        }

        // Count fall risk events
        if (result.fallRiskHigh > 0.5f) fallRiskEventCount++
    }

    fun markEvent() {
        if (isSessionActive) markedEventTimes.add(System.currentTimeMillis())
    }

    fun onMicroStumble() {
        microStumbleCount++
        if (isSessionActive) stumbleEventTimes.add(System.currentTimeMillis())
    }
    fun onTrueFall() { fallRiskEventCount++ }

    fun getFatigueTimeline(): List<Pair<Long, Float>> =
        fatigueTimeline.map { it.timestampMs to it.result.fatigueScore }
    fun getSessionDurationMs() = if (isSessionActive) System.currentTimeMillis() - sessionStartMs else 0L

    suspend fun getTimelineForSession(sessionId: Long) =
        db.timelineDao().getTimelineForSession(sessionId)

    suspend fun getAllSessionsSync(): List<SessionRecord> =
        db.sessionDao().getAllSessionsSync()

    suspend fun clearAllData() {
        if (isSessionActive) stopSession()
        db.timelineDao().deleteAll()
        db.sessionDao().deleteAllSessions()
        db.postureDao().deleteAllSnapshots()
        PersonalAdaptationEngine.adapt(context, emptyList())
    }

    /** Task #5: Cancel background scope and release sensor thread. Call from ViewModel.onCleared(). */
    fun release() {
        sensorManager.release()
        scope.cancel()
    }
}
