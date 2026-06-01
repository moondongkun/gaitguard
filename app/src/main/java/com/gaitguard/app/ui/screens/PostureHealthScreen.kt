package com.gaitguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.data.db.PostureSnapshot
import com.gaitguard.app.data.model.MetricSeverity
import com.gaitguard.app.data.model.PostureResult
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.PosturePhotoThumbnail
import com.gaitguard.app.ui.components.rememberAppearProgress
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.theme.BrandAmber
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandRed
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.BrandViolet
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.gg
import com.gaitguard.app.ui.viewmodel.PostureViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ── Data types ────────────────────────────────────────────────────────────────

data class RecoveryExercise(
    val name: String,
    val targetArea: String,
    val category: String,       // used by filter chips
    val level: String,
    val durationMin: Int,
    val description: String,
    val accent: Color
)

private data class InsightItem(
    val tone: StatusTone,
    val title: String,
    val body: String
)

// ── Static exercise library ───────────────────────────────────────────────────

private fun allExerciseLibrary(): List<RecoveryExercise> = listOf(
    RecoveryExercise(
        "Cervical Reset Routine", "Head & Neck", "Neck", "Level 1", 5,
        "Gentle chin tucks + neck rotations to restore the natural cervical curve and relieve tension.",
        BrandTeal2
    ),
    RecoveryExercise(
        "Neck Half-Circles", "Neck Mobility", "Neck", "Mild", 4,
        "Slow neck half-circles (front arc only) to release sternocleidomastoid tension without strain.",
        BrandTeal
    ),
    RecoveryExercise(
        "Cat-Cow Flow", "Spinal Mobility", "Thoracic", "Mild", 8,
        "Alternating spinal flexion and extension lubricates vertebral joints and relieves thoracic tension.",
        BrandNavy3
    ),
    RecoveryExercise(
        "Thoracic Extension Stretch", "Mid-Back Focus", "Thoracic", "Level 2", 10,
        "Foam roller mobilization from T4 to T9. Reduces kyphosis and meaningfully improves breathing depth.",
        BrandNavy3
    ),
    RecoveryExercise(
        "Wall Slides", "Shoulder & Upper Back", "Shoulders", "Level 1", 5,
        "Slide arms up a wall keeping lower back flat. Opens the chest and restores scapular mobility.",
        BrandTeal2
    ),
    RecoveryExercise(
        "Wall Angels", "Shoulders", "Shoulders", "Level 2", 5,
        "Stand against a wall and slide arms overhead — keep the full back flat to retrain scapular control.",
        BrandAmber
    ),
    RecoveryExercise(
        "Shoulder Blade Squeeze", "Shoulders", "Shoulders", "Level 1", 4,
        "Retract both scapulae for 5 s, 10 reps × 3 sets. Directly strengthens the lower trapezius.",
        BrandTeal2
    ),
    RecoveryExercise(
        "Hip Hinge Reset", "Pelvic Alignment", "Hips", "Daily", 6,
        "Hinge at hips while keeping the spine neutral. Trains glute activation and reduces anterior pelvic tilt.",
        BrandAmber
    ),
    RecoveryExercise(
        "Hip Flexor Release", "Hips & Pelvis", "Hips", "Level 1", 6,
        "90/90 hip stretch — hold 45 s each side. Essential for restoring a neutral pelvic position.",
        BrandAmber
    ),
    RecoveryExercise(
        "Dead Bug Core Activation", "Core & Stability", "Full Body", "Level 2", 8,
        "Contralateral limb extension while pressing the lower back to the floor. Builds deep spinal stability.",
        BrandNavy3
    ),
    RecoveryExercise(
        "Postural Awareness Walk", "Full Spine", "Full Body", "Level 1", 10,
        "Walk heel-to-toe stacking head over shoulders over hips. Reinforces proprioceptive posture habits.",
        BrandTeal
    ),
    RecoveryExercise(
        "Daily Mobility Flow", "Full Body", "Full Body", "Level 1", 10,
        "10-minute gentle routine to sustain excellent posture and maintain full joint range of motion.",
        BrandTeal2
    )
)

// ── Personalised recommendation (top result) ─────────────────────────────────

