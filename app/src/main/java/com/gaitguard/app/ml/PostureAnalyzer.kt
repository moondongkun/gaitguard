package com.gaitguard.app.ml

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.gaitguard.app.data.model.LandmarkPoint
import com.gaitguard.app.data.model.PostureResult
import com.gaitguard.app.util.GaitConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

private const val TAG = "PostureAnalyzer"
private const val FRAME_INTERVAL_MS = 1000L / GaitConstants.POSTURE_FPS

class PostureAnalyzer : ImageAnalysis.Analyzer {

    private val _postureFlow = MutableStateFlow<PostureResult>(PostureResult())
    val postureFlow: StateFlow<PostureResult> = _postureFlow

    private var lastProcessedMs = 0L

    private val detector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        // 15 FPS frame gating — drop frames when analyzer is busy
        if (now - lastProcessedMs < FRAME_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastProcessedMs = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val width = imageProxy.width
        val height = imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                val landmarks = pose.allPoseLandmarks
                if (landmarks.isEmpty()) {
                    _postureFlow.value = PostureResult(avgConfidence = 0f)
                } else {
                    val filtered = landmarks.filter { it.inFrameLikelihood > GaitConstants.POSTURE_CONFIDENCE }
                    val avgConf = landmarks.map { it.inFrameLikelihood }.average().toFloat()
                    val result = computeMetrics(filtered, avgConf, width, height, landmarks)
                    _postureFlow.value = result
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Pose detection failed", e) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun computeMetrics(
        filtered: List<PoseLandmark>,
        avgConf: Float,
        width: Int,
        height: Int,
        all: List<PoseLandmark>
    ): PostureResult {
        fun landmark(type: Int) = all.find { it.landmarkType == type }

        val ls = landmark(PoseLandmark.LEFT_SHOULDER)
        val rs = landmark(PoseLandmark.RIGHT_SHOULDER)
        val lh = landmark(PoseLandmark.LEFT_HIP)
        val rh = landmark(PoseLandmark.RIGHT_HIP)
        val lk = landmark(PoseLandmark.LEFT_KNEE)
        val rk = landmark(PoseLandmark.RIGHT_KNEE)
        val nose = landmark(PoseLandmark.NOSE)
        val la = landmark(PoseLandmark.LEFT_ANKLE)
        val ra = landmark(PoseLandmark.RIGHT_ANKLE)

        // 1. Shoulder level
        val shoulderLevel = if (ls != null && rs != null)
            atan2(ls.position.y - rs.position.y, rs.position.x - ls.position.x).toFloat() * (180f / PI.toFloat())
        else 0f

        // 2. Kyphosis angle — law of cosines at hip (shoulder–hip–knee)
        val midShoulder = if (ls != null && rs != null)
            floatArrayOf((ls.position.x + rs.position.x) / 2f, (ls.position.y + rs.position.y) / 2f) else null
        val midHip = if (lh != null && rh != null)
            floatArrayOf((lh.position.x + rh.position.x) / 2f, (lh.position.y + rh.position.y) / 2f) else null
        val midKnee = if (lk != null && rk != null)
            floatArrayOf((lk.position.x + rk.position.x) / 2f, (lk.position.y + rk.position.y) / 2f) else null

        val kyphosis = if (midShoulder != null && midHip != null && midKnee != null)
            lawOfCosinesAngle(midShoulder, midHip, midKnee)
        else 0f

        // 3. Pelvic tilt
        val pelvicTilt = if (lh != null && rh != null)
            atan2(lh.position.y - rh.position.y, rh.position.x - lh.position.x).toFloat() * (180f / PI.toFloat())
        else 0f

        // 4. Asymmetry index
        val asymmetry = ((abs(shoulderLevel) / 5f * 50f) + (abs(pelvicTilt) / 4f * 50f))
            .coerceIn(0f, 100f)

        // 5. Axial alignment
        val frameWidthPct = width * 0.03f
        val midShoulderX = midShoulder?.get(0) ?: 0f
        val midHipX      = midHip?.get(0) ?: 0f
        val noseX        = nose?.position?.x ?: midShoulderX
        val midAnkleX    = if (la != null && ra != null) (la.position.x + ra.position.x) / 2f else midHipX
        val refX         = midShoulderX
        val axialAligned = listOf(noseX, midHipX, midAnkleX)
            .all { abs(it - refX) < frameWidthPct }

        val lmPoints = all.map { lm ->
            LandmarkPoint(
                id = lm.landmarkType,
                x = lm.position.x / width,
                y = lm.position.y / height,
                confidence = lm.inFrameLikelihood
            )
        }

        return PostureResult(
            shoulderLevelDeg = shoulderLevel,
            kyphosisAngleDeg = kyphosis,
            pelvicTiltDeg    = pelvicTilt,
            asymmetryIndex   = asymmetry,
            axialAligned     = axialAligned,
            avgConfidence    = avgConf,
            landmarks        = lmPoints,
            frameWidth       = width,
            frameHeight      = height
        )
    }

    private fun lawOfCosinesAngle(a: FloatArray, vertex: FloatArray, b: FloatArray): Float {
        val va = floatArrayOf(a[0] - vertex[0], a[1] - vertex[1])
        val vb = floatArrayOf(b[0] - vertex[0], b[1] - vertex[1])
        val dot = va[0] * vb[0] + va[1] * vb[1]
        val magA = sqrt(va[0] * va[0] + va[1] * va[1])
        val magB = sqrt(vb[0] * vb[0] + vb[1] * vb[1])
        return if (magA < 1e-6f || magB < 1e-6f) 0f
        else acos((dot / (magA * magB)).coerceIn(-1f, 1f)).toFloat() * (180f / PI.toFloat())
    }

    /**
     * Runs pose detection on a static [InputImage] (from gallery or captured bitmap).
     * Updates [postureFlow] with the result so the UI reflects it immediately.
     * Suspends until ML Kit returns or throws.
     */
    suspend fun analyzeStaticImage(inputImage: InputImage, width: Int, height: Int): PostureResult =
        suspendCancellableCoroutine { cont ->
            detector.process(inputImage)
                .addOnSuccessListener { pose ->
                    val landmarks = pose.allPoseLandmarks
                    val result = if (landmarks.isEmpty()) {
                        PostureResult(avgConfidence = 0f)
                    } else {
                        val filtered = landmarks.filter {
                            it.inFrameLikelihood > GaitConstants.POSTURE_CONFIDENCE
                        }
                        val avgConf = landmarks.map { it.inFrameLikelihood }.average().toFloat()
                        computeMetrics(filtered, avgConf, width, height, landmarks)
                    }
                    _postureFlow.value = result
                    if (cont.isActive) cont.resume(result)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Static image analysis failed", e)
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    fun close() { detector.close() }
}
