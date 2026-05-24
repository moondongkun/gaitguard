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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.gaitguard.app.ui.theme.BrandAmber
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandRed
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.BrandViolet
import com.gaitguard.app.ui.theme.HeadlineLgStyle
import com.gaitguard.app.ui.theme.HeadlineMdStyle
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.GaitViewModel

@Composable
fun MetricsScreen(
    viewModel: GaitViewModel,
    onNavigateToLive: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val result   = uiState.gaitResult
    val fallRisk = result.fallRiskLevel()
    val t = MaterialTheme.gg

    val riskTone = when (fallRisk) {
        FallRiskLevel.LOW    -> StatusTone.Good
        FallRiskLevel.MEDIUM -> StatusTone.Warn
        FallRiskLevel.HIGH   -> StatusTone.Bad
    }
    val riskLabel = "Fall Risk · ${fallRisk.name.lowercase().replaceFirstChar { it.uppercase() }}"
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

            // ── Status pill ─────────────────────────────────
            item { StatusPill(riskLabel, riskTone) }

            // ── Hero card: fatigue + start CTA ──────────────
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            FatigueRing(score = result.fatigueScore.toInt())
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "TODAY",
                                    style = LabelCapsStyle,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    when {
                                        result.fatigueScore >= 70f -> "High fatigue — rest advised"
                                        result.fatigueScore >= 40f -> "Watch your stride"
                                        else -> "Vitality steady — gait stable"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = (-0.30).sp,
                                    lineHeight = 22.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Symmetry ${result.symmetryScore.toInt()}% · " +
                                            "${result.cadenceSpm.toInt()} spm cadence",
                                    fontSize = 12.5.sp,
                                    color = Color.White.copy(alpha = 0.78f),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        HeroCtaButton(label = "Start Gait Session", onClick = onNavigateToLive)
                    }
                }
            }

            // ── 2×2 metric grid ─────────────────────────────
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
                        label = "Symmetry", value = "${result.symmetryScore.toInt()}",
                        unit = "%",
                        subtitle = "${symmetryLoss.toInt()}% left/right imbalance",
                        accent = BrandNavy3,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, null, tint = BrandNavy3, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "Cadence", value = "${result.cadenceSpm.toInt()}",
                        unit = "spm",
                        subtitle = if (result.cadenceSpm > 0f) "${result.stepTimestamps.size} steps in window" else "Start walking to measure",
                        accent = BrandAmber,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.MonitorHeart, null, tint = BrandAmber, modifier = Modifier.size(16.dp)) }
                    )
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
                        Text("System Insights".uppercase(), style = LabelCapsStyle, color = t.inkMute)
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
    val pct = score.coerceIn(0, 100) / 100f
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            val r = (this.size.minDimension - stroke) / 2f
            // Track
            drawCircle(color = Color.White.copy(alpha = 0.18f), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            // Value arc
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
                    fontFamily = LexendFamily, fontSize = 32.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, letterSpacing = (-0.96).sp
                )
                Text("%", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text("FATIGUE", style = LabelCapsStyle, color = Color.White.copy(alpha = 0.65f))
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
            .padding(vertical = 13.dp, horizontal = 18.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.4.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
