package com.gaitguard.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaitguard.app.data.db.SessionRecord
import com.gaitguard.app.data.model.AlertLevel
import com.gaitguard.app.data.model.GaitResult
import com.gaitguard.app.data.model.TerrainType
import com.gaitguard.app.data.repository.GaitRepository
import com.gaitguard.app.sensor.FallEvent
import com.gaitguard.app.util.GaitConstants
import com.gaitguard.app.util.PdfReportGenerator
import com.gaitguard.app.util.SmsHelper
import android.net.Uri
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/** Stages for the Engine page "Re-Optimize Model" action. */
enum class OptimizeStage { Idle, Pruning, Quantizing, WarmUp, Done }

/** Aggregated session data for the Metrics dashboard (Session Goal + Recent Activity). */
data class DashboardSummary(
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 40,
    val weekMinutes: Int = 0,
    val lastWeekMinutes: Int = 0,
    val dailyMinutes: List<Int> = List(7) { 0 }, // oldest → today (last 7 days)
    val totalHours: Float = 0f,
    val daysMonitored: Int = 0,
    val recent: List<SessionRecord> = emptyList()
) {
    val goalProgress: Float get() = if (goalMinutes <= 0) 0f else (todayMinutes / goalMinutes.toFloat()).coerceIn(0f, 1f)
    val weekDeltaPercent: Int get() =
        if (lastWeekMinutes <= 0) 0 else (((weekMinutes - lastWeekMinutes) / lastWeekMinutes.toFloat()) * 100f).toInt()
}

data class GaitUiState(
    val gaitResult: GaitResult = GaitResult(),
    val terrain: TerrainType = TerrainType.FLAT,
    val elapsedMs: Long = 0L,
    val isSessionActive: Boolean = false,
    val isPaused: Boolean = false,
    val alertLevel: AlertLevel = AlertLevel.SAFE,
    val showFallAlert: Boolean = false,
    val isCalibrating: Boolean = false,
    val calibrationSecondsLeft: Int = GaitConstants.BASELINE_DURATION_SEC,
    val liveWaveform: FloatArray = FloatArray(0),
    val markedEvents: List<Long> = emptyList(),
    val stumbleEvents: List<Long> = emptyList(),
    val microStumbleCount: Int = 0,
    val sessionFatigueTimeline: List<Pair<Long, Float>> = emptyList()
)

class GaitViewModel(app: Application) : AndroidViewModel(app) {