private fun recommendedExercises(result: PostureResult): List<RecoveryExercise> = buildList {
    if (result.kyphosisSeverity != MetricSeverity.NORMAL) {
        add(allExerciseLibrary().first { it.name == "Cervical Reset Routine" })
        add(allExerciseLibrary().first { it.name == "Thoracic Extension Stretch" })
    }
    if (result.shoulderSeverity != MetricSeverity.NORMAL) {
        add(allExerciseLibrary().first { it.name == "Wall Angels" })
        add(allExerciseLibrary().first { it.name == "Shoulder Blade Squeeze" })
    }
    if (result.pelvicSeverity != MetricSeverity.NORMAL) {
        add(allExerciseLibrary().first { it.name == "Hip Flexor Release" })
        add(allExerciseLibrary().first { it.name == "Dead Bug Core Activation" })
    }
    if (!result.axialAligned) {
        add(allExerciseLibrary().first { it.name == "Postural Awareness Walk" })
    }
    if (isEmpty()) add(allExerciseLibrary().first { it.name == "Daily Mobility Flow" })
}

// ── Rule-based insights ───────────────────────────────────────────────────────

private fun generateInsights(result: PostureResult): List<InsightItem> = buildList {
    when (result.kyphosisSeverity) {
        MetricSeverity.ALERT -> add(InsightItem(StatusTone.Bad, "Forward Head Posture",
            "Your forward lean (${result.kyphosisAngleDeg.toInt()}°) adds significant load on cervical vertebrae — roughly 1 kg per extra degree. This commonly causes headaches, upper-back tension, and restricted breathing."))
        MetricSeverity.WATCH -> add(InsightItem(StatusTone.Warn, "Mild Neck Tilt",
            "A slight forward tilt (${result.kyphosisAngleDeg.toInt()}°) detected. With prolonged screen use, 'tech neck' can develop gradually. Simple chin tucks performed 3× daily will help."))
        MetricSeverity.NORMAL -> add(InsightItem(StatusTone.Good, "Healthy Neck Posture",
            "Cervical angle (${result.kyphosisAngleDeg.toInt()}°) is within the optimal range. Keep screens at eye level to sustain this alignment."))
    }
    when (result.shoulderSeverity) {
        MetricSeverity.ALERT -> add(InsightItem(StatusTone.Bad, "Shoulder Imbalance",
            "Visible unevenness (${abs(result.shoulderLevelDeg).toInt()}°). Chronic imbalance causes unequal muscle development and raises rotator cuff injury risk. Check bag-carrying habits and desk ergonomics."))
        MetricSeverity.WATCH -> add(InsightItem(StatusTone.Warn, "Slight Shoulder Tilt",
            "${abs(result.shoulderLevelDeg).toInt()}° tilt noted. Strengthening the lower trapezius and correcting desk height usually resolves this."))
        MetricSeverity.NORMAL -> { /* omit for brevity when all good */ }
    }
    if (!result.axialAligned) add(InsightItem(StatusTone.Warn, "Lateral Deviation",
        "Your spine is drifting from the central axis. Core activation exercises and mindful standing posture can correct this over time."))
    when (result.pelvicSeverity) {
        MetricSeverity.ALERT -> add(InsightItem(StatusTone.Bad, "Significant Pelvic Tilt",
            "${abs(result.pelvicTiltDeg).toInt()}° tilt exceeds the clinical threshold. Tight hip flexors or weak glutes are common causes — hip release and glute activation exercises are strongly recommended."))
        MetricSeverity.WATCH -> add(InsightItem(StatusTone.Warn, "Minor Hip Tilt",
            "${abs(result.pelvicTiltDeg).toInt()}° tilt. Stretching the hip flexors and activating the core helps restore a neutral pelvis and reduces lower-back strain."))
        MetricSeverity.NORMAL -> { }
    }
    if (isEmpty()) add(InsightItem(StatusTone.Good, "Excellent Alignment",
        "All four biomechanical metrics are within their optimal ranges. Your posture today is excellent — keep it up!"))
}

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun PostureHealthScreen(
    viewModel: PostureViewModel,
    onBack: () -> Unit
) {
    val result       by viewModel.postureFlow.collectAsState()
    val weeklyScores by viewModel.weeklyPostureScores.collectAsState()
    val lastSnapshot by viewModel.lastSnapshotTime.collectAsState()
    val photoRecords by viewModel.photoRecords.collectAsState()
    val t = MaterialTheme.gg

    val allExercises = remember { allExerciseLibrary() }
    val recommended  = remember(result) { recommendedExercises(result) }
    val insights     = remember(result) { generateInsights(result) }
    val featured     = remember(result) { recommended.firstOrNull() ?: allExercises.first() }

    val categories = listOf("All", "Neck", "Thoracic", "Shoulders", "Hips", "Full Body")
    var selectedCategory by remember { mutableStateOf("All") }
    val filteredExercises = remember(selectedCategory) {
        if (selectedCategory == "All") allExercises
        else allExercises.filter { it.category == selectedCategory }
    }

    val gradeTone = when {
        result.spinalHealthScore >= 70 -> StatusTone.Good
        result.spinalHealthScore >= 50 -> StatusTone.Warn
        else                           -> StatusTone.Bad
    }
    val weekDelta = run {
        val nonZero = weeklyScores.filter { it > 0f }
        if (nonZero.size >= 2) (nonZero.last() - nonZero.first()).toInt() else 0
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Header ───────────────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(t.surfaceElev)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = t.ink2, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Spinal Health", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            color = t.ink, letterSpacing = (-0.18).sp)
                        Text("Posture Assessment", fontSize = 11.sp, color = t.ink3)
                    }
                    Spacer(Modifier.weight(1f))
                    StatusPill(result.clinicalGrade, gradeTone, dot = false)
                }
            }

            // ── Hero: score ring + grade + last assessment ────────────
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SpinalHealthRing(score = result.spinalHealthScore)
                        Column(Modifier.weight(1f)) {
                            Text("SPINAL HEALTH SCORE", style = LabelCapsStyle,
                                color = Color.White.copy(alpha = 0.70f))
                            Spacer(Modifier.height(4.dp))
                            Text(result.clinicalGrade, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color.White, letterSpacing = (-0.30).sp, lineHeight = 24.sp)
                            Spacer(Modifier.height(8.dp))
                            // Weekly delta chip
                            if (weekDelta != 0) {
                                val deltaColor = if (weekDelta > 0) BrandTeal else BrandAmber
                                val deltaIcon  = if (weekDelta > 0) Icons.Default.ArrowUpward
                                                 else Icons.Default.ArrowDownward
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(deltaColor.copy(alpha = 0.20f))
                                        .padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(deltaIcon, null, tint = deltaColor,
                                        modifier = Modifier.size(11.dp))
                                    Text(
                                        "${if (weekDelta > 0) "+" else ""}$weekDelta pts this week",
                                        fontSize = 10.sp, color = deltaColor, fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                            // Last snapshot time
                            val lastStr = lastSnapshot?.let {
                                SimpleDateFormat("MMM d · HH:mm", Locale.US).format(Date(it))
                            } ?: "No snapshots yet"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(Modifier.size(5.dp).clip(CircleShape)
                                    .background(BrandTeal.copy(alpha = 0.80f)))
                                Text("Last: $lastStr", fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.65f))
                            }
                        }
                    }
                }
            }

            // ── Weekly Posture Index bar chart ────────────────────────
            item {
                GgCard(elevated = true) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WEEKLY POSTURE INDEX", style = LabelCapsStyle, color = t.inkMute)
                            Spacer(Modifier.height(3.dp))
                            val last = weeklyScores.lastOrNull { it > 0f }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "${last?.toInt() ?: "—"}",
                                    fontFamily = LexendFamily, fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold, color = t.ink,
                                    letterSpacing = (-0.44).sp
                                )
                                Text("/ 100", fontSize = 11.sp, color = t.ink3,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 3.dp))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(BrandNavy3))
                            Text("Posture Index", fontSize = 10.sp, color = t.ink3,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    WeeklyBarChart(weeklyScores)
                }
            }

            // ── Top Insights ──────────────────────────────────────────
            item {
                GgCard {
                    Text("TOP INSIGHTS", style = LabelCapsStyle, color = t.inkMute)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        insights.take(3).forEach { PostureInsightRow(it) }
                    }
                }
            }

            // ── Anatomy Breakdown ─────────────────────────────────────
            item {
                Text("ANATOMY BREAKDOWN", style = LabelCapsStyle, color = t.inkMute,
                    modifier = Modifier.padding(start = 4.dp))
            }

            item {
                BodyAreaAccordion(
                    title    = "Head & Neck",
                    severity = result.kyphosisSeverity,
                    summary  = "${result.kyphosisAngleDeg.toInt()}° forward lean",
                    metrics  = listOf(
                        "Kyphosis" to "${result.kyphosisAngleDeg.toInt()}°",
                        "Status"   to result.kyphosisSeverity.name.lowercase()
                            .replaceFirstChar { it.uppercase() }
                    ),
                    insight = when (result.kyphosisSeverity) {
                        MetricSeverity.ALERT ->
                            "Your neck is significantly tilted forward. Each extra degree of forward head posture adds ~1 kg of load on your cervical vertebrae, commonly causing headaches and upper-back pain."
                        MetricSeverity.WATCH ->
                            "A mild forward tilt detected. Prolonged screen time can worsen this into 'tech neck'. Simple chin tucks performed 3× daily reduce cervical strain significantly."
                        MetricSeverity.NORMAL ->
                            "Excellent cervical alignment. Keep screens at eye level and avoid prolonged downward phone-gazing to sustain this."
                    }
                )
            }

            item {
                BodyAreaAccordion(
                    title    = "Shoulders",
                    severity = result.shoulderSeverity,
                    summary  = "${abs(result.shoulderLevelDeg).toInt()}° tilt",
                    metrics  = listOf(
                        "Level Diff"  to "${abs(result.shoulderLevelDeg).toInt()}°",
                        "Direction"   to if (result.shoulderLevelDeg > 0) "Right High" else "Left High"
                    ),
                    insight = when (result.shoulderSeverity) {
                        MetricSeverity.ALERT ->
                            "Visible shoulder imbalance detected. Chronic unevenness leads to unequal muscle development and raises rotator cuff injury risk. Check your bag-carrying habits and desk ergonomics."
                        MetricSeverity.WATCH ->
                            "Slight unevenness. Strengthen the lower trapezius with shoulder blade squeeze exercises and consciously relax both shoulders throughout the day."
                        MetricSeverity.NORMAL ->
                            "Shoulders are level. Maintain this with regular upper-back mobility work and avoid consistently carrying weight on one side."
                    }
                )
            }

            item {
                BodyAreaAccordion(
                    title    = "Spine & Trunk",
                    severity = result.kyphosisSeverity,
                    summary  = if (result.axialAligned) "Axially aligned · ${result.kyphosisAngleDeg.toInt()}°"
                               else "Lateral deviation detected",
                    metrics  = listOf(
                        "Thoracic Angle" to "${result.kyphosisAngleDeg.toInt()}°",
                        "Axial"          to if (result.axialAligned) "Aligned" else "Deviated"
                    ),
                    insight = "Thoracic angle ${result.kyphosisAngleDeg.toInt()}°. " + when {
                        !result.axialAligned ->
                            "Lateral deviation detected — may indicate a muscle-tension imbalance or structural asymmetry. Consult a physiotherapist if this persists."
                        result.kyphosisSeverity == MetricSeverity.ALERT ->
                            "Excessive rounding compresses the lungs and reduces breathing capacity by up to 30%. Thoracic extension stretches and chest-opening exercises are strongly advised."
                        result.kyphosisSeverity == MetricSeverity.WATCH ->
                            "Mild rounding. Prone cobra poses and row exercises help restore the healthy neutral thoracic curve (target: 20–45°)."
                        else ->
                            "Healthy thoracic kyphosis within the optimal range. Your current morning stretch routine is working well."
                    }
                )
            }

            item {
                BodyAreaAccordion(
                    title    = "Hips & Pelvis",
                    severity = result.pelvicSeverity,
                    summary  = "${abs(result.pelvicTiltDeg).toInt()}° pelvic tilt",
                    metrics  = listOf(
                        "Pelvic Tilt" to "${abs(result.pelvicTiltDeg).toInt()}°",
                        "Direction"   to if (result.pelvicTiltDeg > 0) "Anterior" else "Posterior"
                    ),
                    insight = when (result.pelvicSeverity) {
                        MetricSeverity.ALERT ->
                            "Pelvic tilt exceeds the clinical threshold. Tight hip flexors or weak glutes are the most common cause. Hip flexor release and glute activation exercises are strongly recommended."
                        MetricSeverity.WATCH ->
                            "Minor tilt detected. Stretching the hip flexors and activating the core muscles helps restore a neutral pelvis and reduces lower-back strain."
                        MetricSeverity.NORMAL ->
                            "Pelvic alignment is neutral. Maintain this with balanced lower-body strengthening and regular movement breaks throughout the day."
                    }
                )
            }

            // ── Photo Assessment History ──────────────────────────────
            if (photoRecords.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(2.dp))
                    Text("PHOTO ASSESSMENTS", style = LabelCapsStyle, color = t.inkMute,
                        modifier = Modifier.padding(start = 4.dp))
                }
                items(photoRecords.take(5)) { snap ->
                    PhotoRecordCard(snap, onDelete = { viewModel.deletePhotoRecord(snap.id) })
                }
            }

            // ── Recovery Exercises header ─────────────────────────────
            item {
                Spacer(Modifier.height(2.dp))
                Text("RECOMMENDED FOR YOU", style = LabelCapsStyle, color = t.inkMute,
                    modifier = Modifier.padding(start = 4.dp))
            }

            // ── Featured / priority exercise hero card ────────────────
            item { FeaturedExerciseBanner(featured) }

            item {
                Text("EXERCISE LIBRARY", style = LabelCapsStyle, color = t.inkMute,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            }

            // ── Category filter chips ─────────────────────────────────
            item { CategoryFilterRow(categories, selectedCategory) { selectedCategory = it } }

            // ── Exercise library ──────────────────────────────────────
            items(filteredExercises) { ex -> ExerciseCard(ex) }

            // ── Motivation footer ─────────────────────────────────────
            item { InsightMotivationCard() }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Weekly bar chart (day-labelled) ──────────────────────────────────────────

@Composable
private fun WeeklyBarChart(data: List<Float>) {
    val t = MaterialTheme.gg
    val dayLabels = remember {
        val cal = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY    -> "M"
                Calendar.TUESDAY   -> "T"
                Calendar.WEDNESDAY -> "W"
                Calendar.THURSDAY  -> "T"
                Calendar.FRIDAY    -> "F"
                Calendar.SATURDAY  -> "S"
                else               -> "S"  // SUNDAY
            }
        }
    }

    val barData = when {
        data.size >= 7 -> data.takeLast(7)
        else           -> data + List(7 - data.size) { 0f }
    }
    val maxVal = barData.maxOrNull()?.coerceAtLeast(1f) ?: 100f
    val appear = rememberAppearProgress(data, durationMs = 1000)

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val barCount  = 7
            val gap       = 6.dp.toPx()
            val barWidth  = (size.width - gap * (barCount - 1)) / barCount
            val maxBarH   = size.height - 4.dp.toPx()

            barData.forEachIndexed { i, value ->
                // left→right staggered grow
                val barProg = ((appear * 7f) - i).coerceIn(0f, 1f)
                val fullH   = if (value > 0f) (value / maxVal) * maxBarH else 4.dp.toPx()
                val barH    = fullH * barProg
                val left    = i * (barWidth + gap)
                val top     = size.height - barH
                val barColor = when {
                    value <= 0f  -> Color.Gray.copy(alpha = 0.18f)
                    value >= 70f -> BrandTeal
                    value >= 50f -> BrandAmber
                    else         -> BrandRed
                }
                // Track background
                drawRoundRect(
                    color      = t.border.copy(alpha = 0.35f),
                    topLeft    = Offset(left, 0f),
                    size       = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                // Filled bar
                if (barH > 0.5f) {
                    drawRoundRect(
                        brush  = Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.55f)),
                            startY = top, endY = size.height
                        ),
                        topLeft      = Offset(left, top),
                        size         = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            dayLabels.forEach { label ->
                Text(label, fontSize = 10.sp, color = t.inkMute, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Posture insight row ───────────────────────────────────────────────────────

@Composable
private fun PostureInsightRow(item: InsightItem) {
    val t = MaterialTheme.gg
    val dotColor = when (item.tone) {
        StatusTone.Good    -> BrandTeal2
        StatusTone.Warn    -> BrandAmber
        StatusTone.Bad     -> BrandRed
        StatusTone.Info    -> BrandNavy3
        StatusTone.Neutral -> t.ink3
    }
    val bgColor = dotColor.copy(alpha = 0.08f)
    val icon = when (item.tone) {
        StatusTone.Good    -> Icons.Default.CheckCircle
        StatusTone.Warn    -> Icons.Default.Lightbulb
        StatusTone.Bad     -> Icons.Default.Warning
        else               -> Icons.Default.Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(dotColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = dotColor, modifier = Modifier.size(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = t.ink)
            Text(item.body, fontSize = 11.5.sp, color = t.ink2, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ── Featured exercise hero banner ─────────────────────────────────────────────

@Composable
private fun FeaturedExerciseBanner(ex: RecoveryExercise) {
    HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Priority tag pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.Stars, null, tint = BrandTeal,
                    modifier = Modifier.size(12.dp))
                Text(
                    "TOP MATCH - ${ex.targetArea.uppercase()}",
                    style = LabelCapsStyle, color = Color.White.copy(alpha = 0.90f)
                )
            }
            Text(
                ex.name, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.White, letterSpacing = (-0.42).sp, lineHeight = 26.sp
            )
            Text(
                ex.description, fontSize = 13.sp, color = Color.White.copy(alpha = 0.80f),
                lineHeight = 19.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LevelChip(ex.level, BrandTeal)
                LevelChip("${ex.durationMin} min", BrandTeal, Icons.Default.Schedule)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Start", fontSize = 12.sp, color = Color.White,
                            fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                            tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ── Category filter chips (horizontal scroll) ─────────────────────────────────

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val t = MaterialTheme.gg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val active = cat == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (active) Brush.linearGradient(listOf(BrandNavy3, BrandNavy2))
                        else Brush.linearGradient(listOf(t.surfaceElev, t.surfaceElev))
                    )
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (active) Color.White else t.ink3
                )
            }
        }
    }
}

// ── Exercise card ─────────────────────────────────────────────────────────────

@Composable
private fun ExerciseCard(ex: RecoveryExercise) {
    val t = MaterialTheme.gg
    GgCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ex.accent.copy(alpha = 0.22f), ex.accent.copy(alpha = 0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val exerciseIcon = when (ex.category) {
                    "Neck"     -> Icons.Default.Person
                    "Thoracic" -> Icons.Default.AccessibilityNew
                    "Hips"     -> Icons.Default.AccessibilityNew
                    else       -> Icons.Default.FitnessCenter
                }
                Icon(exerciseIcon, null, tint = ex.accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(ex.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = t.ink,
                    letterSpacing = (-0.13).sp)
                Text(ex.targetArea, fontSize = 11.sp, color = t.ink3,
                    modifier = Modifier.padding(top = 1.dp))
                Text(ex.description, fontSize = 11.sp, color = t.ink3, lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LevelChip(ex.level, ex.accent)
            LevelChip("${ex.durationMin} min", BrandTeal2, Icons.Default.Schedule)
        }
    }
}

// ── Motivation footer card ────────────────────────────────────────────────────

@Composable
private fun InsightMotivationCard() {
    val t = MaterialTheme.gg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandTeal2.copy(alpha = 0.10f))
            .border(1.dp, BrandTeal2.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandTeal2.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lightbulb, null, tint = BrandTeal2,
                modifier = Modifier.size(20.dp))
        }
        Column {
            Text("Consistency is Key", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = t.ink)
            Text(
                "Users who complete at least 3 corrective sessions per week report " +
                "a 40% reduction in spinal discomfort within 14 days.",
                fontSize = 12.sp, color = t.ink2, lineHeight = 18.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

// ── Anatomy accordion ─────────────────────────────────────────────────────────

@Composable
private fun BodyAreaAccordion(
    title: String,
    severity: MetricSeverity,
    summary: String,
    insight: String,
    metrics: List<Pair<String, String>> = emptyList()
) {
    val t = MaterialTheme.gg
    var expanded by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    val (dotColor, bgColor) = when (severity) {
        MetricSeverity.ALERT  -> BrandRed   to BrandRed.copy(alpha = 0.07f)
        MetricSeverity.WATCH  -> BrandAmber to BrandAmber.copy(alpha = 0.07f)
        MetricSeverity.NORMAL -> BrandTeal2 to BrandTeal2.copy(alpha = 0.07f)
    }
    val tone = when (severity) {
        MetricSeverity.ALERT  -> StatusTone.Bad
        MetricSeverity.WATCH  -> StatusTone.Warn
        MetricSeverity.NORMAL -> StatusTone.Good
    }

    GgCard(modifier = Modifier.fillMaxWidth()) {
        // Header row (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(dotColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = t.ink)
                Text(
                    summary, fontSize = 11.sp, color = dotColor,
                    modifier = Modifier.padding(top = 2.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusPill(
                severity.name.lowercase().replaceFirstChar { it.uppercase() },
                tone, dot = false
            )
            Icon(
                Icons.Default.ExpandMore, null, tint = t.ink3,
                modifier = Modifier.size(18.dp).rotate(chevronAngle)
            )
        }

        // Expanded content
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric tiles grid
                if (metrics.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        metrics.forEach { (label, value) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(t.surfaceElev)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    label, fontSize = 9.5.sp, color = t.inkMute,
                                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp
                                )
                                Text(
                                    value, fontFamily = LexendFamily, fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold, color = dotColor,
                                    letterSpacing = (-0.36).sp
                                )
                            }
                        }
                    }
                }
                // Insight text with left accent border
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                ) {
                    Box(
                        Modifier.width(3.dp).fillMaxHeight()
                            .background(dotColor)
                    )
                    Text(
                        insight, fontSize = 12.sp, color = t.ink2, lineHeight = 18.sp,
                        modifier = Modifier.padding(
                            start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp
                        )
                    )
                }
            }
        }
    }
}

