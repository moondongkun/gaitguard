package com.gaitguard.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.data.db.SessionRecord
import com.gaitguard.app.data.model.fallRiskLevel
import com.gaitguard.app.data.model.FallRiskLevel
import com.gaitguard.app.data.model.GaitResult
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GaitGuardLockup
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.InsightRow
import com.gaitguard.app.ui.components.MetricTile
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.components.rememberAppearProgress
import com.gaitguard.app.ui.theme.BrandAmber
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.BrandViolet
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.DashboardSummary
import com.gaitguard.app.ui.viewmodel.GaitViewModel
import com.gaitguard.app.util.UserProfile
import java.util.concurrent.TimeUnit

@Composable
fun MetricsScreen(
    viewModel: GaitViewModel,
    onNavigateToLive: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    val result   = uiState.gaitResult
    val fallRisk = result.fallRiskLevel()
    val ctx      = LocalContext.current
    val t = MaterialTheme.gg

    val riskTone = when (fallRisk) {
        FallRiskLevel.LOW    -> StatusTone.Good
        FallRiskLevel.MEDIUM -> StatusTone.Warn
        FallRiskLevel.HIGH   -> StatusTone.Bad
    }
    val symmetryLoss = (100f - result.symmetryScore).coerceIn(0f, 100f)

    var showAlertsDialog by remember { mutableStateOf(false) }

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            title = { Text("Alerts") },
            text = {
                Text(
                    when {
                        result.fatigueScore >= 70f -> "High fatigue alerts are active. Rest before another session."
                        fallRisk == FallRiskLevel.HIGH -> "Fall-risk alerts are active. Move carefully and consider contacting a caregiver."
                        uiState.microStumbleCount > 0 -> "${uiState.microStumbleCount} micro-stumble events detected in the current session."
                        else -> "No active alerts. Gait rhythm, fatigue, and fall-risk signals are currently within normal range."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showAlertsDialog = false }) { Text("Done") }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top app bar ─────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GaitGuardLockup()
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ChromeIconButton(Icons.Default.NotificationsNone) { showAlertsDialog = true }
                        ChromeIconButton(Icons.Default.Settings) { onNavigateToSettings() }
                    }
                }
            }

            // ── Greeting ────────────────────────────────────
            item {
                Column {
                    Text("VITALITY OVERVIEW", style = LabelCapsStyle, color = t.inkMute)
                    Spacer(Modifier.height(6.dp))
                    Text(UserProfile.greeting(ctx), fontFamily = LexendFamily, fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold, color = t.ink, letterSpacing = (-0.5).sp)
                }
            }

            // ── Hero: prominent fatigue ring + status + CTA ──
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()) {
                        FatigueRing(score = result.fatigueScore.toInt(), size = 168.dp)
                        Spacer(Modifier.height(12.dp))
                        StatusPill(
                            "Fall risk · ${fallRisk.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            riskTone
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            when {
                                result.fatigueScore >= 70f -> "High fatigue — rest advised"
                                result.fatigueScore >= 40f -> "Watch your stride"
                                else -> "Current vitality is strong"
                            },
                            fontFamily = LexendFamily, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.White, letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Gait shows ${if (result.symmetryScore >= 90f) "high" else "moderate"} stability. " +
                            "Symmetry ${result.symmetryScore.toInt()}% · ${result.cadenceSpm.toInt()} spm cadence; " +
                            "fatigue remains within your clinical baseline.",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f),
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        HeroCtaButton(label = "Start Gait Tracking (IMU)", onClick = onNavigateToLive)
                    }
                }
            }

            // ── Session Goal widget ─────────────────────────
            item { SessionGoalCard(dashboard) }

            // ── Reference stat cards: Stride + Symmetry ─────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StrideCard(result, Modifier.weight(1f))
                    SymmetryCard(result, symmetryLoss, Modifier.weight(1f))
                }
            }

            // ── Total activity row ──────────────────────────
            item { TotalActivityCard(dashboard) }

            // ── Recent Activity ─────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENT ACTIVITY", style = LabelCapsStyle, color = t.inkMute)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    ) {
                        Text("View all", fontSize = 12.sp, color = BrandTeal2, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = BrandTeal2,
                            modifier = Modifier.size(13.dp))
                    }
                }
            }
            item { RecentActivityCard(dashboard.recent.firstOrNull(), onNavigateToHistory) }

            // ── Live metrics (existing, retained) ───────────
            item {
                Text("LIVE METRICS", style = LabelCapsStyle, color = t.inkMute,
                    modifier = Modifier.padding(top = 2.dp))
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "Trunk Tilt", value = "${result.trunkTiltDeg.toInt()}",
                        unit = "°",
                        subtitle = if (result.trunkTiltDeg == 0f) "Waiting for steps" else "Pitch drift per stride",
                        accent = BrandTeal2,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Straighten, null, tint = BrandTeal2, modifier = Modifier.size(16.dp)) }
                    )
                    MetricTile(
                        label = "Cadence", value = "${result.cadenceSpm.toInt()}",
                        unit = "spm",
                        subtitle = if (result.cadenceSpm > 0f) "${result.stepTimestamps.size} steps in window" else "Start walking to measure",
                        accent = BrandAmber,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.MonitorHeart, null, tint = BrandAmber, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "Harmonic Ratio", value = String.format("%.1f", result.harmonicRatio),
                        unit = "idx",
                        subtitle = when {
                            result.harmonicRatio == GaitResult.HR_HEALTHY_DEFAULT && result.cadenceSpm == 0f -> "Waiting for gait"
                            result.harmonicRatio < 3f -> "Fatigued"
                            result.harmonicRatio < 5f -> "Watch"
                            else -> "Healthy"
                        },
                        accent = BrandViolet,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Straighten, null, tint = BrandViolet, modifier = Modifier.size(16.dp)) }
                    )
                    MetricTile(
                        label = "Stance/Swing", value = String.format("%.1f", result.stanceSwingRatio),
                        unit = "x",
                        subtitle = if (result.cadenceSpm > 0f) "Gait phase ratio" else "Waiting for gait",
                        accent = BrandNavy3,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, null, tint = BrandNavy3, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // ── System insights ─────────────────────────────
            item {
                GgCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SYSTEM INSIGHTS", style = LabelCapsStyle, color = t.inkMute)
                        val insightCount = insights(result.fatigueScore, result.symmetryScore,
                            result.harmonicRatio, uiState.microStumbleCount).size
                        Text("$insightCount new", fontSize = 11.sp, color = t.ink3, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    insights(result.fatigueScore, result.symmetryScore,
                        result.harmonicRatio, uiState.microStumbleCount).forEach { (tone, ico, msg) ->
                        InsightRow(
                            text = msg,
                            tone = tone,
                            icon = { Icon(ico, null, modifier = Modifier.size(13.dp)) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ── Session Goal ─────────────────────────────────────────────── */

@Composable
private fun SessionGoalCard(d: DashboardSummary) {
    val appear = rememberAppearProgress(d, durationMs = 900)
    HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Column {
                    Text("SESSION GOAL", style = LabelCapsStyle, color = Color.White.copy(alpha = 0.75f))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${d.todayMinutes}", fontFamily = LexendFamily, fontSize = 30.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text(" / ${d.goalMinutes} min", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                val delta = d.weekDeltaPercent
                Box(Modifier.clip(RoundedCornerShape(99.dp))
                    .background(Color(0x33043F37)).padding(horizontal = 11.dp, vertical = 6.dp)) {
                    Text("${if (delta >= 0) "+" else ""}$delta% vs LW", fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            // progress bar
            Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.22f))) {
                Box(Modifier.fillMaxWidth(d.goalProgress * appear).height(9.dp)
                    .clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = 0.85f)))
            }
            Spacer(Modifier.height(16.dp))
            Text("DAILY CONSISTENCY", style = LabelCapsStyle, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.height(8.dp))
            val maxDay = (d.dailyMinutes.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom) {
                d.dailyMinutes.forEachIndexed { i, m ->
                    val frac = (m / maxDay.toFloat()).coerceIn(0.04f, 1f) * appear
                    Box(
                        Modifier.weight(1f).height((40 * frac).dp).clip(RoundedCornerShape(5.dp))
                            .background(if (i == 6) Color.White else Color.White.copy(alpha = 0.34f))
                    )
                }
            }
        }
    }
}

/* ── Stride + Symmetry cards ──────────────────────────────────── */

@Composable
private fun StrideCard(result: GaitResult, modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    // Estimated stride length, cadence-correlated (no GPS) — flagged as estimate.
    val hasGait = result.cadenceSpm > 0f
    val stride = (1.30f + (result.cadenceSpm - 100f).coerceIn(-40f, 40f) * 0.004f)
    GgCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(BrandTeal2.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = BrandTeal2, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (hasGait) String.format("%.2f", stride) else "—", fontFamily = LexendFamily,
                fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = t.ink, letterSpacing = (-0.5).sp)
            Text(" m", fontSize = 13.sp, color = t.inkMute, modifier = Modifier.padding(bottom = 3.dp))
        }
        Text("STRIDE LENGTH", style = LabelCapsStyle, color = t.inkMute, modifier = Modifier.padding(top = 6.dp))
        Text(if (hasGait) "Est · Norm 1.35m" else "Start walking to measure",
            fontSize = 11.sp, color = BrandTeal2, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SymmetryCard(result: GaitResult, symmetryLoss: Float, modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    val appear = rememberAppearProgress(result.symmetryScore, durationMs = 900)
    // L/R split derived from symmetry loss
    val rightPct = (50f + symmetryLoss / 2f).coerceIn(40f, 60f)
    val leftPct  = 100f - rightPct
    GgCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(BrandNavy3.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SwapHoriz, null, tint = BrandNavy3, modifier = Modifier.size(18.dp))
            }
            if (result.symmetryScore >= 90f)
                Icon(Icons.Default.CheckCircle, null, tint = BrandTeal2, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${result.symmetryScore.toInt()}", fontFamily = LexendFamily, fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold, color = t.ink, letterSpacing = (-0.5).sp)
            Text(" %", fontSize = 13.sp, color = t.inkMute, modifier = Modifier.padding(bottom = 3.dp))
        }
        Text("SYMMETRY SCORE", style = LabelCapsStyle, color = t.inkMute, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(t.border)) {
            Box(Modifier.fillMaxWidth((result.symmetryScore / 100f) * appear).height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Brush.horizontalGradient(listOf(BrandNavy3, BrandTeal))))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("L ${leftPct.toInt()}%", fontSize = 10.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
            Text("R ${rightPct.toInt()}%", fontSize = 10.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TotalActivityCard(d: DashboardSummary) {
    val t = MaterialTheme.gg
    GgCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                .background(BrandTeal.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MonitorHeart, null, tint = BrandTeal2, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format("%.1f", d.totalHours), fontFamily = LexendFamily, fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold, color = t.ink, letterSpacing = (-0.48).sp)
                    Text(" h / wk", fontSize = 12.sp, color = t.inkMute, modifier = Modifier.padding(bottom = 2.dp))
                }
                Text("TOTAL ACTIVITY · ${d.daysMonitored} DAYS MONITORED", style = LabelCapsStyle,
                    color = t.inkMute, modifier = Modifier.padding(top = 4.dp))
            }
            Box(Modifier.clip(RoundedCornerShape(99.dp)).background(BrandTeal.copy(alpha = 0.14f))
                .padding(horizontal = 9.dp, vertical = 5.dp)) {
                Text("+${d.todayMinutes}m today", fontSize = 10.5.sp, color = BrandTeal2, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecentActivityCard(session: SessionRecord?, onClick: () -> Unit) {
    val t = MaterialTheme.gg
    GgCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }, padding = PaddingValues(0.dp)) {
        // Banner
        Box(
            Modifier.fillMaxWidth().height(84.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF0B2440), Color(0xFF0A3A45))))
        ) {
            Box(
                Modifier.padding(12.dp).clip(RoundedCornerShape(99.dp))
                    .background(Color(0x66000000)).padding(horizontal = 10.dp, vertical = 5.dp)
                    .align(Alignment.BottomStart)
            ) {
                Text("Personal Session", fontSize = 11.sp, color = Color(0xFFDFF7F1), fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SESSION COMPLETED", style = LabelCapsStyle, color = t.inkMute)
                Text(session?.let { relativeTime(it.date) } ?: "—", fontSize = 11.sp, color = t.ink3,
                    fontWeight = FontWeight.SemiBold)
            }
            Text(session?.let { sessionTitle(it.date) } ?: "No sessions yet",
                fontFamily = LexendFamily, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                color = t.ink, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                MiniStat("${session?.durationMinutes ?: 0}m", "DURATION")
                MiniStat("${session?.avgSymmetryScore?.toInt() ?: 0}%", "SYMMETRY")
                MiniStat("${session?.enduranceScore?.toInt() ?: 0}", "ENDURANCE")
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("IMU enabled", StatusTone.Info, dot = false)
                StatusPill("Clinical grade", StatusTone.Good, dot = false)
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    val t = MaterialTheme.gg
    Column {
        Text(value, fontFamily = LexendFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = t.ink)
        Text(label, style = LabelCapsStyle, color = t.inkMute, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun relativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val h = TimeUnit.MILLISECONDS.toHours(diff)
    val d = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        h < 1  -> "Just now"
        h < 24 -> "${h}h ago"
        d < 2  -> "Yesterday"
        else   -> "${d}d ago"
    }
}

private fun sessionTitle(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val tod = when (cal.get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11  -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..20 -> "Evening"
        else      -> "Night"
    }
    return "$tod Gait Analysis"
}

@Composable
private fun ChromeIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val t = MaterialTheme.gg
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(t.surfaceElev)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(icon, null, tint = t.ink2, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FatigueRing(score: Int, size: androidx.compose.ui.unit.Dp = 110.dp) {
    val appear = rememberAppearProgress(score, durationMs = 1100)
    val pct = (score.coerceIn(0, 100) / 100f) * appear
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(size)) {
            val stroke = 12.dp.toPx()
            val r = (this.size.minDimension - stroke) / 2f
            drawCircle(color = Color.White.copy(alpha = 0.18f), radius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(BrandTeal, BrandTeal2, BrandTeal)),
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter  = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    score.toString(),
                    fontFamily = LexendFamily, fontSize = size.value.times(0.30f).sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, letterSpacing = (-0.96).sp
                )
                Text("%", color = Color.White.copy(alpha = 0.72f), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
            }
            Text("FATIGUE", style = LabelCapsStyle, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun HeroCtaButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 14.dp, horizontal = 18.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.3.sp)
    }
}

private fun insights(
    fatigue: Float, sym: Float, hr: Float, stumbles: Int
): List<Triple<StatusTone, androidx.compose.ui.graphics.vector.ImageVector, String>> = buildList {
    if (fatigue >= 70f) add(Triple(StatusTone.Bad, Icons.Default.Warning, "High fatigue detected — consider rest"))
    if (sym < 90f)     add(Triple(StatusTone.Warn, Icons.Default.Warning, "Symmetry below 90% — gait imbalance detected"))
    if (hr  < 3f)      add(Triple(StatusTone.Warn, Icons.Default.Warning, "Low harmonic ratio — gait irregularity"))
    if (stumbles >= 3) add(Triple(StatusTone.Bad, Icons.Default.Warning, "$stumbles micro-stumbles — consult a physiotherapist"))
    if (isEmpty())     add(Triple(StatusTone.Good, Icons.Default.CheckCircle, "All metrics within normal range"))
}
