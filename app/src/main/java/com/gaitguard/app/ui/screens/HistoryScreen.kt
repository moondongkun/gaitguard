package com.gaitguard.app.ui.screens

import android.graphics.Color as AColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GaitGuardLockup
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.SessionCard
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandAmber
import com.gaitguard.app.ui.theme.BrandRed
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.BrandViolet
import com.gaitguard.app.ui.theme.HeadlineLgStyle
import com.gaitguard.app.ui.theme.HeadlineMdStyle
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.HistoryFilter
import com.gaitguard.app.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onSessionClick: (Long) -> Unit = {}
) {
    val sessions by viewModel.sessions.collectAsState()
    val filter   by viewModel.activeFilter.collectAsState()
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val t = MaterialTheme.gg

    val endurance      = viewModel.getWeeklyEndurance()
    val symmetry       = viewModel.getWeeklySymmetry()
    val postureHealth  by viewModel.weeklyPostureHealth.collectAsState()
    val currentGlobalScore = remember(sessions) { currentGaitGlobalScore(sessions) }
    val weeklyGlobalScores = remember(sessions) { weeklyGaitGlobalScores(sessions) }
    val topInsights = remember(sessions, currentGlobalScore) {
        gaitDashboardInsights(sessions, currentGlobalScore)
    }

    Scaffold(
        snackbarHost  = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent
    ) { innerPad ->
        Box(Modifier.fillMaxSize()) {
            AmbientBackdrop()
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPad),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) { GaitGuardLockup() }
                }
                item {
                    Column {
                        Text("Gait Dashboard", style = HeadlineLgStyle, color = t.ink)
                        Spacer(Modifier.height(2.dp))
                        Text("${sessions.size} sessions tracked", fontSize = 13.sp, color = t.ink3)
                    }
                }

                item {
                    HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            DashboardScoreRing(currentGlobalScore)
                            Column(Modifier.weight(1f)) {
                                Text("GLOBAL GAIT SCORE", style = LabelCapsStyle,
                                    color = Color.White.copy(alpha = 0.72f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    when {
                                        currentGlobalScore >= 85f -> "Excellent Rhythm"
                                        currentGlobalScore >= 70f -> "Stable Mobility"
                                        currentGlobalScore >= 55f -> "Needs Attention"
                                        else -> "High Risk Pattern"
                                    },
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = (-0.32).sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Computed from fatigue, symmetry, endurance, and fall-risk events.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.74f),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                item {
                    GgCard(elevated = true) {
                        TrendHeader("Weekly Global Score", "${weeklyGlobalScores.lastOrNull()?.toInt() ?: 0}",
                            "7-day summary", color = BrandNavy3, deltaIsLabel = true)
                        Spacer(Modifier.height(14.dp))
                        WeeklyGlobalBarChart(weeklyGlobalScores)
                    }
                }

                item {
                    GgCard {
                        Text("TOP INSIGHTS", style = LabelCapsStyle, color = t.inkMute)
                        Spacer(Modifier.height(10.dp))
                        topInsights.forEach { (tone, text) ->
                            DashboardInsightRow(tone, text)
                            Spacer(Modifier.height(7.dp))
                        }
                    }
                }

                // ── Endurance trend ──────────────────────────
                item {
                    GgCard(elevated = true) {
                        TrendHeader("Weekly Endurance", "${endurance.lastOrNull()?.toInt() ?: 0}",
                            "+${(endurance.lastOrNull() ?: 0f).minus(endurance.firstOrNull() ?: 0f).toInt()}% wk",
                            color = BrandTeal2)
                        Spacer(Modifier.height(8.dp))
                        TrendChart(endurance, BrandTeal2)
                    }
                }
                // ── Symmetry trend ───────────────────────────
                item {
                    GgCard {
                        TrendHeader("Symmetry Trend", "${symmetry.lastOrNull()?.toInt() ?: 0}%",
                            "avg 7 days", color = BrandNavy3, deltaIsLabel = true)
                        Spacer(Modifier.height(8.dp))
                        TrendChart(symmetry, BrandNavy3)
                    }
                }
                // ── Posture health trend ──────────────────────
                item {
                    GgCard {
                        TrendHeader(
                            label        = "Spinal Health Score",
                            value        = "${postureHealth.lastOrNull()?.toInt() ?: 0}",
                            delta        = "7-day posture avg",
                            color        = BrandViolet,
                            deltaIsLabel = true,
                            icon         = Icons.Default.FavoriteBorder
                        )
                        Spacer(Modifier.height(8.dp))
                        if (postureHealth.all { it == 0f }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Capture posture snapshots to see your weekly health trend",
                                    fontSize = 12.sp,
                                    color    = t.ink3,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        } else {
                            TrendChart(postureHealth, BrandViolet)
                        }
                    }
                }

                // ── Filter chips ─────────────────────────────
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryFilter.entries.forEach { f ->
                            FilterChip(
                                label    = f.name.replace('_', ' ').lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                active   = filter == f,
                                onClick  = { viewModel.setFilter(f) }
                            )
                        }
                    }
                }

                // ── Export row ───────────────────────────────
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinePillButton(
                            label = "CSV",
                            icon  = Icons.Default.FileDownload,
                            onClick = { viewModel.exportCsv() },
                            modifier = Modifier.weight(1f)
                        )
                        GradientPillButton(
                            label = "Generate PDF Report",
                            icon  = Icons.Default.PictureAsPdf,
                            onClick = {
                                val uri = viewModel.generatePdfReport()
                                scope.launch {
                                    if (uri != null) snackbar.showSnackbar("PDF saved to Downloads")
                                    else snackbar.showSnackbar("No sessions to export")
                                }
                            },
                            modifier = Modifier.weight(2f)
                        )
                    }
                }

                if (sessions.isEmpty()) {
                    item {
                        Text(
                            "No sessions yet. Start your first session from the Live screen.",
                            fontSize = 13.sp,
                            color    = t.ink3,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                items(sessions) { session ->
                    SessionCard(
                        session  = session,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSessionClick(session.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun TrendHeader(
    label: String,
    value: String,
    delta: String,
    color: Color,
    deltaIsLabel: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Filled.TrendingUp
) {
    val t = MaterialTheme.gg
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(label.uppercase(), style = LabelCapsStyle, color = t.inkMute)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(value, fontFamily = LexendFamily, fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold, color = t.ink, letterSpacing = (-0.44).sp)
                if (deltaIsLabel) {
                    Text(delta, fontSize = 11.sp, color = t.ink3, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 3.dp))
                } else {
                    StatusPill(delta, StatusTone.Good, dot = false)
                }
            }
        }
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DashboardScoreRing(score: Float, size: androidx.compose.ui.unit.Dp = 96.dp) {
    val pct = score.coerceIn(0f, 100f) / 100f
    val ringColor = when {
        score >= 70f -> BrandTeal
        score >= 55f -> BrandAmber
        else -> BrandRed
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 8.dp.toPx()
            val r = (this.size.minDimension - stroke) / 2f
            drawCircle(color = Color.White.copy(alpha = 0.18f), radius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(ringColor.copy(alpha = 0.65f), ringColor)),
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toInt().toString(), fontFamily = LexendFamily, fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.7).sp)
            Text("/ 100", fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f),
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WeeklyGlobalBarChart(values: List<Float>) {
    val t = MaterialTheme.gg
    val data = if (values.size >= 7) values.takeLast(7) else values + List(7 - values.size) { 0f }
    Column {
        Canvas(Modifier.fillMaxWidth().height(116.dp)) {
            val gap = 7.dp.toPx()
            val barWidth = (size.width - gap * 6) / 7
            val maxH = size.height - 6.dp.toPx()
            data.forEachIndexed { index, value ->
                val left = index * (barWidth + gap)
                val h = if (value > 0f) (value.coerceIn(0f, 100f) / 100f) * maxH else 5.dp.toPx()
                val top = size.height - h
                val color = when {
                    value <= 0f -> t.border.copy(alpha = 0.55f)
                    value >= 70f -> BrandTeal
                    value >= 55f -> BrandAmber
                    else -> BrandRed
                }
                drawRoundRect(
                    color = t.border.copy(alpha = 0.35f),
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(5.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.52f))),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(5.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, fontSize = 10.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DashboardInsightRow(tone: StatusTone, text: String) {
    val t = MaterialTheme.gg
    val color = when (tone) {
        StatusTone.Good -> BrandTeal2
        StatusTone.Warn -> BrandAmber
        StatusTone.Bad -> BrandRed
        StatusTone.Info -> BrandNavy3
        StatusTone.Neutral -> t.ink3
    }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.padding(top = 5.dp).size(7.dp).clip(CircleShape).background(color))
        Text(text, fontSize = 12.sp, color = t.ink2, lineHeight = 17.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TrendChart(data: List<Float>, color: Color) {
    val t = MaterialTheme.gg
    if (data.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No data yet — complete a session to see trends",
                fontSize = 12.sp, color = t.ink3)
        }
        return
    }
    val chartColor = AColor.argb((color.alpha * 255).toInt(),
        (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
    val fillAlpha  = 60
    val axisColor  = AColor.argb((t.ink3.alpha * 255).toInt(),
        (t.ink3.red * 255).toInt(), (t.ink3.green * 255).toInt(), (t.ink3.blue * 255).toInt())
    val gridColor  = AColor.argb(60,
        (t.border.red * 255).toInt(), (t.border.green * 255).toInt(), (t.border.blue * 255).toInt())
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled      = false
                setTouchEnabled(false)
                axisRight.isEnabled   = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = axisColor
                    textSize  = 9f
                }
                axisLeft.apply {
                    textColor = axisColor
                    textSize  = 9f
                    setDrawGridLines(true)
                    this.gridColor = gridColor
                }
                setViewPortOffsets(20f, 8f, 8f, 24f)
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { i, v -> Entry(i.toFloat(), v) }
            val set = LineDataSet(entries, "").apply {
                setColor(chartColor)
                setCircleColor(chartColor)
                lineWidth = 2.4f
                circleRadius = 3.5f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = chartColor
                this.fillAlpha = fillAlpha
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(set)
            chart.invalidate()
        }
    )
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val t = MaterialTheme.gg
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (active) Brush.linearGradient(listOf(BrandNavy3, BrandNavy2))
                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (active) Color.White else t.ink3)
    }
}

@Composable
private fun OutlinePillButton(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val t = MaterialTheme.gg
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = t.ink2, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = t.ink2, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun GradientPillButton(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(12.dp), clip = false,
                ambientColor = BrandNavy2.copy(alpha = 0.30f),
                spotColor    = BrandNavy2.copy(alpha = 0.30f))
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(BrandNavy3, BrandNavy2)))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun currentGaitGlobalScore(sessions: List<com.gaitguard.app.data.db.SessionRecord>): Float {
    val latest = sessions.firstOrNull() ?: return 0f
    return sessionGlobalScore(latest)
}

private fun weeklyGaitGlobalScores(sessions: List<com.gaitguard.app.data.db.SessionRecord>): List<Float> {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000
    return (6 downTo 0).map { daysAgo ->
        val start = now - (daysAgo + 1) * dayMs
        val end = now - daysAgo * dayMs
        val avg = sessions
            .filter { it.date in start..end }
            .map { sessionGlobalScore(it) }
            .average()
        if (avg.isNaN()) 0f else avg.toFloat()
    }
}

private fun sessionGlobalScore(session: com.gaitguard.app.data.db.SessionRecord): Float {
    val fatigueHealth = 100f - session.peakFatigueScore.coerceIn(0f, 100f)
    val symmetry = session.avgSymmetryScore.coerceIn(0f, 100f)
    val endurance = session.enduranceScore.coerceIn(0f, 100f)
    val fallRisk = (100f - session.fallRiskEventCount * 12f - session.microStumbleCount * 4f).coerceIn(0f, 100f)
    return (fatigueHealth * 0.30f + symmetry * 0.30f + endurance * 0.25f + fallRisk * 0.15f).coerceIn(0f, 100f)
}

private fun gaitDashboardInsights(
    sessions: List<com.gaitguard.app.data.db.SessionRecord>,
    currentScore: Float
): List<Pair<StatusTone, String>> {
    val latest = sessions.firstOrNull()
    return buildList {
        when {
            sessions.isEmpty() -> add(StatusTone.Info to "Start a live session to build your personalized gait dashboard.")
            currentScore >= 85f -> add(StatusTone.Good to "Your latest global gait score is strong. Keep your current walking routine consistent.")
            currentScore >= 70f -> add(StatusTone.Good to "Your gait rhythm is stable, with room to improve endurance and fatigue resistance.")
            currentScore >= 55f -> add(StatusTone.Warn to "Your global gait score needs attention. Shorter, more frequent walks may help reduce fatigue.")
            else -> add(StatusTone.Bad to "Your recent gait pattern shows elevated risk. Consider resting and reviewing your session details.")
        }
        latest?.let {
            if (it.peakFatigueScore >= 60f) add(StatusTone.Warn to "Peak fatigue reached ${it.peakFatigueScore.toInt()} / 100 in the last session.")
            if (it.avgSymmetryScore < 90f) add(StatusTone.Warn to "Stride symmetry averaged ${it.avgSymmetryScore.toInt()}%, suggesting left-right imbalance.")
            if (it.fallRiskEventCount > 0 || it.microStumbleCount > 0) {
                add(StatusTone.Bad to "Fall-risk events or micro-stumbles appeared recently. Use flat terrain and monitor changes.")
            }
        }
        if (size < 3) add(StatusTone.Info to "Weekly bars summarize your global score by day so progress is easy to scan.")
    }.take(3)
}