    val repository = GaitRepository(app.applicationContext)
    private val caregiverPrefs = app.getSharedPreferences("gaitguard_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GaitUiState())
    val uiState: StateFlow<GaitUiState> = _uiState

    private var timerJob: Job? = null
    private var calibrationJob: Job? = null
    private var highRiskJob: Job? = null
    private var alertPlayer: MediaPlayer? = null
    private var microStumbleCount = 0

    // ── Engine: re-optimize model ───────────────────────────────────────────
    private val _optimizeStage = MutableStateFlow(OptimizeStage.Idle)
    val optimizeStage: StateFlow<OptimizeStage> = _optimizeStage
    private var optimizeJob: Job? = null

    // ── Metrics dashboard summary (Session Goal + Recent Activity) ───────────
    val dashboard: StateFlow<DashboardSummary> = repository.allSessions
        .map { buildDashboard(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, DashboardSummary())

    private fun buildDashboard(sessions: List<SessionRecord>): DashboardSummary {
        if (sessions.isEmpty()) return DashboardSummary()
        val cal = Calendar.getInstance()
        // Local-midnight day index for a timestamp (timezone-correct, unlike epoch/dayMs).
        fun localMidnight(ts: Long): Long {
            cal.timeInMillis = ts
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        val dayMs = 24L * 60 * 60 * 1000
        val today0 = localMidnight(System.currentTimeMillis())
        // last-7-day buckets, index 6 == today
        val daily = IntArray(7)
        var lastWeek = 0
        sessions.forEach { s ->
            val daysAgo = ((today0 - localMidnight(s.date)) / dayMs).toInt()
            when {
                daysAgo in 0..6  -> daily[6 - daysAgo] += s.durationMinutes
                daysAgo in 7..13 -> lastWeek += s.durationMinutes
            }
        }
        val week = daily.sum()
        val distinctDays = sessions.map { localMidnight(it.date) }.distinct().size
        return DashboardSummary(
            todayMinutes = daily[6],
            goalMinutes = 40,
            weekMinutes = week,
            lastWeekMinutes = lastWeek,
            dailyMinutes = daily.toList(),
            totalHours = week / 60f,
            daysMonitored = distinctDays,
            recent = sessions.sortedByDescending { it.date }.take(5)
        )
    }

    /**
     * Reload the pruned + dynamic-range INT8 interpreter and re-benchmark it on-device,
     * driving a staged progress indicator (Pruning → Quantizing → Warm-up → Done).
     * The pruning/quantization artifact is produced offline (model/train.py); this
     * re-applies it and refreshes the live warm-up/latency readouts.
     */
    fun reOptimizeModel() {
        if (optimizeJob?.isActive == true) return
        optimizeJob = viewModelScope.launch {
            _optimizeStage.value = OptimizeStage.Pruning
            delay(900)
            _optimizeStage.value = OptimizeStage.Quantizing
            delay(900)
            _optimizeStage.value = OptimizeStage.WarmUp
            val done = kotlinx.coroutines.CompletableDeferred<Unit>()
            repository.inferenceEngine.reOptimize { done.complete(Unit) }
            done.await()
            delay(500)
            _optimizeStage.value = OptimizeStage.Done
            delay(1500)
            _optimizeStage.value = OptimizeStage.Idle
        }
    }

    init {
        viewModelScope.launch {
            combine(
                repository.gaitResultFlow,
                repository.terrainFlow
            ) { result, terrain ->
                result to terrain
            }.collect { (result, terrain) ->
                val prev = _uiState.value
                _uiState.value = prev.copy(
                    gaitResult   = result,
                    terrain      = terrain,
                    alertLevel   = result.alertLevel,
                    liveWaveform = result.verticalAccWindow,
                    sessionFatigueTimeline = repository.getFatigueTimeline()
                )
                handleAlerts(result)
            }
        }

        viewModelScope.launch {
            repository.fallEventFlow.collect { event ->
                when (event) {
                    is FallEvent.TrueFall -> handleTrueFall()
                    is FallEvent.MicroStumble -> handleMicroStumble()
                }
            }
        }
    }

    // ── Session control ──────────────────────────────────────────────────────

    fun startSession() {
        repository.startSession()
        _uiState.value = _uiState.value.copy(
            isSessionActive = true,
            isPaused = false,
            markedEvents = emptyList(),
            stumbleEvents = emptyList(),
            microStumbleCount = 0
        )
        microStumbleCount = 0
        startTimer()
    }

    fun pauseSession() {
        _uiState.value = _uiState.value.copy(isPaused = !_uiState.value.isPaused)
        if (_uiState.value.isPaused) timerJob?.cancel() else startTimer()
    }

    fun stopSession() {
        timerJob?.cancel()
        viewModelScope.launch {
            repository.stopSession()
        }
        _uiState.value = _uiState.value.copy(isSessionActive = false, isPaused = false, elapsedMs = 0L)
        stopAlertTone()
    }

    fun markEvent() {
        val now = System.currentTimeMillis()
        repository.markEvent()
        val events = _uiState.value.markedEvents + now
        _uiState.value = _uiState.value.copy(markedEvents = events)
    }

    suspend fun exportHealthReport(): Uri? {
        val sessions = repository.getAllSessionsSync()
        return PdfReportGenerator.generate(getApplication(), sessions, _uiState.value.gaitResult)
    }

    suspend fun clearAllData() {
        stopAlertTone()
        repository.clearAllData()
        timerJob?.cancel()
        microStumbleCount = 0
        _uiState.value = GaitUiState(gaitResult = repository.gaitResultFlow.value)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    elapsedMs = repository.getSessionDurationMs()
                )
                delay(1000L)
            }
        }
    }

