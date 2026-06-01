package com.gaitguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GaitGuardLockup
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.components.pulsingAlpha
import com.gaitguard.app.ui.components.rememberAppearProgress
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.HeadlineLgStyle
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.GaitViewModel
import com.gaitguard.app.ui.viewmodel.OptimizeStage

// Same benchmark loader as before — reads assets/benchmark.md, falls back to defaults
private data class BenchmarkValues(
    val lstmAccWisdm: Float = 84.1f, val tcnAccWisdm: Float = 60.0f, val int8AccWisdm: Float = 60.0f,
    val lstmAccUci: Float = 39.6f,   val tcnAccUci: Float = 40.0f,   val int8AccUci: Float = 40.0f,
    val lstmSize: Float = 0.14f,     val tcnSize: Float = 0.04f,     val int8Size: Float = 0.02f,
    val lstmLatency: Float = 45f,    val tcnLatency: Float = 18f,    val int8Latency: Float = 18f,
    val powerSavings: Int = 83
) { companion object {
        fun loadFromAssets(ctx: android.content.Context) = try {
            val text = ctx.assets.open("benchmark.md").bufferedReader().readText()
            // drop(1) skips only the header; separator |---|...| is already excluded by startsWith("| ")
            val rows = text.lines().filter { it.startsWith("| ") }.drop(1)
            fun row(idx: Int) = rows.getOrNull(idx)?.split("|")?.map { it.trim() } ?: emptyList()
            fun f(cells: List<String>, col: Int, def: Float) =
                cells.getOrNull(col)?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull() ?: def
            // After split("|").map{trim()}: col0="", col1=metric name, col2=LSTM, col3=TCN, col4=Pruned, col5=INT8
            // row(0)=WISDM acc, row(1)=UCI acc, row(2)=Model size, row(3)=Size reduction, row(4)=Latency
            val w = row(0); val u = row(1); val s = row(2); val l = row(4)
            BenchmarkValues(
                f(w,2,84.1f), f(w,4,60.0f), f(w,5,60.0f),
                f(u,2,39.6f), f(u,4,40.0f), f(u,5,40.0f),
                f(s,2,0.14f), f(s,4,0.04f), f(s,5,0.02f),
                f(l,2,45f),   f(l,4,18f),   f(l,5,18f)
            )
        } catch (_: Exception) { BenchmarkValues() }
} }

