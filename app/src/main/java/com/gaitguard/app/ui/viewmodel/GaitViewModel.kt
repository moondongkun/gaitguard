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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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
