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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
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
    onNavigateBackToLive: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToResult: () -> Unit = {}
) {
    val postureResult      by viewModel.postureFlow.collectAsState()
    val isAnalyzingGallery by viewModel.isAnalyzingGallery.collectAsState()
    val countdown          by viewModel.countdown.collectAsState()
    val lifecycleOwner     = LocalLifecycleOwner.current

    var viewSize       by remember { mutableStateOf(IntSize.Zero) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var selectedDelay  by remember { mutableStateOf<Int?>(null) }
    val sheetScaffoldState = rememberBottomSheetScaffoldState()

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

    BottomSheetScaffold(
        scaffoldState = sheetScaffoldState,
        sheetPeekHeight = 82.dp,
        sheetDragHandle = null,
        sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        sheetContainerColor = Color.Transparent,
        containerColor = Color(0xFF0A0E14),
        sheetContent = {
            SpinalHealthQuickSheet(
                score = postureResult.spinalHealthScore,
                grade = postureResult.clinicalGrade,
                shoulder = abs(postureResult.shoulderLevelDeg).toInt(),
                kyphosis = postureResult.kyphosisAngleDeg.toInt(),
                axialAligned = postureResult.axialAligned,
                onOpenFull = onNavigateToHealth
            )
        }
    ) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0E14))) {
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

        // ── Back to live ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x9907090E))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(99.dp))
                    .clickable(onClick = onNavigateBackToLive)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Live", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            GlassIconButton(Icons.Default.Cameraswitch) {
                previewViewRef?.let { pv -> viewModel.flipCamera(lifecycleOwner, pv) }
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

        ClinicalHudStack(
            score = postureResult.spinalHealthScore,
            grade = postureResult.clinicalGrade,
            shoulder = abs(postureResult.shoulderLevelDeg).toInt(),
            shoulderSeverity = postureResult.shoulderSeverity,
            kyphosis = postureResult.kyphosisAngleDeg.toInt(),
            kyphosisSeverity = postureResult.kyphosisSeverity,
            axialAligned = postureResult.axialAligned,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, bottom = 128.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 82.dp)
        ) {
            if (coachCue != null && postureResult.avgConfidence > 0.4f) {
                val toneColor = severityColor(coachCue.severity)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(toneColor.copy(alpha = 0.12f))
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.padding(top = 5.dp).size(5.dp).clip(CircleShape).background(toneColor))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(coachCue.headline, fontSize = 12.sp, color = toneColor, fontWeight = FontWeight.Bold)
                        Text(coachCue.detail, fontSize = 11.sp, color = Color.White.copy(alpha = 0.80f), lineHeight = 15.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GalleryControl { galleryLauncher.launch("image/*") }
                ShutterControl {
                    if (selectedDelay != null) viewModel.startCountdown(selectedDelay!!)
                    else viewModel.captureNow()
                }
                TimerControl(delay = selectedDelay) {
                    selectedDelay = when (selectedDelay) {
                        null -> 2
                        2 -> 5
                        5 -> 10
                        else -> null
                    }
                }
            }
        }

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
                    CircularProgressIndicator(color = BrandTeal, strokeWidth = 3.dp, modifier = Modifier.size(52.dp))
                    Text("Analyzing photo...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("ML Kit pose detection running", fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
                }
            }
        }

        AnimatedVisibility(
            visible  = countdown != null,
            enter    = fadeIn() + scaleIn(initialScale = 0.85f),
            exit     = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xBB000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        (countdown ?: 0).toString(),
                        fontFamily = LexendFamily,
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-4).sp
                    )
                    Text("Get ready...", fontSize = 16.sp, color = Color.White.copy(alpha = 0.72f),
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.cancelCountdown() }) {
                        Text("Cancel", fontSize = 14.sp, color = BrandRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ClinicalHudStack(
    score: Int,
    grade: String,
    shoulder: Int,
    shoulderSeverity: MetricSeverity,
    kyphosis: Int,
    kyphosisSeverity: MetricSeverity,
    axialAligned: Boolean,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 70 -> BrandTeal
        score >= 50 -> BrandAmber
        else -> BrandRed
    }
    val status = when {
        score >= 85 -> "EXCELLENT"
        score >= 70 -> "GOOD"
        score >= 50 -> "WATCH"
        else -> "ALERT"
    }

    Column(modifier = modifier.width(152.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x9907090E))
                .border(1.dp, scoreColor.copy(alpha = 0.44f), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Text("POSTURE SCORE", style = LabelCapsStyle, color = Color.White.copy(alpha = 0.56f))
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "$score",
                    fontFamily = LexendFamily,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                    letterSpacing = (-0.8).sp
                )
                Column {
                    Text("/100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.58f))
                    Text(status, style = LabelCapsStyle, color = scoreColor, fontWeight = FontWeight.Bold)
                }
            }
            Text(grade, fontSize = 10.sp, color = Color.White.copy(alpha = 0.70f), lineHeight = 13.sp)
        }

        HudMetricCard("Shoulder Degree", "$shoulder°", severityColor(shoulderSeverity))
        HudMetricCard("Kyphosis Degree", "$kyphosis°", severityColor(kyphosisSeverity))
        HudMetricCard(
            label = "Axial Status",
            value = if (axialAligned) "OK" else "OFF",
            color = if (axialAligned) BrandTeal else BrandRed,
            showStateDot = true
        )
    }
}

