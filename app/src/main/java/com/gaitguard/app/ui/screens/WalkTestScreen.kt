package com.gaitguard.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.theme.*
import com.gaitguard.app.ui.viewmodel.GaitViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun WalkTestScreen(viewModel: GaitViewModel, onBack: () -> Unit) {
    val t = MaterialTheme.gg
    val totalSec = 6 * 60
    var elapsedSec by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        while (elapsedSec < totalSec) { delay(1000); elapsedSec++ }
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBack,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                        .background(t.surfaceElev)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = t.ink2, modifier = Modifier.size(18.dp))
                }
                Text("6-Minute Walk Test", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                    color = t.ink, letterSpacing = (-0.17).sp)
            }

            StatusPill("In Progress", StatusTone.Warn)

            // Big timer with progress arc
            HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("TIME REMAINING", style = LabelCapsStyle, color = Color.White.copy(alpha = 0.75f))
                    Spacer(Modifier.height(6.dp))
                    val left = (totalSec - elapsedSec).coerceAtLeast(0)
                    val m = TimeUnit.SECONDS.toMinutes(left.toLong())
                    val s = left % 60
                    Text(String.format("%02d:%02d", m, s),
                        fontFamily = LexendFamily, fontSize = 64.sp,
                        fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-1.92).sp)
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.size(220.dp, 110.dp), contentAlignment = Alignment.BottomCenter) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val sw = 10.dp.toPx()
                            // Track
                            drawArc(
                                color = Color.White.copy(alpha = 0.18f),
                                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2),
                                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height * 2 - sw),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = StrokeCap.Round)
                            )
                            // Progress
                            drawArc(
                                brush = Brush.sweepGradient(listOf(BrandTeal, BrandTeal2, BrandTeal)),
                                startAngle = 180f,
                                sweepAngle = 180f * (elapsedSec.toFloat() / totalSec),
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2),
                                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height * 2 - sw),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = StrokeCap.Round)
                            )
                        }
                        Row(modifier = Modifier.padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(110.dp)) {
                            Text("0:00", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                            Text("6:00", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Live metrics row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniWalkStat("Distance", String.format("%.0f", elapsedSec * 1.32f / 60 * 60), "m", Modifier.weight(1f))
                MiniWalkStat("Steps",    "${(elapsedSec * 1.78f).toInt()}", "", Modifier.weight(1f))
                MiniWalkStat("Cadence",  "${uiState.gaitResult.cadenceSpm.toInt()}", "spm", Modifier.weight(1f))
            }

            // Instructions card
            GgCard(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(
                        BrandAmber.copy(alpha = 0.12f), BrandAmber.copy(alpha = 0.02f)
                    )))
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                        .background(BrandAmber), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                    Column {
                        Text("Keep walking at a steady pace",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB57500))
                        Text("Walk back and forth in a 30 m hallway. Pause if you feel unsafe — your score still counts.",
                            fontSize = 11.5.sp, color = t.ink3, lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(14.dp), clip = false,
                        ambientColor = BrandRed.copy(alpha = 0.35f),
                        spotColor    = BrandRed.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandRed)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("End Test Early", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MiniWalkStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    GgCard(modifier = modifier, padding = PaddingValues(11.dp)) {
        Text(label.uppercase(), style = LabelCapsStyle.copy(fontSize = 9.sp), color = t.inkMute)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text(value, fontFamily = LexendFamily, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = t.ink, letterSpacing = (-0.40).sp)
            if (unit.isNotEmpty())
                Text(unit, fontSize = 10.sp, color = t.ink3,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
    }
}
