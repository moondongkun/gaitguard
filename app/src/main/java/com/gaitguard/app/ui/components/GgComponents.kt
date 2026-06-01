package com.gaitguard.app.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.R
import com.gaitguard.app.ui.theme.BrandAmber
import com.gaitguard.app.ui.theme.BrandNavy
import com.gaitguard.app.ui.theme.BrandNavy2
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.BrandRed
import com.gaitguard.app.ui.theme.BrandTeal
import com.gaitguard.app.ui.theme.BrandTeal2
import com.gaitguard.app.ui.theme.BrandViolet
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.gg

// ════════════════════════════════════════════════════════════════
//  GaitGuard — modernized shared composables
//  Drop-in primitives that match the App Mockups deck.
// ════════════════════════════════════════════════════════════════

/* ───────────────────────  Spine mark + wordmark  ─────────────── */

/** Vertical spine glyph — 5 stacked vertebrae shapes. Used in the brand lockup
 *  and inside the launcher icon. Tint with [tint]. */
@Composable
fun SpineMark(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f
        val ys     = floatArrayOf(0.16f, 0.32f, 0.50f, 0.68f, 0.84f)
        val widths = floatArrayOf(0.55f, 0.62f, 0.66f, 0.58f, 0.52f)
        val alphas = floatArrayOf(0.95f, 0.85f, 0.78f, 0.65f, 0.55f)
        for (i in ys.indices) {
            drawOval(
                color = tint.copy(alpha = alphas[i]),
                topLeft = Offset(cx - w * widths[i] / 2f, ys[i] * h - h * 0.05f),
                size = androidx.compose.ui.geometry.Size(w * widths[i], h * 0.10f)
            )
        }
    }
}

/** Brand lockup: gradient-tile spine mark + "GaitGuard" wordmark (teal accent
 *  on the "Guard" half). Drops on any background — pass [onDark] when atop
 *  the hero gradient card or the camera overlay. */
@Composable
fun GaitGuardLockup(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val nameColor      = if (onDark) Color.White else MaterialTheme.gg.ink
    val accentColor    = if (onDark) BrandTeal else BrandNavy3
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.gg_logo),
            contentDescription = "GaitGuard logo",
            modifier = Modifier
                .size(32.dp)
                .shadow(6.dp, RoundedCornerShape(9.dp), clip = false)
                .clip(RoundedCornerShape(9.dp))
        )
        Row {
            Text("Gait",  color = nameColor,   fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Guard", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/* ───────────────────────  Ambient backdrop  ──────────────────── */

/** Soft radial-gradient blobs in the screen background. Place behind everything
 *  with `Box(Modifier.fillMaxSize()) { AmbientBackdrop(); content }`. Adapts to
 *  light + dark via [MaterialTheme.gg]. */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val t = MaterialTheme.gg
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(t.ambientA, Color.Transparent),
                    center = Offset(w * 0.95f, -h * 0.05f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.95f, -h * 0.05f)
            )
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(t.ambientB, Color.Transparent),
                    center = Offset(-w * 0.1f, h * 0.45f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(-w * 0.1f, h * 0.45f)
            )
        }
    }
}

/* ───────────────────────  Hero gradient card  ────────────────── */

/** The signature navy → teal-tinted hero card seen on Metrics, Live, Engine,
 *  Settings profile, Walk Test, Replay summary. */
@Composable
fun HeroGradientCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(20.dp, RoundedCornerShape(22.dp), clip = false,
                ambientColor = BrandNavy.copy(alpha = 0.40f),
                spotColor    = BrandNavy.copy(alpha = 0.40f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(BrandNavy3, BrandNavy2, BrandNavy)))
    ) {
        // Top-right teal glow
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(BrandTeal.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 1.0f, 0f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 1.0f, 0f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 1.1f),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 1.1f)
            )
        }
        Box(Modifier.padding(padding), content = content)
    }
}

/* ───────────────────────  Plain & glass cards  ───────────────── */

@Composable
fun GgCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val t = MaterialTheme.gg
    Column(
        modifier = modifier
            .then(if (elevated) Modifier.shadow(10.dp, RoundedCornerShape(16.dp), clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor    = Color.Black.copy(alpha = 0.08f)) else Modifier)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, t.border, RoundedCornerShape(16.dp))
            .padding(padding),
        content = content
    )
}

/** Translucent/glass card — sits over a busy backdrop (camera, hero). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    padding: PaddingValues = PaddingValues(14.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val bg = if (onDark) Color(0x9E07090E) else MaterialTheme.gg.surfaceElev
    val border = if (onDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.gg.border
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(padding),
        content = content
    )
}

/* ───────────────────────  Status pill  ───────────────────────── */

