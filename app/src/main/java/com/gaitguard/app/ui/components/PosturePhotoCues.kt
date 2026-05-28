package com.gaitguard.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gaitguard.app.data.model.LandmarkPoint
import com.gaitguard.app.data.model.MetricSeverity
import com.gaitguard.app.data.model.PostureResult
import com.gaitguard.app.ui.theme.BrandAmber
import com.gaitguard.app.ui.theme.BrandRed
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.gg
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.max

@Composable
fun PosturePhotoWithCues(
    photoUri: String?,
    result: PostureResult,
    modifier: Modifier = Modifier
) {
    val image = rememberPostureImage(photoUri)
    val t = MaterialTheme.gg

    if (image == null) {
        PosturePhotoPlaceholder(modifier.fillMaxWidth().aspectRatio(3f / 4f))
        return
    }

    val aspect = max(image.width.toFloat() / image.height.toFloat(), 0.6f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .border(1.dp, t.border, RoundedCornerShape(18.dp))
    ) {
        Image(
            bitmap = image,
            contentDescription = "Captured posture photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        PostureCueOverlay(result = result, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun PosturePhotoThumbnail(
    photoUri: String?,
    modifier: Modifier = Modifier
) {
    val image = rememberPostureImage(photoUri)
    if (image == null) {
        PosturePhotoPlaceholder(modifier)
        return
    }
    Image(
        bitmap = image,
        contentDescription = "Posture history photo",
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun PostureCueOverlay(result: PostureResult, modifier: Modifier = Modifier) {
    val points = remember(result.landmarks) { result.landmarks.associateBy { it.id } }
    val shoulderTone = severityColor(result.shoulderSeverity)
    val hipTone = severityColor(result.pelvicSeverity)
    val spineTone = when {
        result.kyphosisSeverity == MetricSeverity.ALERT || !result.axialAligned -> BrandRed
        result.kyphosisSeverity == MetricSeverity.WATCH -> BrandAmber
        else -> BrandTeal2
    }

    Canvas(modifier) {
        fun p(id: Int): Offset? = points[id]
            ?.takeIf { it.confidence > 0.25f }
            ?.let { Offset(it.x * size.width, it.y * size.height) }

        fun line(a: Int, b: Int, color: Color, width: Float = 5f) {
            val start = p(a) ?: return
            val end = p(b) ?: return
            drawLine(color = color.copy(alpha = 0.94f), start = start, end = end, strokeWidth = width)
        }

        line(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, shoulderTone, 7f)
        line(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, hipTone, 7f)
        line(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, spineTone, 5f)
        line(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, spineTone, 5f)
        line(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_HIP, spineTone.copy(alpha = 0.65f), 3f)
        line(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.LEFT_HIP, spineTone.copy(alpha = 0.65f), 3f)

        drawIdealGuide(points, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, BrandTeal2)
        drawIdealGuide(points, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, BrandTeal2)

        points.values
            .filter { it.confidence > 0.25f }
            .forEach { landmark ->
                val color = landmarkColor(landmark, result)
                val center = Offset(landmark.x * size.width, landmark.y * size.height)
                drawCircle(color = Color.White.copy(alpha = 0.92f), radius = 8f, center = center)
                drawCircle(color = color, radius = 5f, center = center)
            }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIdealGuide(
    points: Map<Int, LandmarkPoint>,
    leftId: Int,
    rightId: Int,
    color: Color
) {
    val left = points[leftId]?.takeIf { it.confidence > 0.25f } ?: return
    val right = points[rightId]?.takeIf { it.confidence > 0.25f } ?: return
    val y = ((left.y + right.y) / 2f) * size.height
    val x1 = left.x * size.width
    val x2 = right.x * size.width
    drawLine(
        color = color.copy(alpha = 0.36f),
        start = Offset(x1, y),
        end = Offset(x2, y),
        strokeWidth = 3f
    )
}

private fun landmarkColor(landmark: LandmarkPoint, result: PostureResult): Color {
    return when (landmark.id) {
        PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER -> severityColor(result.shoulderSeverity)
        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP -> severityColor(result.pelvicSeverity)
        PoseLandmark.LEFT_EAR, PoseLandmark.RIGHT_EAR,
        PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW -> severityColor(result.kyphosisSeverity)
        else -> if (result.axialAligned) BrandTeal2 else BrandRed
    }
}

private fun severityColor(severity: MetricSeverity): Color = when (severity) {
    MetricSeverity.NORMAL -> BrandTeal2
    MetricSeverity.WATCH -> BrandAmber
    MetricSeverity.ALERT -> BrandRed
}

@Composable
private fun rememberPostureImage(photoUri: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(photoUri) {
        loadPostureBitmap(context, photoUri)?.asImageBitmap()
    }
}

private fun loadPostureBitmap(context: Context, photoUri: String?): android.graphics.Bitmap? {
    if (photoUri.isNullOrBlank()) return null
    return try {
        val uri = Uri.parse(photoUri)
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return null
        val rotation = context.contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) bitmap else {
            val matrix = Matrix().apply { postRotate(rotation) }
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun PosturePhotoPlaceholder(modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(t.surfaceElev)
            .border(1.dp, t.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = t.ink3,
            modifier = Modifier.size(22.dp)
        )
    }
}
