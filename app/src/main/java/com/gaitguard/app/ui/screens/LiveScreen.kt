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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GaitGuardLockup
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.components.WaveformChart
import com.gaitguard.app.ui.components.pulsingAlpha
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandRed
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.HeadlineMdStyle
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.GaitViewModel
import com.gaitguard.app.util.GaitConstants
import java.util.concurrent.TimeUnit

@Composable
fun LiveScreen(
    viewModel: GaitViewModel,
    onNavigateToPosture: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val result   = uiState.gaitResult
    val t = MaterialTheme.gg

    LaunchedEffect(Unit) {
        if (!viewModel.uiState.value.isSessionActive) viewModel.startSession()
    }

    if (uiState.showFallAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFallAlert() },
            icon  = { Icon(Icons.Default.Warning, null, tint = BrandRed, modifier = Modifier.size(40.dp)) },
            title = { Text("Fall Alert", color = BrandRed, style = HeadlineMdStyle) },
            text  = { Text("A fall or high fall risk has been detected. Emergency SMS sent to caregiver.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissFallAlert() },
                    shape   = RoundedCornerShape(14.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) { Text("Dismiss", color = Color.White) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GaitGuardLockup()
                if (uiState.isSessionActive) {
                    StatusPill("Recording", StatusTone.Good)
                }
            }

            // ── Timer hero ───────────────────────────────
            HeroGradientCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandTeal.copy(alpha = if (uiState.isSessionActive) pulsingAlpha() else 0.45f))
                        )
                        Text(
                            if (uiState.isSessionActive) "RECORDING" else if (uiState.isPaused) "PAUSED" else "READY",
                            style = LabelCapsStyle,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val h = TimeUnit.MILLISECONDS.toHours(uiState.elapsedMs)
                    val m = TimeUnit.MILLISECONDS.toMinutes(uiState.elapsedMs) % 60
                    val s = TimeUnit.MILLISECONDS.toSeconds(uiState.elapsedMs) % 60
                    Text(
                        text       = String.format("%02d:%02d", h * 60 + m, s),
                        fontFamily = LexendFamily,
                        fontSize   = 56.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White,
                        letterSpacing = (-1.7).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Session · ${uiState.terrain.label}",
                        style = LabelCapsStyle,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // ── Terrain chip ─────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("${uiState.terrain.icon} Terrain · ${uiState.terrain.label}", StatusTone.Info, dot = false)
                if (uiState.isSessionActive)
                    StatusPill("Live Data", StatusTone.Good)
                if (uiState.markedEvents.isNotEmpty())
                    StatusPill("${uiState.markedEvents.size} marked", StatusTone.Warn)
            }

            PostureAnalysisCallout(onClick = onNavigateToPosture)

            // ── Waveform ─────────────────────────────────
            GgCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IMU Gait Rhythm", style = HeadlineMdStyle.copy(fontSize = 14.sp), color = t.ink)
                    Text("Last 4.0 s", style = LabelCapsStyle, color = t.inkMute)
                }
                Spacer(Modifier.height(8.dp))
                val stepIndices = mapStepIndices(result.stepTimestamps)
                val markIndices = mapEventIndices(uiState.markedEvents)
                val stumbleIndices = mapEventIndices(uiState.stumbleEvents)
                WaveformChart(
                    samples = result.verticalAccWindow,
                    stepIndices = stepIndices,
                    markIndices = markIndices,
                    stumbleIndices = stumbleIndices
                )
                Spacer(Modifier.height(10.dp))
                if (uiState.markedEvents.isNotEmpty() || uiState.stumbleEvents.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill("${uiState.markedEvents.size} marks", StatusTone.Warn, dot = false)
                        if (uiState.stumbleEvents.isNotEmpty()) {
                            StatusPill("${uiState.stumbleEvents.size} stumbles", StatusTone.Bad, dot = false)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                val ratio = result.stanceSwingRatio.coerceAtLeast(0.01f)
                val swing = 100f / (1f + ratio)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("Cadence",  "${result.cadenceSpm.toInt()}", "spm")
                    MiniStat("Swing",    "${swing.toInt()}", "%")
                    MiniStat("Stance",   "${(100 - swing).toInt()}", "%")
                }
            }

            // ── Stride symmetry ─────────────────────────
            GgCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stride Symmetry", style = HeadlineMdStyle.copy(fontSize = 14.sp), color = t.ink)
                    Text(
                        "${result.symmetryScore.toInt()}%",
                        fontFamily = LexendFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = BrandTeal2, letterSpacing = (-0.32).sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                val sym = result.symmetryScore.coerceIn(50f, 100f)
                val leftPct  = 50f - (100f - sym) / 2f
                val rightPct = 100f - leftPct
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("L · ${leftPct.toInt()}%",  style = LabelCapsStyle, color = BrandNavy3)
                    Text("R · ${rightPct.toInt()}%", style = LabelCapsStyle, color = BrandTeal2)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(leftPct / 100f)
                            .background(Brush.horizontalGradient(listOf(BrandNavy2, BrandNavy3)))
                    )
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(BrandTeal2, BrandTeal))))
                }
            }

            // ── Action row ──────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = if (uiState.isPaused) "Resume" else "Pause",
                    icon = Icons.Default.Pause,
                    onClick = { viewModel.pauseSession() },
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "Mark",
                    icon = Icons.Default.Flag,
                    onClick = { viewModel.markEvent() },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Stop / start ────────────────────────────
            if (uiState.isSessionActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(14.dp), clip = false,
                            ambientColor = BrandRed.copy(alpha = 0.35f),
                            spotColor    = BrandRed.copy(alpha = 0.35f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandRed)
                        .clickable { viewModel.stopSession() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(99.dp)).padding(2.dp))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(14.dp), clip = false,
                            ambientColor = BrandNavy2.copy(alpha = 0.35f),
                            spotColor    = BrandNavy2.copy(alpha = 0.35f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(BrandNavy3, BrandNavy2)))
                        .clickable { viewModel.startSession() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PostureAnalysisCallout(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp), clip = false,
                ambientColor = BrandTeal.copy(alpha = 0.18f),
                spotColor = BrandTeal.copy(alpha = 0.24f))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(BrandTeal2, BrandNavy3)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccessibilityNew, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Check posture", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text("Open the live camera posture view", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: String, unit: String) {
    val t = MaterialTheme.gg
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontFamily = LexendFamily, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, color = t.ink, letterSpacing = (-0.36).sp)
            if (unit.isNotEmpty())
                Text(unit, fontSize = 10.sp, color = t.ink3, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
        Text(label.uppercase(), style = LabelCapsStyle, color = t.inkMute, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = MaterialTheme.gg
    TextButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(icon, null, tint = t.ink2, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = t.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

private fun mapStepIndices(timestamps: List<Long>): List<Int> {
    if (timestamps.isEmpty()) return emptyList()
    val lastTs = timestamps.last()
    val windowMs = GaitConstants.WINDOW_SAMPLES * 1000L / GaitConstants.SAMPLE_RATE_HZ
    return timestamps.mapNotNull { stepTs ->
        val offsetMs = lastTs - stepTs
        if (offsetMs > windowMs) null
        else {
            val sampleFromEnd = (offsetMs * GaitConstants.SAMPLE_RATE_HZ / 1000L).toInt()
            (GaitConstants.WINDOW_SAMPLES - 1 - sampleFromEnd).coerceAtLeast(0)
        }
    }
}

private fun mapEventIndices(timestamps: List<Long>): List<Int> {
    if (timestamps.isEmpty()) return emptyList()
    val now = System.currentTimeMillis()
    val windowMs = GaitConstants.WINDOW_SAMPLES * 1000L / GaitConstants.SAMPLE_RATE_HZ
    return timestamps.mapNotNull { eventTs ->
        val offsetMs = now - eventTs
        if (offsetMs !in 0..windowMs) null
        else {
            val sampleFromEnd = (offsetMs * GaitConstants.SAMPLE_RATE_HZ / 1000L).toInt()
            (GaitConstants.WINDOW_SAMPLES - 1 - sampleFromEnd).coerceIn(0, GaitConstants.WINDOW_SAMPLES - 1)
        }
    }
}
