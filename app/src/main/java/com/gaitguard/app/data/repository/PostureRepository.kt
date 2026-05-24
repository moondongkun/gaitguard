package com.gaitguard.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
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
            val inputImage = InputImage.fromFilePath(context, uri)
            analyzer.analyzeStaticImage(inputImage, inputImage.width, inputImage.height)
        } catch (e: Exception) {
            Log.e("PostureRepository", "Static image analysis failed: ${e.message}")
            PostureResult(avgConfidence = 0f)
        }
    }
}
