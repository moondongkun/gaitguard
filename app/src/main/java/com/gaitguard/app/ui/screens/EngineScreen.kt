package com.gaitguard.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GaitGuardLockup
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.components.pulsingAlpha
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.BrandViolet
import com.gaitguard.app.ui.theme.HeadlineLgStyle
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.GaitViewModel

// Same benchmark loader as before — reads assets/benchmark.md, falls back to defaults
private data class BenchmarkValues(
    val lstmAccWisdm: Float = 84.7f, val tcnAccWisdm: Float = 81.5f, val int8AccWisdm: Float = 80.7f,
    val lstmAccUci: Float = 40.4f,   val tcnAccUci: Float = 40.0f,   val int8AccUci: Float = 39.6f,
    val lstmSize: Float = 0.14f,     val tcnSize: Float = 0.08f,     val int8Size: Float = 0.05f,
    val lstmLatency: Float = 45f,    val tcnLatency: Float = 18f,    val int8Latency: Float = 11f,
    val powerSavings: Int = 74
) { companion object {
        fun loadFromAssets(ctx: android.content.Context) = try {
            val text = ctx.assets.open("benchmark.md").bufferedReader().readText()
            val rows = text.lines().filter { it.startsWith("| ") }.drop(2)
            fun row(idx: Int) = rows.getOrNull(idx)?.split("|")?.map { it.trim() } ?: emptyList()
            fun f(cells: List<String>, col: Int, def: Float) =
                cells.getOrNull(col)?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull() ?: def
            val w = row(0); val u = row(1); val s = row(2); val l = row(4)
            BenchmarkValues(
                f(w,1,84.7f), f(w,2,81.5f), f(w,3,80.7f),
                f(u,1,40.4f), f(u,2,40.0f), f(u,3,39.6f),
                f(s,1,0.14f), f(s,2,0.08f), f(s,3,0.05f),
                f(l,1,45f),   f(l,2,18f),   f(l,3,11f)
            )
        } catch (_: Exception) { BenchmarkValues() }
} }

