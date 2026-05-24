package com.gaitguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.data.db.SessionRecord
import com.gaitguard.app.ui.theme.BodySmStyle
import com.gaitguard.app.ui.theme.Error
import com.gaitguard.app.ui.theme.GaugeSafe
import com.gaitguard.app.ui.theme.GaugeAmber
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.LexendFamily
import com.gaitguard.app.ui.theme.OnSurface
import com.gaitguard.app.ui.theme.Outline
import com.gaitguard.app.ui.theme.Primary
import com.gaitguard.app.ui.theme.Secondary
import com.gaitguard.app.ui.theme.SurfaceContainerLow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Task #13: derive a friendly session name from the hour-of-day at session start. */
private fun sessionLabel(timestampMs: Long): String {
    val hour = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
        .get(java.util.Calendar.HOUR_OF_DAY)
    val timeOfDay = when {
        hour in 5..11  -> "Morning"
        hour in 12..16 -> "Afternoon"
        hour in 17..20 -> "Evening"
        else           -> "Night"
    }
    val dateStr = SimpleDateFormat("MMM d", Locale.US).format(Date(timestampMs))
    return "$timeOfDay Walk · $dateStr"
}

@Composable
fun SessionCard(session: SessionRecord, modifier: Modifier = Modifier) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(session.date))
    val fatigue = session.peakFatigueScore
    val fatigueColor = when {
        fatigue >= 70f -> Error
        fatigue >= 40f -> GaugeAmber
        else           -> GaugeSafe
    }
    // Gradient from fatigueColor (lighter) to solid
    val fatigueGradient = Brush.horizontalGradient(
        listOf(fatigueColor.copy(alpha = 0.5f), fatigueColor)
    )

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle with gradient tint
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint     = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = sessionLabel(session.date),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = OnSurface
                )
                Text(
                    text  = dateStr,
                    style = BodySmStyle,
                    color = Outline
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(label = "${session.durationMinutes}m", tint = Primary.copy(alpha = 0.7f))
                    StatChip(label = "Sym ${session.avgSymmetryScore.toInt()}%", tint = Secondary.copy(alpha = 0.7f))
                    if (session.fallRiskEventCount > 0)
                        StatChip(label = "${session.fallRiskEventCount} falls", tint = Error)
                }
                Spacer(Modifier.height(8.dp))
                // Fatigue bar with gradient fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFECEEF1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fatigue / 100f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(fatigueGradient)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "${fatigue.toInt()}",
                    fontFamily = LexendFamily,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = fatigueColor
                )
                Text(
                    text  = "fatigue",
                    style = BodySmStyle,
                    color = Outline
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, style = BodySmStyle, color = tint)
    }
}