@Composable
private fun HudMetricCard(
    label: String,
    value: String,
    color: Color,
    showStateDot: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x8F07090E))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), style = LabelCapsStyle, color = Color.White.copy(alpha = 0.50f), fontSize = 8.5.sp)
            Text(value, fontFamily = LexendFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
        if (showStateDot) Box(Modifier.size(9.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun GalleryControl(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(BrandTeal2.copy(alpha = 0.45f), BrandNavy3.copy(alpha = 0.60f))))
                .border(1.dp, Color.White.copy(alpha = 0.26f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ShutterControl(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(86.dp)
            .shadow(18.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.24f))
            .border(1.dp, Color.White.copy(alpha = 0.42f), CircleShape)
            .clickable(onClick = onClick)
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandNavy3, BrandNavy2))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun TimerControl(delay: Int?, onClick: () -> Unit) {
    val label = delay?.let { "${it}S" } ?: "OFF"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(12.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = if (delay == null) 0.10f else 0.18f))
                .border(
                    1.dp,
                    if (delay == null) Color.White.copy(alpha = 0.18f) else BrandTeal.copy(alpha = 0.62f),
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Timer,
                null,
                tint = if (delay == null) Color.White.copy(alpha = 0.72f) else BrandTeal,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(label, style = LabelCapsStyle, color = if (delay == null) Color.White.copy(alpha = 0.58f) else BrandTeal, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpinalHealthReportDrawerSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Color(0xE607090E))
            .border(1.dp, BrandTeal.copy(alpha = 0.30f), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
    ) {
        content()
    }
}

@Composable
private fun SpinalHealthReportDrawerHeader(showChevron: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(BrandTeal.copy(alpha = 0.14f))
                .border(1.dp, BrandTeal.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Article, null, tint = BrandTeal, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Spinal Health Report", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Real-time biomechanical summary", fontSize = 11.sp, color = Color.White.copy(alpha = 0.66f))
        }
        if (showChevron) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(26.dp))
        }
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
    val scoreColor = when {
        score >= 70 -> BrandTeal
        score >= 50 -> BrandAmber
        else -> BrandRed
    }
    SpinalHealthReportDrawerSurface(modifier = Modifier.fillMaxWidth()) {
        SpinalHealthReportDrawerHeader(showChevron = true)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
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
                    Text("$score / 100", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(grade, fontSize = 13.sp, color = Color.White.copy(alpha = 0.66f), modifier = Modifier.padding(top = 2.dp))
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
}

@Composable
private fun QuickMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = LexendFamily, fontSize = 18.sp,
            fontWeight = FontWeight.Bold, color = Color.White)
        Text(label.uppercase(), style = LabelCapsStyle, color = Color.White.copy(alpha = 0.58f))
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