enum class StatusTone { Good, Warn, Bad, Info, Neutral }

@Composable
fun StatusPill(
    text: String,
    tone: StatusTone = StatusTone.Good,
    modifier: Modifier = Modifier,
    dot: Boolean = true,
) {
    val t = MaterialTheme.gg
    val (fg, bg) = when (tone) {
        StatusTone.Good    -> BrandTeal2  to BrandTeal2.copy(alpha = 0.12f)
        StatusTone.Warn    -> BrandAmber  to BrandAmber.copy(alpha = 0.14f)
        StatusTone.Bad     -> BrandRed    to BrandRed.copy(alpha = 0.14f)
        StatusTone.Info    -> BrandNavy2  to BrandNavy2.copy(alpha = 0.10f)
        StatusTone.Neutral -> t.ink3      to t.ink3.copy(alpha = 0.10f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
        }
        Text(text.uppercase(), style = LabelCapsStyle, color = fg)
    }
}

/* ───────────────────────  Metric tile  ───────────────────────── */

@Composable
fun MetricTile(
    label: String,
    value: String,
    unit: String = "",
    subtitle: String = "",
    accent: Color = BrandNavy3,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val t = MaterialTheme.gg
    GgCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon?.invoke()
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(10.dp))
        Text(
            label.uppercase(),
            style = LabelCapsStyle,
            color = t.inkMute
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp, lineHeight = 26.sp),
                color = t.ink
            )
            if (unit.isNotEmpty()) {
                Text(unit, style = TextStyle(fontSize = 11.sp, color = t.ink3, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = TextStyle(fontSize = 11.sp, color = t.ink3))
        }
    }
}

/* ───────────────────────  Insight row  ───────────────────────── */

@Composable
fun InsightRow(
    text: String,
    tone: StatusTone,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = MaterialTheme.gg
    val color = when (tone) {
        StatusTone.Good    -> BrandTeal2
        StatusTone.Warn    -> BrandAmber
        StatusTone.Bad     -> BrandRed
        StatusTone.Info    -> BrandNavy2
        StatusTone.Neutral -> t.ink3
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Color.White
            ) { icon() }
        }
        Text(text, style = TextStyle(fontSize = 12.sp, color = t.ink2, lineHeight = 17.sp),
            overflow = TextOverflow.Ellipsis)
    }
}

/* ───────────────────────  Helpers  ───────────────────────────── */

/** Pulsing dot — for "live" / "recording" indicators. Returns the current
 *  alpha you can apply yourself, OR use the convenience composable below. */
@Composable
fun pulsingAlpha(periodMs: Int = 1400): Float {
    val transition = rememberInfiniteTransition(label = "p")
    val a by transition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(periodMs, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "pa"
    )
    return a
}

/* ───────────────────────  Load-in animation helper  ──────────── */

/**
 * Returns a 0→1 progress that eases in once, on first composition (and replays
 * whenever [key] changes). Drive ring sweeps, bar growth, etc. with this so the
 * chart "fills" on screen open — matches the reference design's load animations.
 */
@Composable
fun rememberAppearProgress(key: Any? = Unit, durationMs: Int = 1000): Float {
    var play by remember(key) { androidx.compose.runtime.mutableStateOf(false) }
    val p by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (play) 1f else 0f,
        animationSpec = tween(durationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "appear"
    )
    LaunchedEffect(key) { play = true }
    return p
}

/* ───────────────────────  Sparkline  ─────────────────────────── */

/** Minimal polyline sparkline (no axes). Used in compact trend chips. */
@Composable
fun Sparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = BrandTeal,
    strokeWidth: Float = 5f,
) {
    if (data.size < 2) return
    val appear = rememberAppearProgress(data, durationMs = 900)
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val min = data.minOrNull() ?: 0f; val max = data.maxOrNull() ?: 1f
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val n = data.size
        val pts = data.mapIndexed { i, v ->
            val x = (i / (n - 1f)) * w
            val y = h - ((v - min) / span) * (h - strokeWidth) - strokeWidth / 2f
            Offset(x, y)
        }
        // gradient stroke, revealed left→right by appear progress
        val cutoff = (appear * (n - 1)).coerceIn(0f, (n - 1).toFloat())
        for (i in 0 until n - 1) {
            if (i > cutoff) break
            drawLine(
                brush = Brush.linearGradient(listOf(color, BrandTeal2)),
                start = pts[i], end = pts[i + 1],
                strokeWidth = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