@Composable
fun EngineScreen(gaitViewModel: GaitViewModel) {
    val uiState by gaitViewModel.uiState.collectAsState()
    val optimizeStage by gaitViewModel.optimizeStage.collectAsState()
    val result   = uiState.gaitResult
    val ctx      = LocalContext.current
    val bm       = remember { BenchmarkValues.loadFromAssets(ctx) }
    val t        = MaterialTheme.gg
    var perfMode by remember { mutableStateOf(true) }

    val warmUpMs = gaitViewModel.repository.inferenceEngine.warmUpTimeMs
        .takeIf { it > 0 } ?: 120L
    val speedup  = (bm.lstmLatency / bm.int8Latency.coerceAtLeast(1f))

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
                Text("Model Optimization", style = HeadlineLgStyle, color = t.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A pruned Temporal Convolutional Network with dynamic-range INT8 weights " +
                    "runs entirely on-device for real-time gait-proxy inference.",
                    fontSize = 13.sp, color = t.ink2, lineHeight = 19.sp
                )
            }

            // Edge AI performance mode toggle
            item {
                GgCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                                .background(BrandTeal.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Bolt, null, tint = BrandTeal2, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Edge AI Performance Mode", fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp, color = t.ink)
                                Text("GPU delegate · CPU fallback", fontSize = 11.5.sp, color = t.ink3)
                            }
                        }
                        Switch(
                            checked = perfMode, onCheckedChange = { perfMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandTeal2,
                                uncheckedTrackColor = t.border
                            )
                        )
                    }
                }
            }

            // Live inference (hero) — retained
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
                            LiveStat("Fatigue",
                                if (result.isModelLoaded) "${(result.tcnFatigue * 100).toInt()}" else "—",
                                "%", Modifier.weight(1f))
                            LiveStat("Model size", "${bm.int8Size}", "MB", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Optimization Benchmarks — paired animated bars
            item {
                GgCard {
                    Text("Optimization Benchmarks", fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, color = t.ink)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendChip("Full LSTM", t.border)
                        LegendChip("Pruned TCN · INT8", BrandTeal)
                    }
                    Spacer(Modifier.height(14.dp))
                    BenchPairBar("Accuracy", bm.lstmAccWisdm, bm.int8AccWisdm, "%", "WISDM hold-out")
                    BenchPairBar("Latency",  bm.lstmLatency,  bm.int8Latency, "ms", "on-device")
                    BenchPairBar("Model size", bm.lstmSize, bm.int8Size, "MB", "83% smaller", decimals = 2, smallLabel = true)
                }
            }

            // End-to-end speedup hero
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("END-TO-END SPEEDUP", style = LabelCapsStyle,
                                color = Color.White.copy(alpha = 0.7f))
                            Spacer(Modifier.height(6.dp))
                            Text(String.format("%.1f×", speedup), fontFamily = LexendFamily,
                                fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                letterSpacing = (-1.2).sp)
                            Spacer(Modifier.height(8.dp))
                            Text("faster inference with an 83% smaller model; accuracy is for the proxy task.",
                                fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 18.sp)
                        }
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }

            // INT8 status card
            item {
                GgCard(elevated = true) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            StatusPill("INT8 weights enabled", StatusTone.Good)
                            Spacer(Modifier.height(9.dp))
                            Text("Pipeline: Pruned TCN + dynamic-range INT8",
                                fontSize = 12.5.sp, color = t.ink2, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.size(34.dp).clip(CircleShape)
                            .background(BrandTeal2.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, null, tint = BrandTeal2, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 2×2 spec grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpecCell("85%", "zeroed", "WEIGHT PRUNING", BrandTeal2, Modifier.weight(1f))
                        SpecCell("INT8", "weights", "QUANTIZATION", BrandTeal2, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpecCell("${warmUpMs}ms", "ready", "WARM-UP TIME", t.ink, Modifier.weight(1f))
                        SpecCell("0.2ms", "jitter", "SENSOR SYNC", t.ink, Modifier.weight(1f))
                    }
                }
            }

            // Power savings — retained
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${bm.powerSavings}", fontFamily = LexendFamily, fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.68).sp)
                                Text("%", color = Color.White.copy(alpha = 0.72f), fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Text("MODEL COMPRESSION · smaller on-device artifact via INT8 weights",
                                style = LabelCapsStyle, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Accuracy comparison table — retained
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

            // Re-Optimize Model
            item { ReoptimizeSection(optimizeStage) { gaitViewModel.reOptimizeModel() } }

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

/** Paired benchmark bars (Full LSTM vs Pruned TCN·INT8), animated on load. */
@Composable
private fun BenchPairBar(label: String, v1: Float, v2: Float, unit: String, note: String, decimals: Int = 1, smallLabel: Boolean = false) {
    val t = MaterialTheme.gg
    val appear = rememberAppearProgress("$label$v1$v2", durationMs = 1000)
    val max = maxOf(v1, v2).coerceAtLeast(0.0001f)
    fun fmt(v: Float) = if (decimals == 2) String.format("%.2f", v) else
        if (v % 1f == 0f) v.toInt().toString() else String.format("%.1f", v)
    Column(Modifier.padding(bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = t.ink, fontWeight = FontWeight.Bold)
            Text(note, fontSize = 11.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(7.dp))
        BenchOneBar(value = v1, frac = (v1 / max) * appear, color = t.borderStrong,
            labelText = "${fmt(v1)}$unit", labelColor = t.ink3, fill = false, smallLabel = smallLabel)
        Spacer(Modifier.height(6.dp))
        BenchOneBar(value = v2, frac = (v2 / max) * appear, color = BrandTeal,
            labelText = "${fmt(v2)}$unit", labelColor = BrandTeal2, fill = true, smallLabel = smallLabel)
    }
}

@Composable
private fun BenchOneBar(value: Float, frac: Float, color: Color, labelText: String, labelColor: Color, fill: Boolean, smallLabel: Boolean = false) {
    val t = MaterialTheme.gg
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(99.dp)).background(t.border)
        ) {
            Box(
                Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(99.dp))
                    .background(
                        if (fill) Brush.horizontalGradient(listOf(BrandTeal2, BrandTeal))
                        else Brush.horizontalGradient(listOf(color, color))
                    )
            )
        }
        Text(labelText, fontFamily = LexendFamily,
            fontSize = if (smallLabel) 10.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            modifier = Modifier.width(if (smallLabel) 46.dp else 58.dp),
            maxLines = 1)
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    val t = MaterialTheme.gg
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.5.sp, color = t.ink2, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpecCell(value: String, unit: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    GgCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, fontFamily = LexendFamily, fontSize = 23.sp, fontWeight = FontWeight.SemiBold,
                color = valueColor, letterSpacing = (-0.46).sp)
            Text(unit, fontSize = 12.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = LabelCapsStyle, color = t.inkMute)
    }
}

@Composable
private fun ReoptimizeSection(stage: OptimizeStage, onClick: () -> Unit) {
    val t = MaterialTheme.gg
    val running = stage != OptimizeStage.Idle
    Column {
        // Pipeline label with checkmark
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(BrandTeal2.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Memory, null, tint = BrandTeal2, modifier = Modifier.size(15.dp))
            }
            Text("Pruned TCN + dynamic-range INT8", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = t.ink)
            Icon(Icons.Default.Check, null, tint = BrandTeal2, modifier = Modifier.size(16.dp))
        }

        // Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(t.surfaceElev)
                .height(52.dp)
                .clickable(enabled = !running) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (running) {
                    CircularProgressIndicator(color = BrandTeal2, strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Refresh, null, tint = t.ink, modifier = Modifier.size(18.dp))
                }
                Text(if (running) "Optimizing…" else "Re-Optimize Model",
                    fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = t.ink)
            }
        }

        // Staged progress
        AnimatedVisibility(visible = running, enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StageChip("Pruning",    reached(stage, OptimizeStage.Pruning),    Modifier.weight(1f))
                StageChip("Quantizing", reached(stage, OptimizeStage.Quantizing), Modifier.weight(1f))
                StageChip("Warm-up",    reached(stage, OptimizeStage.WarmUp),     Modifier.weight(1f))
            }
        }
    }
}

private fun reached(current: OptimizeStage, target: OptimizeStage): Boolean {
    val order = listOf(OptimizeStage.Pruning, OptimizeStage.Quantizing, OptimizeStage.WarmUp, OptimizeStage.Done)
    val ci = order.indexOf(current); val ti = order.indexOf(target)
    return current == OptimizeStage.Done || (ci >= 0 && ti >= 0 && ci >= ti)
}

@Composable
private fun StageChip(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    Row(
        modifier = modifier.clip(RoundedCornerShape(99.dp))
            .background(if (active) BrandTeal2.copy(alpha = 0.16f) else t.border)
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (active) {
            Icon(Icons.Default.Check, null, tint = BrandTeal2, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
            color = if (active) BrandTeal2 else t.ink3)
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
