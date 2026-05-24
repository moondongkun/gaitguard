package com.gaitguard.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.data.model.MetricSeverity
import com.gaitguard.app.data.model.PostureResult
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.PosturePhotoThumbnail
import com.gaitguard.app.ui.components.PosturePhotoWithCues
import com.gaitguard.app.ui.components.StatusTone
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs

@Composable
fun PostureCaptureResultScreen(
    viewModel: PostureViewModel,
    onBack: () -> Unit,
    onViewReport: () -> Unit
) {
    // Use the frozen captured result; fall back to live flow if not yet set
    val capturedResult by viewModel.capturedResult.collectAsState()
    val capturedPhotoUri by viewModel.capturedPhotoUri.collectAsState()
    val liveResult     by viewModel.postureFlow.collectAsState()
    val photoRecords   by viewModel.photoRecords.collectAsState()
    val result = capturedResult ?: liveResult
    val resultPhotoUri = capturedPhotoUri
        ?: photoRecords.firstOrNull { it.timestamp == result.timestamp }?.sourceUri

    val t = MaterialTheme.gg

    val insights = remember(result) { buildResultInsights(result) }

    val scoreColor = when {
        result.spinalHealthScore >= 70 -> BrandTeal
        result.spinalHealthScore >= 50 -> BrandAmber
        else                           -> BrandRed
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                            .background(t.surfaceElev)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = t.ink2, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Analysis Result", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            color = t.ink, letterSpacing = (-0.18).sp)
                        Text(
                            SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.US)
                                .format(Date(result.timestamp)),
                            fontSize = 11.sp, color = t.ink3
                        )
                    }
                }
            }

            // ── Hero: score ring + grade ─────────────────────────────────
            item {
                GgCard {
                    Text("PHOTO ANALYSIS", style = LabelCapsStyle, color = t.inkMute)
                    Spacer(Modifier.height(10.dp))
                    PosturePhotoWithCues(
                        photoUri = resultPhotoUri,
                        result = result,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusLegendDot("Aligned", BrandTeal2, Modifier.weight(1f))
                        StatusLegendDot("Watch", BrandAmber, Modifier.weight(1f))
                        StatusLegendDot("Irregular", BrandRed, Modifier.weight(1f))
                    }
                }
            }

            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ResultScoreRing(score = result.spinalHealthScore)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("SPINAL HEALTH SCORE", style = LabelCapsStyle,
                                color = Color.White.copy(alpha = 0.68f))
                            Text(
                                result.clinicalGrade,
                                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color.White, letterSpacing = (-0.44).sp
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(scoreColor.copy(alpha = 0.22f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.AccessibilityNew, null,
                                    tint = scoreColor, modifier = Modifier.size(11.dp))
                                Text(
                                    when {
                                        result.spinalHealthScore >= 70 -> "Good posture"
                                        result.spinalHealthScore >= 50 -> "Needs attention"
                                        else                           -> "Correction needed"
                                    },
                                    fontSize = 10.sp, color = scoreColor, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── 4 metric cards in 2×2 grid ───────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricResultCard(
                            label = "Shoulder Level",
                            value = "${abs(result.shoulderLevelDeg).toInt()}°",
                            sev   = result.shoulderSeverity,
                            note  = if (result.shoulderLevelDeg > 0) "Right high" else "Left high",
                            modifier = Modifier.weight(1f)
                        )
                        MetricResultCard(
                            label = "Kyphosis",
                            value = "${result.kyphosisAngleDeg.toInt()}°",
                            sev   = result.kyphosisSeverity,
                            note  = "Thoracic angle",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricResultCard(
                            label = "Pelvic Tilt",
                            value = "${abs(result.pelvicTiltDeg).toInt()}°",
                            sev   = result.pelvicSeverity,
                            note  = if (result.pelvicTiltDeg > 0) "Anterior tilt" else "Posterior tilt",
                            modifier = Modifier.weight(1f)
                        )
                        MetricResultCard(
                            label = "Axial Align",
                            value = if (result.axialAligned) "Aligned" else "Deviated",
                            sev   = if (result.axialAligned) MetricSeverity.NORMAL else MetricSeverity.ALERT,
                            note  = "Lateral axis",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── AI Insights ───────────────────────────────────────────────
            item {
                GgCard {
                    Text("AI INSIGHTS", style = LabelCapsStyle, color = t.inkMute)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        insights.take(3).forEach { ResultInsightRow(it) }
                    }
                }
            }

            item {
                GgCard {
                    Text("ANATOMY BREAKDOWN", style = LabelCapsStyle, color = t.inkMute)
                    Spacer(Modifier.height(10.dp))
                    AnatomyStatusRow("Head & Neck", result.kyphosisSeverity, "${result.kyphosisAngleDeg.toInt()} deg forward angle")
                    HorizontalDivider(color = t.border, modifier = Modifier.padding(vertical = 8.dp))
                    AnatomyStatusRow("Shoulders", result.shoulderSeverity, "${abs(result.shoulderLevelDeg).toInt()} deg level difference")
                    HorizontalDivider(color = t.border, modifier = Modifier.padding(vertical = 8.dp))
                    AnatomyStatusRow("Spine & Trunk", if (result.axialAligned) MetricSeverity.NORMAL else MetricSeverity.ALERT,
                        if (result.axialAligned) "Centered axis" else "Lateral deviation detected")
                    HorizontalDivider(color = t.border, modifier = Modifier.padding(vertical = 8.dp))
                    AnatomyStatusRow("Hips & Pelvis", result.pelvicSeverity, "${abs(result.pelvicTiltDeg).toInt()} deg pelvic tilt")
                }
            }

            // ── Action buttons ────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(BrandNavy3, BrandNavy2)))
                            .clickable { onViewReport() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("View Full Health Report →", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, t.border, RoundedCornerShape(16.dp))
                            .clickable { onBack() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("← Retake", fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = t.ink2)
                    }
                }
            }

            // ── Recent photo records ──────────────────────────────────────
            val recent = photoRecords.take(5)
            if (recent.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("PHOTO HISTORY", style = LabelCapsStyle, color = t.inkMute,
                        modifier = Modifier.padding(start = 2.dp))
                }
                items(recent) { snap ->
                    GgCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val snapColor = when {
                                snap.spinalHealthScore >= 70 -> BrandTeal2
                                snap.spinalHealthScore >= 50 -> BrandAmber
                                else                         -> BrandRed
                            }
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(snapColor.copy(alpha = 0.10f))
                                    .border(2.dp, snapColor.copy(alpha = 0.45f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (snap.spinalHealthScore > 0) snap.spinalHealthScore.toString() else "—",
                                        fontFamily = LexendFamily, fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold, color = snapColor
                                    )
                                    if (snap.spinalHealthScore > 0)
                                        Text("/100", fontSize = 6.sp, color = t.ink3)
                                }
                            }
                            PosturePhotoThumbnail(
                                photoUri = snap.sourceUri,
                                modifier = Modifier.size(54.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (snap.clinicalGrade.isNotEmpty()) snap.clinicalGrade else "Captured",
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = t.ink
                                    )
                                }
                                Text(
                                    SimpleDateFormat("MMM d · HH:mm", Locale.US)
                                        .format(Date(snap.timestamp)),
                                    fontSize = 10.sp, color = t.ink3
                                )
                                val issues = buildList {
                                    if (snap.kyphosisAngleDeg > 40f) add("Kyphosis ${snap.kyphosisAngleDeg.toInt()}°")
                                    if (abs(snap.shoulderLevelDeg) > 3f) add("Shoulder ${abs(snap.shoulderLevelDeg).toInt()}°")
                                    if (!snap.axialAligned) add("Lateral deviation")
                                    if (isEmpty()) add("No issues detected")
                                }
                                Text(issues.take(2).joinToString(" · "),
                                    fontSize = 10.sp, color = snapColor.copy(alpha = 0.80f),
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                            IconButton(onClick = { viewModel.deletePhotoRecord(snap.id) }) {
                                Icon(Icons.Default.DeleteOutline, null, tint = BrandRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Score ring ────────────────────────────────────────────────────────────────

@Composable
private fun ResultScoreRing(score: Int, sizeDp: androidx.compose.ui.unit.Dp = 96.dp) {
    val pct = score.coerceIn(0, 100) / 100f
    val arcColor = when {
        score >= 70 -> BrandTeal
        score >= 50 -> BrandAmber
        else        -> BrandRed
    }
    Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp)) {
            val stroke = 8.dp.toPx()
            val r = (size.minDimension - stroke) / 2f
            drawCircle(color = Color.White.copy(alpha = 0.15f), radius = r, style = Stroke(stroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(arcColor.copy(alpha = 0.65f), arcColor)),
                startAngle = -90f, sweepAngle = 360f * pct, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), fontFamily = LexendFamily, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.78).sp)
            Text("/ 100", color = Color.White.copy(alpha = 0.60f), fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Metric result card ────────────────────────────────────────────────────────

@Composable
private fun MetricResultCard(
    label: String,
    value: String,
    sev: MetricSeverity,
    note: String,
    modifier: Modifier = Modifier
) {
    val t = MaterialTheme.gg
    val c = when (sev) {
        MetricSeverity.ALERT  -> BrandRed
        MetricSeverity.WATCH  -> BrandAmber
        MetricSeverity.NORMAL -> BrandTeal2
    }
    GgCard(modifier = modifier) {
        Text(label.uppercase(), style = LabelCapsStyle, color = t.inkMute, fontSize = 9.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = LexendFamily, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, color = c, letterSpacing = (-0.40).sp)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(c.copy(alpha = 0.10f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(note, fontSize = 9.sp, color = c, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Insight row ───────────────────────────────────────────────────────────────

@Composable
private fun StatusLegendDot(label: String, color: Color, modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 9.sp, color = t.ink2, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AnatomyStatusRow(title: String, severity: MetricSeverity, summary: String) {
    val t = MaterialTheme.gg
    val color = when (severity) {
        MetricSeverity.ALERT -> BrandRed
        MetricSeverity.WATCH -> BrandAmber
        MetricSeverity.NORMAL -> BrandTeal2
    }
    val label = when (severity) {
        MetricSeverity.ALERT -> "Needs correction"
        MetricSeverity.WATCH -> "Watch"
        MetricSeverity.NORMAL -> "Aligned"
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = t.ink)
            Text(summary, fontSize = 11.sp, color = t.ink3, modifier = Modifier.padding(top = 1.dp))
        }
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

private data class ResultInsight(val tone: StatusTone, val title: String, val body: String)

@Composable
private fun ResultInsightRow(item: ResultInsight) {
    val t = MaterialTheme.gg
    val c = when (item.tone) {
        StatusTone.Good    -> BrandTeal2
        StatusTone.Warn    -> BrandAmber
        StatusTone.Bad     -> BrandRed
        StatusTone.Info    -> BrandNavy3
        StatusTone.Neutral -> t.ink3
    }
    val icon = when (item.tone) {
        StatusTone.Good -> Icons.Default.CheckCircle
        StatusTone.Warn -> Icons.Default.Lightbulb
        StatusTone.Bad  -> Icons.Default.Warning
        else            -> Icons.Default.Info
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(c.copy(alpha = 0.07f)).padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(26.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = c, modifier = Modifier.size(13.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = t.ink)
            Text(item.body, fontSize = 10.5.sp, color = t.ink2, lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ── Rule-based insights for result screen ────────────────────────────────────

private fun buildResultInsights(result: PostureResult): List<ResultInsight> = buildList {
    when (result.kyphosisSeverity) {
        MetricSeverity.ALERT -> add(ResultInsight(StatusTone.Bad, "Forward Head Posture",
            "Your ${result.kyphosisAngleDeg.toInt()}° lean adds significant load on cervical vertebrae — roughly 1 kg per degree. Common cause of headaches and upper-back tension."))
        MetricSeverity.WATCH -> add(ResultInsight(StatusTone.Warn, "Mild Neck Tilt",
            "Slight forward tilt (${result.kyphosisAngleDeg.toInt()}°). Simple chin tucks 3× daily reduce cervical strain."))
        MetricSeverity.NORMAL -> add(ResultInsight(StatusTone.Good, "Healthy Cervical Alignment",
            "Neck angle ${result.kyphosisAngleDeg.toInt()}° is within the optimal range."))
    }
    when (result.shoulderSeverity) {
        MetricSeverity.ALERT -> add(ResultInsight(StatusTone.Bad, "Shoulder Imbalance",
            "${abs(result.shoulderLevelDeg).toInt()}° unevenness raises rotator cuff injury risk. Check bag-carrying habits and desk height."))
        MetricSeverity.WATCH -> add(ResultInsight(StatusTone.Warn, "Slight Shoulder Tilt",
            "${abs(result.shoulderLevelDeg).toInt()}° tilt. Strengthening the lower trapezius usually resolves this."))
        MetricSeverity.NORMAL -> { }
    }
    if (!result.axialAligned) add(ResultInsight(StatusTone.Warn, "Lateral Deviation",
        "Spine is drifting off-center. Core activation and mindful standing posture correct this gradually."))
    when (result.pelvicSeverity) {
        MetricSeverity.ALERT -> add(ResultInsight(StatusTone.Bad, "Significant Pelvic Tilt",
            "${abs(result.pelvicTiltDeg).toInt()}° tilt exceeds clinical threshold. Hip flexor release and glute activation are strongly recommended."))
        MetricSeverity.WATCH -> add(ResultInsight(StatusTone.Warn, "Minor Hip Tilt",
            "${abs(result.pelvicTiltDeg).toInt()}° tilt. Hip flexor stretches reduce lower-back strain."))
        MetricSeverity.NORMAL -> { }
    }
    if (isEmpty()) add(ResultInsight(StatusTone.Good, "Excellent Alignment",
        "All four biomechanical metrics are within their optimal ranges. Great posture today!"))
}