// ── Spinal Health Score ring ──────────────────────────────────────────────────

@Composable
private fun SpinalHealthRing(score: Int, sizeDp: androidx.compose.ui.unit.Dp = 100.dp) {
    val appear = rememberAppearProgress(score, durationMs = 1100)
    val pct = (score.coerceIn(0, 100) / 100f) * appear
    val arcColor = when {
        score >= 70 -> BrandTeal
        score >= 50 -> BrandAmber
        else        -> BrandRed
    }
    Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(sizeDp)) {
            val stroke = 9.dp.toPx()
            val r = (size.minDimension - stroke) / 2f
            drawCircle(color = Color.White.copy(alpha = 0.18f), radius = r,
                style = Stroke(stroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(arcColor.copy(alpha = 0.70f), arcColor)),
                startAngle = -90f, sweepAngle = 360f * pct, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                score.toString(), fontFamily = LexendFamily, fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold, color = Color.White,
                letterSpacing = (-0.84).sp
            )
            Text("/ 100", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Photo record card ─────────────────────────────────────────────────────────

@Composable
private fun PhotoRecordCard(snap: PostureSnapshot, onDelete: () -> Unit) {
    val t = MaterialTheme.gg
    val score = snap.spinalHealthScore
    val scoreColor = when {
        score >= 70 -> BrandTeal2
        score >= 50 -> BrandAmber
        else        -> BrandRed
    }
    val dateStr = remember(snap.timestamp) {
        SimpleDateFormat("MMM d · HH:mm", Locale.US).format(Date(snap.timestamp))
    }
    val issues = buildList {
        if (snap.kyphosisAngleDeg > 50f) add("Forward lean ${snap.kyphosisAngleDeg.toInt()}°")
        else if (snap.kyphosisAngleDeg > 40f) add("Mild neck tilt ${snap.kyphosisAngleDeg.toInt()}°")
        if (abs(snap.shoulderLevelDeg) > 5f) add("Shoulder imbalance ${abs(snap.shoulderLevelDeg).toInt()}°")
        if (abs(snap.pelvicTiltDeg) > 4f) add("Pelvic tilt ${abs(snap.pelvicTiltDeg).toInt()}°")
        if (!snap.axialAligned) add("Lateral deviation")
        if (isEmpty()) add("No issues detected ✓")
    }

    GgCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Score ring
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.10f))
                    .border(2.dp, scoreColor.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (score > 0) {
                        Text(
                            score.toString(),
                            fontFamily = LexendFamily, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = scoreColor,
                            letterSpacing = (-0.28).sp
                        )
                        Text("/ 100", fontSize = 7.sp, color = t.ink3)
                    } else {
                        Icon(Icons.Default.PhotoCamera, null,
                            tint = scoreColor.copy(alpha = 0.80f), modifier = Modifier.size(22.dp))
                    }
                }
            }
            PosturePhotoThumbnail(
                photoUri = snap.sourceUri,
                modifier = Modifier.size(58.dp)
            )
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Gallery Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = t.ink)
                    if (snap.clinicalGrade.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(scoreColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(snap.clinicalGrade, fontSize = 9.sp, color = scoreColor,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(dateStr, fontSize = 10.sp, color = t.ink3,
                    modifier = Modifier.padding(top = 1.dp))
                Spacer(Modifier.height(5.dp))
                issues.take(2).forEach { issue ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Box(Modifier.size(4.dp).clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.65f)))
                        Text(issue, fontSize = 10.5.sp, color = t.ink2)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = BrandRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Level / duration chip ─────────────────────────────────────────────────────

@Composable
private fun LevelChip(label: String, color: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
        Text(label, style = LabelCapsStyle, color = color)
    }
}
