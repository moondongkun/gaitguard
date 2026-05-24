package com.gaitguard.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.gaitguard.app.ui.components.GgCard
import com.gaitguard.app.ui.components.HeroGradientCard
import com.gaitguard.app.ui.theme.*
import com.gaitguard.app.ui.viewmodel.GaitViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    gaitViewModel: GaitViewModel,
    onBack: () -> Unit,
    onNavigateToWalkTest: () -> Unit,
    onDarkModeChanged: (Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val t   = MaterialTheme.gg
    var darkMode by remember { mutableStateOf(isDarkModeEnabled(ctx)) }
    var fatigueAlerts by remember { mutableStateOf(true) }
    var fallSms by remember { mutableStateOf(true) }
    var reminder by remember { mutableStateOf(false) }
    var deviceTheme by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all data?") },
            text = { Text("This removes all gait sessions, posture photos, posture assessments, and saved timelines from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        gaitViewModel.clearAllData()
                        Toast.makeText(ctx, "All local GaitGuard data cleared", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Clear", color = BrandRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }

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
                    Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                        color = t.ink, letterSpacing = (-0.18).sp)
                }
            }

            // Profile hero
            item {
                HeroGradientCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(BrandTeal, BrandTeal2))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("D", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = Color(0xFF003322))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Dong Kun Moon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("2020-13236 · Solo Project",
                                    color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ProfileStat("24",  "Sessions")
                            ProfileStat("18d", "Monitoring")
                            ProfileStat("92%", "Avg Sym")
                        }
                    }
                }
            }

            // Appearance
            item {
                SectionHeader("Appearance")
                GgCard(padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    SettingRow("Dark mode", trailing = { GgToggle(darkMode) {
                        darkMode = it; setDarkModeEnabled(ctx, it); onDarkModeChanged(it)
                    } })
                    SettingDivider()
                    SettingRow("Use device theme", trailing = { GgToggle(deviceTheme) { deviceTheme = it } })
                }
            }

            // Alerts
            item {
                SectionHeader("Alerts")
                GgCard(padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    SettingRow("Fatigue alerts",
                        sub = "Vibrate when score enters amber zone",
                        trailing = { GgToggle(fatigueAlerts) { fatigueAlerts = it } })
                    SettingDivider()
                    SettingRow("Fall detection SMS",
                        sub = "To caregiver: +1 (415) 555-0142",
                        trailing = { GgToggle(fallSms) { fallSms = it } })
                    SettingDivider()
                    SettingRow("Daily reminder",
                        sub = "Reminds you to walk every morning",
                        trailing = { GgToggle(reminder) { reminder = it } })
                }
            }

            // Data
            item {
                SectionHeader("Data & Privacy")
                GgCard(padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    SettingRow(
                        "Export health PDF",
                        sub = "Summarised status report for users",
                        trailing = { ChevronChrome() },
                        onClick = {
                            scope.launch {
                                val uri = gaitViewModel.exportHealthReport()
                                Toast.makeText(
                                    ctx,
                                    if (uri != null) "Health PDF saved to Downloads" else "Could not create PDF report",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    SettingDivider()
                    SettingRow("Clear all data", labelColor = BrandRed,
                        trailing = { ChevronChrome(BrandRed) },
                        onClick = { confirmClear = true })
                }
            }

            // About
            item {
                SectionHeader("About")
                GgCard(padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    SettingRow("Version", trailing = {
                        Text("1.0.0 · Galaxy S22", fontSize = 12.sp, color = t.ink3,
                            fontWeight = FontWeight.SemiBold)
                    })
                    SettingDivider()
                    SettingRow("Run Walk Test", trailing = { ChevronChrome() },
                        onClick = onNavigateToWalkTest)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column {
        Text(value, fontFamily = LexendFamily, fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = (-0.36).sp)
        Text(label.uppercase(), style = LabelCapsStyle, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun SectionHeader(title: String) {
    val t = MaterialTheme.gg
    Text(title.uppercase(), style = LabelCapsStyle, color = t.inkMute,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
}

@Composable
private fun SettingRow(
    label: String, sub: String = "", labelColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val t = MaterialTheme.gg
    val mod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = mod.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                color = if (labelColor == Color.Unspecified) t.ink else labelColor)
            if (sub.isNotEmpty()) {
                Text(sub, fontSize = 11.sp, color = t.ink3, modifier = Modifier.padding(top = 2.dp))
            }
        }
        trailing()
    }
}
@Composable
private fun SettingDivider() {
    val t = MaterialTheme.gg
    Box(Modifier.fillMaxWidth().height(1.dp).background(t.border))
}

@Composable
private fun GgToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val thumbX by animateDpAsState(if (on) 18.dp else 2.dp, label = "thumb")
    Box(
        modifier = Modifier
            .size(42.dp, 24.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (on) Brush.linearGradient(listOf(BrandNavy3, BrandNavy2))
                else androidx.compose.ui.graphics.SolidColor(MaterialTheme.gg.border)
            )
            .clickable { onChange(!on) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX, y = 2.dp)
                .size(20.dp).clip(CircleShape).background(Color.White)
        )
    }
}

@Composable
private fun ChevronChrome(tint: Color = MaterialTheme.gg.ink3) {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = tint, modifier = Modifier.size(18.dp))
}
