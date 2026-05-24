package com.gaitguard.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.gaitguard.app.data.db.PostureSnapshot
import com.gaitguard.app.data.model.PostureResult
import com.gaitguard.app.data.repository.PostureRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.abs

class PostureViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PostureRepository(app.applicationContext)
    val postureFlow: StateFlow<PostureResult> = repository.postureResultFlow

    // ── Weekly posture trend ───────────────────────────────────────────────
    private val _weeklyPostureScores = MutableStateFlow<List<Float>>(emptyList())
    val weeklyPostureScores: StateFlow<List<Float>> = _weeklyPostureScores

    private val _lastSnapshotTime = MutableStateFlow<Long?>(null)
    val lastSnapshotTime: StateFlow<Long?> = _lastSnapshotTime

    val recentSnapshots: StateFlow<List<PostureSnapshot>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val photoRecords: StateFlow<List<PostureSnapshot>> = repository.photoRecords
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Gallery analysis state ────────────────────────────────────────────
    private val _isAnalyzingGallery = MutableStateFlow(false)
    val isAnalyzingGallery: StateFlow<Boolean> = _isAnalyzingGallery

    // ── Captured result (frozen at capture time, shown on result screen) ──
    private val _capturedResult = MutableStateFlow<PostureResult?>(null)
    val capturedResult: StateFlow<PostureResult?> = _capturedResult

    private val _capturedPhotoUri = MutableStateFlow<String?>(null)
    val capturedPhotoUri: StateFlow<String?> = _capturedPhotoUri

    // ── Capture event (triggers navigation to result screen) ─────────────
    val captureEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // ── Countdown timer (2 / 5 / 10 s before auto-capture) ───────────────
    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            repository.recentSnapshots.collect { snapshots ->
                _lastSnapshotTime.value = snapshots.firstOrNull()?.timestamp
                val now   = System.currentTimeMillis()
                val dayMs = 24L * 60 * 60 * 1000
                _weeklyPostureScores.value = (6 downTo 0).map { daysAgo ->
                    val start = now - (daysAgo + 1) * dayMs
                    val end   = now - daysAgo * dayMs
                    val avg   = snapshots
                        .filter { it.timestamp in start..end }
                        .map { snapshotHealthScore(it) }
                        .average()
                    if (avg.isNaN()) 0f else avg.toFloat()
                }
            }
        }
    }

    private fun snapshotHealthScore(s: PostureSnapshot): Float {
        if (s.spinalHealthScore > 0) return s.spinalHealthScore.toFloat()
        val k  = when { s.kyphosisAngleDeg > 50f -> 30f; s.kyphosisAngleDeg > 40f -> 65f; else -> 100f }
        val sh = when { abs(s.shoulderLevelDeg) > 5f -> 35f; abs(s.shoulderLevelDeg) > 2f -> 70f; else -> 100f }
        val p  = when { abs(s.pelvicTiltDeg) > 4f -> 35f; abs(s.pelvicTiltDeg) > 2f -> 70f; else -> 100f }
        val a  = if (s.axialAligned) 100f else 50f
        return k * 0.35f + sh * 0.25f + p * 0.25f + a * 0.15f
    }

    // ── Camera ────────────────────────────────────────────────────────────
    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var boundPreviewView: PreviewView? = null

    val hasCameraPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (!hasCameraPermission) return
        val future = ProcessCameraProvider.getInstance(getApplication())
        future.addListener({
            cameraProvider = future.get()
            bindUseCases(lifecycleOwner, previewView)
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    private fun bindUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        boundPreviewView = previewView
        val provider = cameraProvider ?: return
        provider.unbindAll()
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, repository.analyzer) }
        try {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
        } catch (e: Exception) {
            Log.e("PostureViewModel", "Camera bind failed", e)
        }
    }

    fun flipCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        bindUseCases(lifecycleOwner, previewView)
    }

    fun unbindCamera() { cameraProvider?.unbindAll() }

    // ── Countdown ─────────────────────────────────────────────────────────

    fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        _countdown.value = seconds
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _countdown.value = if (remaining > 0) remaining else null
            }
            captureNow()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _countdown.value = null
    }

    // ── Capture ───────────────────────────────────────────────────────────

    /** Freeze current live-frame analysis, persist it, and fire navigation event. */
    fun captureNow() {
        viewModelScope.launch {
            val result = postureFlow.value
            val photoUri = saveCurrentPreviewBitmap(result.timestamp)
            _capturedResult.value = result
            _capturedPhotoUri.value = photoUri
            repository.saveSnapshot(result, sourceUri = photoUri)
            captureEvent.emit(Unit)
        }
    }

    private suspend fun saveCurrentPreviewBitmap(timestamp: Long): String? {
        val bitmap = boundPreviewView?.bitmap ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val picturesDir = getApplication<Application>()
                    .getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: getApplication<Application>().filesDir
                val postureDir = File(picturesDir, "posture").apply { mkdirs() }
                val file = File(postureDir, "posture_$timestamp.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                Uri.fromFile(file).toString()
            } catch (e: Exception) {
                Log.e("PostureViewModel", "Preview photo save failed", e)
                null
            }
        }
    }

    // ── Gallery / static image analysis ───────────────────────────────────

    fun loadGalleryImage(uri: Uri) {
        viewModelScope.launch {
            _isAnalyzingGallery.value = true
            try {
                val result = repository.analyzeStaticImage(uri)
                if (result.avgConfidence > 0.1f) {
                    val sourceUri = uri.toString()
                    repository.saveSnapshot(result, sourceUri = sourceUri)
                    _capturedResult.value = result
                    _capturedPhotoUri.value = sourceUri
                    captureEvent.emit(Unit)
                }
            } catch (e: Exception) {
                Log.e("PostureViewModel", "Gallery analysis failed", e)
            } finally {
                _isAnalyzingGallery.value = false
            }
        }
    }

    fun deletePhotoRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteSnapshot(id)
        }
    }

    override fun onCleared() {
        cancelCountdown()
        unbindCamera()
        repository.analyzer.close()
        analysisExecutor.shutdown()
    }
}