@Composable
fun EngineScreen(gaitViewModel: GaitViewModel) {
    val uiState by gaitViewModel.uiState.collectAsState()
    val result   = uiState.gaitResult
    val ctx      = LocalContext.current
    val bm       = remember { BenchmarkValues.loadFromAssets(ctx) }
    val t        = MaterialTheme.gg

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                StatusPill("Neural Engine Core", StatusTone.Info, dot = true)
                Spacer(Modifier.height(6.dp))
                Text("Model Performance &\nOptimization", style = HeadlineLgStyle, color = t.ink)
            }

            // Live inference (hero)
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Live Inference", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape)
                                    .background(BrandTeal.copy(alpha = pulsingAlpha())))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (result.isModelLoaded) "MODEL ACTIVE" else "SIGNAL ONLY",
                                    style = LabelCapsStyle,
                                    color = if (result.isModelLoaded) BrandTeal else Color(0xFFFFB4AB)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LiveStat("Latency",
                                if (result.inferenceLatencyMs > 0) "${result.inferenceLatencyMs}" else "—",
                                "ms", Modifier.weight(1f))
                            LiveStat("Warm-up",
                                "${gaitViewModel.repository.inferenceEngine.warmUpTimeMs}",
                                "ms", Modifier.weight(1f))
                            LiveStat("Fatigue",
                                if (result.isModelLoaded) "${(result.tcnFatigue * 100).toInt()}" else "—",
                                "%", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val maxRisk = maxOf(result.fallRiskLow, result.fallRiskMedium, result.fallRiskHigh)
                            val riskLabel = when (maxRisk) {
                                result.fallRiskHigh   -> "HIGH"
                                result.fallRiskMedium -> "MED"
                                else                  -> "LOW"
                            }
                            LiveStat("Fall risk",
                                if (result.isModelLoaded) riskLabel else "—", "",
                                Modifier.weight(1f), valueColor = BrandTeal)
                            LiveStat("Symmetry", "${result.symmetryScore.toInt()}", "%", Modifier.weight(1f))
                            LiveStat("Cadence",  "${result.cadenceSpm.toInt()}", "spm", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Benchmarks (rendered as our own bar charts for full theme control)
            item {
                GgCard {
                    Column {
                        Text("Optimization Benchmarks", fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = t.ink)
                        Text("FP32 LSTM to INT8 TCN", fontSize = 11.sp, color = t.ink3)
                        Spacer(Modifier.height(10.dp))
                        BenchBar("Accuracy (WISDM)", bm.lstmAccWisdm, bm.int8AccWisdm, "%")
                        BenchBar("Model size",       bm.lstmSize, bm.int8Size, "MB")
                        BenchBar("Latency",          bm.lstmLatency,  bm.int8Latency, "ms")
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LegendDot("FP32 LSTM", BrandNavy2)
                            LegendDot("INT8 TCN",  BrandTeal)
                        }
                    }
                }
            }

            // Power savings + status
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HeroGradientCard(modifier = Modifier.weight(1f), padding = PaddingValues(18.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(BrandTeal.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Bolt, null, tint = BrandTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${bm.powerSavings}", fontFamily = LexendFamily, fontSize = 32.sp,
                                    fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.64).sp)
                                Text("%", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 3.dp))
                            }
                            Text("POWER SAVINGS", style = LabelCapsStyle,
                                color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    GgCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(
                                BrandTeal.copy(alpha = 0.20f), BrandTeal.copy(alpha = 0.05f)
                            ))),
                        padding = PaddingValues(16.dp)
                    ) {
                        StatusPill("Active", StatusTone.Good)
                        Spacer(Modifier.height(6.dp))
                        Text("Quantized INT8", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = t.ink)
                        Text("Pruned TCN + INT8", fontSize = 11.sp, color = t.ink3)
                    }
                }
            }

            // Accuracy table
            item {
                GgCard {
                    Text("Proxy Accuracy Comparison", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = t.ink)
                    Spacer(Modifier.height(10.dp))
                    TableHeader(); TableRow("WISDM proxy",  "${bm.lstmAccWisdm}%", "${bm.tcnAccWisdm}%", "${bm.int8AccWisdm}%")
                    TableRow("UCI sanity", "${bm.lstmAccUci}%",   "${bm.tcnAccUci}%",   "${bm.int8AccUci}%")
                    TableRow("Model size", "${bm.lstmSize}MB",    "${bm.tcnSize}MB",    "${bm.int8Size}MB")
                    TableRow("Latency",    "${bm.lstmLatency}ms", "${bm.tcnLatency}ms", "${bm.int8Latency}ms")
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun LiveStat(label: String, value: String, unit: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = LabelCapsStyle, color = Color.White.copy(alpha = 0.65f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontFamily = LexendFamily, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = valueColor, letterSpacing = (-0.40).sp)
            if (unit.isNotEmpty()) Text(unit, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
    }
}

@Composable
private fun BenchBar(label: String, v1: Float, v2: Float, unit: String) {
    val t = MaterialTheme.gg
    val max = maxOf(v1, v2).coerceAtLeast(1f)
    Column(Modifier.padding(bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, color = t.ink2, fontWeight = FontWeight.SemiBold)
            Text(unit,  fontSize = 11.sp, color = t.ink3)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(v1 / max)
                .height(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(BrandNavy2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Text(String.format("%.1f", v1), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(v2 / max)
                .height(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.horizontalGradient(listOf(BrandTeal2, BrandTeal))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Text(String.format("%.1f", v2), fontSize = 10.sp, color = Color(0xFF003322), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    val t = MaterialTheme.gg
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.5.sp, color = t.ink3, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TableHeader() {
    val t = MaterialTheme.gg
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Metric" to t.inkMute, "LSTM" to t.inkMute, "TCN" to t.inkMute, "INT8" to BrandTeal2).forEach { (s, c) ->
            Text(s.uppercase(), style = LabelCapsStyle, color = c, modifier = Modifier.weight(1f))
        }
    }
}
@Composable
private fun TableRow(c1: String, c2: String, c3: String, c4: String) {
    val t = MaterialTheme.gg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(t.border, RoundedCornerShape(99.dp))
            .height(1.dp)
    ) {}
    Spacer(Modifier.height(5.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(c1, fontSize = 12.sp, color = t.ink2, modifier = Modifier.weight(1f))
        Text(c2, fontSize = 12.sp, color = t.ink3, modifier = Modifier.weight(1f))
        Text(c3, fontSize = 12.sp, color = t.ink3, modifier = Modifier.weight(1f))
        Text(c4, fontSize = 12.sp, color = BrandTeal2, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}
