package com.gaitguard.app.ui.screens

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaitguard.app.data.db.FatigueTimelinePoint
import com.gaitguard.app.data.db.SessionRecord
import com.gaitguard.app.data.repository.GaitRepository
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── ViewModel (unchanged structure — kept inline so the file is drop-in) ──
class SessionReplayViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = GaitRepository(app.applicationContext)
    private val _session  = MutableStateFlow<SessionRecord?>(null)
    val session: StateFlow<SessionRecord?> = _session
    private val _timeline = MutableStateFlow<List<FatigueTimelinePoint>>(emptyList())
    val timeline: StateFlow<List<FatigueTimelinePoint>> = _timeline

    fun load(sessionId: Long) {
        viewModelScope.launch { _timeline.value = repository.getTimelineForSession(sessionId) }
        viewModelScope.launch {
            repository.allSessions.collect { all -> _session.value = all.firstOrNull { it.id == sessionId } }
        }
    }
}

@Composable
fun SessionReplayScreen(
    sessionId: Long,
    viewModel: SessionReplayViewModel,
    onBack: () -> Unit
) {
    val t = MaterialTheme.gg
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val s by viewModel.session.collectAsState()
    val timeline by viewModel.timeline.collectAsState()

    val dateStr = s?.let {
        SimpleDateFormat("EEEE, MMM d - HH:mm", Locale.US).format(Date(it.date))
    } ?: ""

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                            .background(t.surfaceElev)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = t.ink2, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Session Replay", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = t.ink)
                        Text(dateStr.ifEmpty { "No session selected" }, fontSize = 11.sp, color = t.ink3,
                            modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }

            // ── Summary hero ─────────────────────────────────
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            BigStat("Duration",  "${s?.durationMinutes ?: 0}m")
                            BigStat("Peak Fatigue", "${s?.peakFatigueScore?.toInt() ?: 0}%")
                            BigStat("Endurance", "${s?.enduranceScore?.toInt() ?: 0}")
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipStat("Symmetry",   "${s?.avgSymmetryScore?.toInt() ?: 0}%",
                                BrandTeal, Modifier.weight(1f))
                            ChipStat("Fall Events", "${s?.fallRiskEventCount ?: 0}",
                                if ((s?.fallRiskEventCount ?: 0) > 0) BrandRed else BrandTeal, Modifier.weight(1f))
                            ChipStat("Stumbles",   "${s?.microStumbleCount ?: 0}",
                                if ((s?.microStumbleCount ?: 0) > 0) BrandAmber else BrandTeal, Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Fatigue curve ─────────────────────────────────
            item {
                GgCard {
                    Text("Fatigue over time", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = t.ink)
                    Spacer(Modifier.height(10.dp))
                    FatigueCurve(timeline)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0:00", fontSize = 9.5.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
                        Text("${(s?.durationMinutes ?: 0)} min",
                            fontSize = 9.5.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Timeline ─────────────────────────────────────
            item {
                GgCard {
                    Text("EVENT TIMELINE", style = LabelCapsStyle, color = t.inkMute)
                    Spacer(Modifier.height(8.dp))
                    if (timeline.isEmpty()) {
                        Text("No timeline points yet - replay data will appear here.",
                            fontSize = 12.sp, color = t.ink3,
                            modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        timeline.filter { it.isMarked || it.isStumble || it.fatigueScore >= 40f }
                            .ifEmpty { timeline.take(6) }
                            .take(8)
                            .forEach { p ->
                                val min = p.elapsedSeconds / 60
                                val sec = p.elapsedSeconds % 60
                                val label = when {
                                    p.isStumble -> "Stumble - fatigue ${p.fatigueScore.toInt()}%, sym ${p.symmetryScore.toInt()}%"
                                    p.isMarked -> "Marked - fatigue ${p.fatigueScore.toInt()}%, sym ${p.symmetryScore.toInt()}%"
                                    else -> "Fatigue ${p.fatigueScore.toInt()}%, sym ${p.symmetryScore.toInt()}%"
                                }
                                EventRow(
                                    "%d:%02d".format(min, sec),
                                    label,
                                    if (p.isStumble) BrandRed
                                    else if (p.isMarked) BrandAmber
                                    else if (p.fatigueScore >= 70) BrandRed
                                    else if (p.fatigueScore >= 40) BrandAmber else BrandTeal
                                )
                            }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = LabelCapsStyle, color = Color.White.copy(alpha = 0.7f))
        Text(value, fontFamily = LexendFamily, fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.44).sp,
            modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ChipStat(label: String, value: String, fg: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier
        .clip(RoundedCornerShape(10.dp))
        .background(Color.White.copy(alpha = 0.10f))
        .padding(vertical = 8.dp, horizontal = 12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {
            Text(value, fontFamily = LexendFamily, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, color = fg, letterSpacing = (-0.32).sp)
            Text(label.uppercase(), style = LabelCapsStyle, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun FatigueCurve(points: List<FatigueTimelinePoint>) {
    val t = MaterialTheme.gg
    if (points.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(118.dp), contentAlignment = Alignment.Center) {
            Text("No data", fontSize = 12.sp, color = t.ink3)
        }
        return
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(118.dp)) {
        val w = size.width; val h = size.height
        val leftPad = 28f
        val bottomPad = 16f
        val chartW = w - leftPad
        val chartH = h - bottomPad - 8f
        fun yFor(v: Float) = 8f + (1f - v.coerceIn(0f, 100f) / 100f) * chartH
        fun xFor(i: Int) = leftPad + i * chartW / (points.size - 1).coerceAtLeast(1)

        listOf(0f, 50f, 100f).forEach { tick ->
            val y = yFor(tick)
            drawLine(t.border, Offset(leftPad, y), Offset(w, y), strokeWidth = 1f)
        }
        val pts = points.mapIndexed { i, p -> Offset(xFor(i), yFor(p.fatigueScore)) }
        val areaPath = Path().apply {
            moveTo(leftPad, h - bottomPad); pts.forEach { lineTo(it.x, it.y) }
            lineTo(w, h - bottomPad); close()
        }
        drawPath(areaPath, brush = Brush.verticalGradient(
            listOf(BrandAmber.copy(alpha = 0.35f), BrandAmber.copy(alpha = 0f))
        ))
        val linePath = Path().apply {
            pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(linePath, color = BrandAmber, style = Stroke(width = 2.4f))
        val ty = yFor(40f)
        drawLine(BrandAmber.copy(alpha = 0.4f), Offset(leftPad, ty), Offset(w, ty),
            strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
        points.forEachIndexed { i, p ->
            val color = when {
                p.isStumble -> BrandRed
                p.isMarked -> BrandAmber
                else -> null
            }
            if (color != null) {
                val center = Offset(xFor(i), yFor(p.fatigueScore))
                drawCircle(color = Color.White, radius = 6.5f, center = center)
                drawCircle(color = color, radius = 4.5f, center = center)
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Y-axis 0-100", fontSize = 9.5.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
        Text("Amber mark / Red stumble", fontSize = 9.5.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EventRow(time: String, label: String, color: Color) {
    val t = MaterialTheme.gg
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(time, fontFamily = LexendFamily, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, color = t.ink3,
            modifier = Modifier.padding(end = 10.dp))
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(10.dp))
        Text(label, fontSize = 12.5.sp, color = t.ink2, fontWeight = FontWeight.Medium)
    }
}
