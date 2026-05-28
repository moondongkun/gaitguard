package com.gaitguard.app.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.gaitguard.app.data.db.GaitDatabase
import com.gaitguard.app.data.db.PostureSnapshot
import com.gaitguard.app.data.model.PostureResult
import com.gaitguard.app.ml.PostureAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class PostureRepository(private val context: Context) {

    private val db = GaitDatabase.getInstance(context)
    val analyzer = PostureAnalyzer()

    val postureResultFlow: StateFlow<PostureResult> = analyzer.postureFlow

    val recentSnapshots: Flow<List<PostureSnapshot>> = db.postureDao().getRecentSnapshots(50)
    val allRecords: Flow<List<PostureSnapshot>>      = db.postureDao().getAllRecords(50)
    val photoRecords: Flow<List<PostureSnapshot>>    = db.postureDao().getPhotoRecords(30)

    /**
     * Persist a posture analysis result.
     * @param sourceUri  Null for live-camera captures; the content URI string for gallery photos.
     */
    suspend fun saveSnapshot(result: PostureResult, sourceUri: String? = null): Long {
        val snapshot = PostureSnapshot(
            timestamp         = result.timestamp,
            shoulderLevelDeg  = result.shoulderLevelDeg,
            kyphosisAngleDeg  = result.kyphosisAngleDeg,
            pelvicTiltDeg     = result.pelvicTiltDeg,
            asymmetryIndex    = result.asymmetryIndex,
            axialAligned      = result.axialAligned,
            spinalHealthScore = result.spinalHealthScore,
            clinicalGrade     = result.clinicalGrade,
            sourceUri         = sourceUri
        )
        return db.postureDao().insertSnapshot(snapshot)
    }

    suspend fun deleteSnapshot(id: Long) {
        db.postureDao().deleteSnapshot(id)
    }

    suspend fun clearAllSnapshots() {
        db.postureDao().deleteAllSnapshots()
    }

    /**
     * Analyze a gallery / captured image from a content [uri].
     * Uses [InputImage.fromFilePath] which handles EXIF rotation automatically.
     * Updates [postureResultFlow] so the PostureScreen overlay reflects the photo result.
     * Returns the computed [PostureResult] (avgConfidence == 0 if no pose found).
     */
    suspend fun analyzeStaticImage(uri: Uri): PostureResult {
        return try {
            val bitmap = loadUprightBitmap(uri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            analyzer.analyzeStaticImage(inputImage, bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e("PostureRepository", "Static image analysis failed: ${e.message}")
            PostureResult(avgConfidence = 0f)
        }
    }

    private fun loadUprightBitmap(uri: Uri): android.graphics.Bitmap {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Unable to decode image")
        val rotation = context.contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
