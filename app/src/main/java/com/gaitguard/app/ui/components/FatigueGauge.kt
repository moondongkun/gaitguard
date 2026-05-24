package com.gaitguard.app.ui.components

import androidx.compose.ui.tooling.preview.Preview
import com.gaitguard.app.ui.theme.GaitGuardTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.data.model.AlertLevel
import com.gaitguard.app.ui.theme.GaugeAmber
import com.gaitguard.app.ui.theme.GaugeBackground
import com.gaitguard.app.ui.theme.GaugeRed
import com.gaitguard.app.ui.theme.GaugeSafe
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.OnSurface
import com.gaitguard.app.ui.theme.Outline
import com.gaitguard.app.ui.theme.Primary

@Composable
fun FatigueGauge(
    score: Float,
    alertLevel: AlertLevel = AlertLevel.SAFE,
    size: Dp = 192.dp,
    strokeWidth: Dp = 12.dp
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0f, 100f),
        animationSpec = tween(600),
        label = "gauge"
    )

    // Pulse animation when RED
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (alertLevel == AlertLevel.RED) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val gaugeColor = when {
        animatedScore >= 70f -> GaugeRed
        animatedScore >= 40f -> lerp(GaugeAmber, GaugeRed, (animatedScore - 40f) / 30f)
        else                 -> lerp(GaugeSafe,  GaugeAmber, animatedScore / 40f)
    }
    val finalColor = if (alertLevel == AlertLevel.RED) gaugeColor.copy(alpha = pulseAlpha) else gaugeColor

    val startAngle = 135f
    val sweepMax   = 270f
    val sweepAngle = sweepMax * (animatedScore / 100f)

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset  = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            // Background arc
            drawArc(
                color      = GaugeBackground,
                startAngle = startAngle,
                sweepAngle = sweepMax,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress arc
            if (sweepAngle > 0f) {
                drawArc(
                    color      = finalColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text     = "${animatedScore.toInt()}%",
                fontFamily = LexendFamily,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                color    = Primary
            )
            Text(
                text     = "FATIGUE",
                style    = LabelCapsStyle,
                color    = Outline
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Safe — 25%")
@Composable
private fun PreviewSafe() = GaitGuardTheme { FatigueGauge(score = 25f, alertLevel = AlertLevel.SAFE) }

@Preview(showBackground = true, name = "Amber — 55%")
@Composable
private fun PreviewAmber() = GaitGuardTheme { FatigueGauge(score = 55f, alertLevel = AlertLevel.AMBER) }

@Preview(showBackground = true, name = "Red — 80%")
@Composable
private fun PreviewRed() = GaitGuardTheme { FatigueGauge(score = 80f, alertLevel = AlertLevel.RED) }
