package com.gaitguard.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.pose.PoseLandmark
import com.gaitguard.app.data.model.MetricSeverity
import com.gaitguard.app.sensor.PostureCoach
import com.gaitguard.app.ui.components.GaitGuardLockup
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.components.pulsingAlpha
import com.gaitguard.app.ui.theme.BrandAmber
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandRed
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.PostureViewModel
import com.gaitguard.app.util.GaitConstants
import kotlin.math.abs

private val HEAD_CONNECTIONS  = listOf(0 to 11, 0 to 12)
private val UPPER_CONNECTIONS = listOf(11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16)
private val SPINE_CONNECTIONS = listOf(11 to 23, 12 to 24)
private val LOWER_CONNECTIONS = listOf(23 to 24, 23 to 25, 24 to 26, 25 to 27, 26 to 28)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostureScreen(
    viewModel: PostureViewModel,
    hasCameraPermission: Boolean,
    onNavigateToHealth: () -> Unit = {},
    onNavigateToResult: () -> Unit = {}
) {
    val postureResult      by viewModel.postureFlow.collectAsState()
    val isAnalyzingGallery by viewModel.isAnalyzingGallery.collectAsState()
    val countdown          by viewModel.countdown.collectAsState()
    val lifecycleOwner     = LocalLifecycleOwner.current

    var viewSize       by remember { mutableStateOf(IntSize.Zero) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    // Selected countdown delay: null = immediate, 2/5/10 = delayed
    var selectedDelay  by remember { mutableStateOf<Int?>(null) }
    var showHealthSheet by remember { mutableStateOf(false) }
    val healthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.loadGalleryImage(it) } }

    val coachCue = remember(postureResult) { PostureCoach.evaluate(postureResult).firstOrNull() }

    if (!hasCameraPermission) { CameraPermissionDenied(); return }

    // Navigate to result screen whenever a capture fires
    LaunchedEffect(Unit) {
        viewModel.captureEvent.collect { onNavigateToResult() }
    }
    DisposableEffect(lifecycleOwner) {
        onDispose { viewModel.unbindCamera() }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "scanY"
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0E14))) {
        if (showHealthSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHealthSheet = false },
                sheetState = healthSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                SpinalHealthQuickSheet(
                    score = postureResult.spinalHealthScore,
                    grade = postureResult.clinicalGrade,
                    shoulder = abs(postureResult.shoulderLevelDeg).toInt(),
                    kyphosis = postureResult.kyphosisAngleDeg.toInt(),
                    axialAligned = postureResult.axialAligned,
                    onOpenFull = {
                        showHealthSheet = false
                        onNavigateToHealth()
                    }
                )
            }
        }

        // ── Camera preview ───────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    previewViewRef = pv
                    viewModel.bindCamera(lifecycleOwner, pv)
                }
            },
            modifier = Modifier.fillMaxSize().onGloballyPositioned { viewSize = it.size }
        )

        // ── AR skeleton + scan bar ───────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (postureResult.landmarks.isEmpty() || viewSize == IntSize.Zero) return@Canvas
            val w = size.width; val h = size.height
            val lmMap = postureResult.landmarks.associateBy { it.id }

            val axialColor = if (postureResult.axialAligned) BrandTeal else BrandRed
            val upperColor = severityColor(postureResult.shoulderSeverity)
            val spineColor = severityColor(postureResult.kyphosisSeverity)
            val lowerColor = severityColor(postureResult.pelvicSeverity)

            listOf(
                HEAD_CONNECTIONS  to axialColor,
                UPPER_CONNECTIONS to upperColor,
                SPINE_CONNECTIONS to spineColor,
                LOWER_CONNECTIONS to lowerColor
            ).forEach { (connections, regionColor) ->
                connections.forEach { (a, b) ->
                    val lmA = lmMap[a]; val lmB = lmMap[b]
                    if (lmA != null && lmB != null &&
                        lmA.confidence > GaitConstants.POSTURE_CONFIDENCE &&
                        lmB.confidence > GaitConstants.POSTURE_CONFIDENCE
                    ) {
                        val conf = (lmA.confidence + lmB.confidence) / 2f
                        drawLine(
                            color       = regionColor.copy(alpha = conf * 0.90f),
                            start       = Offset(lmA.x * w, lmA.y * h),
                            end         = Offset(lmB.x * w, lmB.y * h),
                            strokeWidth = 5f,
                            cap         = StrokeCap.Round
                        )
                    }
                }
            }

            // Joint dots
            val regionIds = mapOf(
                HEAD_CONNECTIONS.flatMap  { listOf(it.first, it.second) }.toSet() to axialColor,
                UPPER_CONNECTIONS.flatMap { listOf(it.first, it.second) }.toSet() to upperColor,
                SPINE_CONNECTIONS.flatMap { listOf(it.first, it.second) }.toSet() to spineColor,
                LOWER_CONNECTIONS.flatMap { listOf(it.first, it.second) }.toSet() to lowerColor
            )
            postureResult.landmarks
                .filter { it.confidence > GaitConstants.POSTURE_CONFIDENCE }
                .forEach { lm ->
                    val c = regionIds.entries.firstOrNull { (ids, _) -> lm.id in ids }?.value ?: BrandTeal
                    drawCircle(color = c.copy(alpha = lm.confidence * 0.85f), radius = 8f,
                        center = Offset(lm.x * w, lm.y * h))
                    drawCircle(color = Color.White.copy(alpha = 0.90f), radius = 3.5f,
                        center = Offset(lm.x * w, lm.y * h))
                }

            // Reference lines (shoulder + hip level)
            val ls = lmMap[PoseLandmark.LEFT_SHOULDER]; val rs = lmMap[PoseLandmark.RIGHT_SHOULDER]
            if (ls != null && rs != null) {
                val sy = ((ls.y + rs.y) / 2f) * h
                drawLine(color = upperColor.copy(alpha = 0.50f),
                    start = Offset(0f, sy), end = Offset(w, sy),
                    strokeWidth = 1.5f, cap = StrokeCap.Round)
            }
            val lh = lmMap[PoseLandmark.LEFT_HIP]; val rh = lmMap[PoseLandmark.RIGHT_HIP]
            if (lh != null && rh != null) {
                val hy = ((lh.y + rh.y) / 2f) * h
                drawLine(color = lowerColor.copy(alpha = 0.50f),
                    start = Offset(0f, hy), end = Offset(w, hy),
                    strokeWidth = 1.5f, cap = StrokeCap.Round)
            }

            // Scan bar
            if (h > 0f) {
                val scanY = scanOffset * h
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, BrandTeal.copy(alpha = 0.06f), Color.Transparent),
                        startY = scanY, endY = scanY + h / 3f
                    ),
                    size = this.size
                )
            }
        }

        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GaitGuardLockup(onDark = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassIconButton(Icons.Default.Cameraswitch) {
                    previewViewRef?.let { pv -> viewModel.flipCamera(lifecycleOwner, pv) }
                }
            }
        }

        // ── Pose status indicator ────────────────────────────────────────
        val poseAlpha by animateFloatAsState(
            if (postureResult.avgConfidence >= GaitConstants.POSTURE_MIN_CONF) 1f else 0f,
            label = "poseAlpha"
        )
        if (poseAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 114.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x88022240))
                    .border(1.dp, BrandTeal.copy(alpha = 0.35f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(BrandTeal.copy(alpha = pulsingAlpha())))
                    Text("POSE LOCKED", style = LabelCapsStyle, color = Color.White)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC07090E))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Stand straight · face the camera",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.90f),
                    fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Bottom panel ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            SpinalHealthHandle(
                score = postureResult.spinalHealthScore,
                grade = postureResult.clinicalGrade,
                onOpen = { showHealthSheet = true }
            )

            // Coach tip strip
            if (coachCue != null && postureResult.avgConfidence > 0.4f) {
                val toneColor = severityColor(coachCue.severity)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(toneColor.copy(alpha = 0.14f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.padding(top = 5.dp).size(5.dp).clip(CircleShape).background(toneColor)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(coachCue.headline, fontSize = 12.sp, color = toneColor,
                            fontWeight = FontWeight.Bold)
                        Text(coachCue.detail, fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.82f), lineHeight = 15.sp)
                    }
                }
            }

            // Score + metrics row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xBB07090E))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score block
                val scoreColor = when {
                    postureResult.spinalHealthScore >= 70 -> BrandTeal
                    postureResult.spinalHealthScore >= 50 -> BrandAmber
                    else -> BrandRed
                }
                Column(Modifier.weight(1.4f)) {
                    Text("POSTURE SCORE", style = LabelCapsStyle,
                        color = Color.White.copy(alpha = 0.52f))
                    Row(verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            postureResult.spinalHealthScore.toString(),
                            fontFamily = LexendFamily, fontSize = 26.sp,
                            fontWeight = FontWeight.Bold, color = scoreColor,
                            letterSpacing = (-0.78).sp
                        )
                        Text("/ 100", fontSize = 11.sp, color = Color.White.copy(alpha = 0.45f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text(postureResult.clinicalGrade, fontSize = 10.sp,
                        color = scoreColor.copy(alpha = 0.90f), fontWeight = FontWeight.SemiBold)
                }

                // Vertical divider
                Box(Modifier.width(1.dp).height(44.dp).background(Color.White.copy(alpha = 0.12f)))

                // 3 mini metrics
                listOf(
                    "Shoulder" to "${abs(postureResult.shoulderLevelDeg).toInt()}°",
                    "Kyphosis" to "${postureResult.kyphosisAngleDeg.toInt()}°",
                    "Axial"    to if (postureResult.axialAligned) "OK" else "Off"
                ).forEachIndexed { i, (label, value) ->
                    val metricSev = when (i) {
                        0 -> postureResult.shoulderSeverity
                        1 -> postureResult.kyphosisSeverity
                        else -> if (postureResult.axialAligned) MetricSeverity.NORMAL else MetricSeverity.ALERT
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, style = LabelCapsStyle, color = Color.White.copy(alpha = 0.50f),
                            fontSize = 9.sp)
                        Text(value, fontFamily = LexendFamily, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = severityColor(metricSev),
                            letterSpacing = (-0.3).sp)
                    }
                }
            }

            // Controls row: countdown selector + shutter + gallery
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xEA07090E))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown chips: 2s / 5s / 10s
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(2, 5, 10).forEach { secs ->
                        val active = selectedDelay == secs
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (active) Brush.linearGradient(listOf(BrandNavy3, BrandNavy2))
                                    else Brush.linearGradient(listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.06f)
                                    ))
                                )
                                .border(1.dp,
                                    if (active) Color.Transparent else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(99.dp))
                                .clickable {
                                    selectedDelay = if (active) null else secs
                                }
                                .padding(horizontal = 13.dp, vertical = 7.dp)
                        ) {
                            Text("${secs}s", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else Color.White.copy(alpha = 0.65f))
                        }
                    }
                }

                // Shutter button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                        .clickable {
                            if (selectedDelay != null) viewModel.startCountdown(selectedDelay!!)
                            else viewModel.captureNow()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(BrandNavy3, BrandNavy2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White,
                            modifier = Modifier.size(24.dp))
                    }
                }

                // Gallery button
                GlassIconButton(Icons.Default.PhotoLibrary) { galleryLauncher.launch("image/*") }
            }
        }

        // ── Gallery analysis loading overlay ─────────────────────────────
        AnimatedVisibility(
            visible  = isAnalyzingGallery,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = BrandTeal, strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp))
                    Text("Analyzing photo…", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("ML Kit pose detection running", fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f))
                }
            }
        }

        // ── Countdown overlay ────────────────────────────────────────────
        AnimatedVisibility(
            visible  = countdown != null,
            enter    = fadeIn() + scaleIn(initialScale = 0.85f),
            exit     = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        (countdown ?: 0).toString(),
                        fontFamily = LexendFamily,
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-4).sp
                    )
                    Text("Get ready…", fontSize = 16.sp, color = Color.White.copy(alpha = 0.72f),
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.cancelCountdown() }
                    ) {
                        Text("Cancel", fontSize = 14.sp, color = BrandRed,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpinalHealthHandle(
    score: Int,
    grade: String,
    onOpen: () -> Unit
) {
    val scoreColor = when {
        score >= 70 -> BrandTeal
        score >= 50 -> BrandAmber
        else -> BrandRed
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD907090E))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -12f) onOpen()
                }
            }
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(scoreColor.copy(alpha = 0.16f))
                .border(1.dp, scoreColor.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(score.toString(), fontFamily = LexendFamily, fontSize = 15.sp,
                fontWeight = FontWeight.Bold, color = scoreColor)
        }
        Column(Modifier.weight(1f)) {
            Text("Spinal Health", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Swipe up for posture assessment · $grade",
                fontSize = 11.sp, color = Color.White.copy(alpha = 0.68f))
        }
        Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SpinalHealthQuickSheet(
    score: Int,
    grade: String,
    shoulder: Int,
    kyphosis: Int,
    axialAligned: Boolean,
    onOpenFull: () -> Unit
) {
    val t = MaterialTheme.gg
    val scoreColor = when {
        score >= 70 -> BrandTeal
        score >= 50 -> BrandAmber
        else -> BrandRed
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.12f))
                    .border(2.dp, scoreColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(score.toString(), fontFamily = LexendFamily, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, color = scoreColor)
            }
            Column(Modifier.weight(1f)) {
                Text("Spinal Health", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = t.ink)
                Text(grade, fontSize = 13.sp, color = t.ink3, modifier = Modifier.padding(top = 2.dp))
            }
            StatusPill(if (axialAligned) "Aligned" else "Off Axis",
                if (axialAligned) StatusTone.Good else StatusTone.Warn, dot = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickMetric("Shoulder", "$shoulder°", Modifier.weight(1f))
            QuickMetric("Kyphosis", "$kyphosis°", Modifier.weight(1f))
            QuickMetric("Axial", if (axialAligned) "OK" else "Off", Modifier.weight(1f))
        }
        TextButton(
            onClick = onOpenFull,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(BrandNavy3, BrandNavy2)))
                .padding(vertical = 6.dp)
        ) {
            Text("Open Full Assessment", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(t.surfaceElev)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = LexendFamily, fontSize = 18.sp,
            fontWeight = FontWeight.Bold, color = t.ink)
        Text(label.uppercase(), style = LabelCapsStyle, color = t.inkMute)
    }
}

@Composable
private fun GlassIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(13.dp))
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CameraPermissionDenied() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Camera permission required",
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Enable camera access in Settings to analyze your posture.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun severityColor(s: MetricSeverity): Color = when (s) {
    MetricSeverity.ALERT  -> BrandRed
    MetricSeverity.WATCH  -> BrandAmber
    MetricSeverity.NORMAL -> BrandTeal
}
