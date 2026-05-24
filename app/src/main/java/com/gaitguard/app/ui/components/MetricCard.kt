package com.gaitguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.ui.theme.BodySmStyle
import com.gaitguard.app.ui.theme.GaitGuardTheme
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.OnSurface
import com.gaitguard.app.ui.theme.Outline
import com.gaitguard.app.ui.theme.Primary
import com.gaitguard.app.ui.theme.Surface

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color = Color.White,
    contentColor: Color = OnSurface,
    titleColor: Color = Outline,
    accentColor: Color = Primary,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Accent line at top
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .fillMaxWidth(0.3f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.4f))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text  = title.uppercase(),
                style = LabelCapsStyle,
                color = titleColor
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = value,
                fontFamily = LexendFamily,
                fontSize   = 30.sp,
                color      = accentColor
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = subtitle,
                    style = BodySmStyle,
                    color = titleColor
                )
            }
            if (content != null) {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "MetricCard — Cadence")
@Composable
private fun PreviewMetricCard() = GaitGuardTheme {
    MetricCard(title = "Cadence", value = "112 spm", subtitle = "↑ 3 from last session")
}

@Preview(showBackground = true, name = "MetricCard — With content")
@Composable
private fun PreviewMetricCardContent() = GaitGuardTheme {
    MetricCard(title = "Symmetry", value = "94%") {
        androidx.compose.material3.LinearProgressIndicator(progress = { 0.94f })
    }
}