    // ── Calibration ──────────────────────────────────────────────────────────

    fun startCalibration() {
        repository.startCalibration()
        _uiState.value = _uiState.value.copy(
            isCalibrating = true,
            calibrationSecondsLeft = GaitConstants.BASELINE_DURATION_SEC
        )
        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            for (s in GaitConstants.BASELINE_DURATION_SEC downTo 1) {
                _uiState.value = _uiState.value.copy(calibrationSecondsLeft = s)
                delay(1000L)
            }
            repository.completeCalibration()
            _uiState.value = _uiState.value.copy(isCalibrating = false, calibrationSecondsLeft = 0)
        }
    }

    fun isCalibrated() = repository.isCalibrated()
    fun resetBaseline() = repository.resetBaseline()

    // ── Alert handling ───────────────────────────────────────────────────────

    private fun handleAlerts(result: GaitResult) {
        when (result.alertLevel) {
            AlertLevel.RED -> {
                vibrate(longArrayOf(0, 200, 100, 200))
                stopAlertTone()
                highRiskJob?.cancel()
            }
            AlertLevel.AMBER -> {
                vibrate(longArrayOf(0, 150))
                stopAlertTone()
                highRiskJob?.cancel()
            }
            AlertLevel.SAFE -> {
                stopAlertTone()
                highRiskJob?.cancel()
            }
        }
    }

    private fun handleHighRiskSms(result: GaitResult) {
        if (highRiskJob?.isActive == true) return
        highRiskJob = viewModelScope.launch {
            delay(GaitConstants.HIGH_RISK_SMS_DELAY * 1000L)
            if (_uiState.value.alertLevel == AlertLevel.RED) {
                sendEmergencySms("High fall risk sustained for 30 seconds")
                _uiState.value = _uiState.value.copy(showFallAlert = true)
            }
        }
    }

    private fun handleTrueFall() {
        repository.onTrueFall()
        vibrate(longArrayOf(0, 500, 100, 500, 100, 500))
        if (alertPlayer == null) startAlertTone()
        _uiState.value = _uiState.value.copy(showFallAlert = true)
        viewModelScope.launch {
            sendEmergencySms("Fall detected")
        }
    }

    private fun handleMicroStumble() {
        microStumbleCount++
        repository.onMicroStumble()
        _uiState.value = _uiState.value.copy(
            microStumbleCount = microStumbleCount,
            stumbleEvents = _uiState.value.stumbleEvents + System.currentTimeMillis()
        )
    }

    fun dismissFallAlert() {
        _uiState.value = _uiState.value.copy(showFallAlert = false)
        highRiskJob?.cancel()
    }

    private suspend fun sendEmergencySms(reason: String) {
        val phone = caregiverPrefs.getString("caregiver_phone", "") ?: return
        SmsHelper.sendEmergencySms(getApplication(), phone, reason)
    }

    // ── Vibration ────────────────────────────────────────────────────────────

    private fun vibrate(pattern: LongArray) {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    // ── Audio alert ──────────────────────────────────────────────────────────

    private fun startAlertTone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            alertPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(getApplication<Application>(), uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {}
    }

    private fun stopAlertTone() {
        alertPlayer?.apply { if (isPlaying) stop(); release() }
        alertPlayer = null
    }

    override fun onCleared() {
        // Task #4: save session data before the ViewModel is destroyed
        if (uiState.value.isSessionActive) {
            viewModelScope.launch { repository.stopSession() }
        }
        stopAlertTone()
        repository.inferenceEngine.release()
        repository.release()          // Task #5: cancel background scope + sensor thread
        timerJob?.cancel()
        calibrationJob?.cancel()
        highRiskJob?.cancel()
        super.onCleared()
    }
}
