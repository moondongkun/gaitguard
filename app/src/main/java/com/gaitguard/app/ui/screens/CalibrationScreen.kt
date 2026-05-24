package com.gaitguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitguard.app.ui.components.AmbientBackdrop
import com.gaitguard.app.ui.components.GaitGuardLockup
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.components.SpineMark
import com.gaitguard.app.ui.components.StatusPill
import com.gaitguard.app.ui.components.StatusTone
import com.gaitguard.app.ui.theme.*
import com.gaitguard.app.ui.viewmodel.GaitViewModel
import kotlinx.coroutines.delay

/**
 * Calibration is a short waiting screen: 5-second hold while the IMU records
 * a baseline. We simply show progress and call onComplete when the count
 * finishes. (If your repository exposes explicit begin/complete hooks, wire
 * them in the LaunchedEffect below.)
 */
@Composable
fun CalibrationScreen(viewModel: GaitViewModel, onComplete: () -> Unit) {
    val t = MaterialTheme.gg
    var elapsed by remember { mutableIntStateOf(0) }
    val total = 5

    LaunchedEffect(Unit) {
        repeat(total) { delay(1000); elapsed++ }
        // If your VM has begin/completeCalibration(), call them here.
        onComplete()
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop()
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) { GaitGuardLockup() }

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                StatusPill("Step 2 of 3", StatusTone.Good)
                Spacer(Modifier.height(12.dp))
                Text("Hold still for $total seconds",
                    style = HeadlineLgStyle, color = t.ink)
                Spacer(Modifier.height(8.dp))
                Text("Stand upright with the phone in your trouser pocket so we can calibrate the gravity vector.",
                    fontSize = 13.sp, color = t.ink3, lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 24.dp))
            }

            HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(96.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center) {
                        SpineMark(modifier = Modifier.size(48.dp), tint = Color.White)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("${(total - elapsed).coerceAtLeast(0)}s",
                        fontFamily = LexendFamily, fontSize = 44.sp,
                        fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-1.3).sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress  = { elapsed.toFloat() / total },
                        modifier  = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color     = BrandTeal,
                        trackColor = Color.White.copy(alpha = 0.18f)
                    )
                }
            }

            GgCard {
                Text("SETUP CHECKLIST", style = LabelCapsStyle, color = t.inkMute)
                Spacer(Modifier.height(6.dp))
                CheckItem("Phone in pocket",  done = true)
                CheckItem("Stand upright",    done = true)
                CheckItem("Hold still",       done = elapsed >= total)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CheckItem(label: String, done: Boolean) {
    val t = MaterialTheme.gg
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
            .background(if (done) BrandTeal2 else t.border),
            contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
            else Box(Modifier.size(8.dp).clip(CircleShape).background(t.inkMute))
        }
        Text(label, fontSize = 13.sp, color = t.ink, fontWeight = FontWeight.SemiBold)
    }
}
