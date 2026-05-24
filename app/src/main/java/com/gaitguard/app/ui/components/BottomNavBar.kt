package com.gaitguard.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gaitguard.app.ui.theme.BrandNavy
import com.gaitguard.app.ui.theme.BrandNavy3
import com.gaitguard.app.ui.theme.LabelCapsStyle
import com.gaitguard.app.ui.theme.gg

/** Modernized floating glass nav bar. 5 tabs, animated active state. */

data class NavTab(val route: String, val label: String, val icon: ImageVector)

val navTabs = listOf(
    NavTab("metrics", "Metrics", Icons.Default.DashboardCustomize),
    NavTab("live",    "Live",    Icons.AutoMirrored.Filled.DirectionsRun),
    NavTab("posture", "Posture", Icons.Default.AccessibilityNew),
    NavTab("history", "History", Icons.Default.History),
    NavTab("engine",  "Engine",  Icons.Default.Memory),
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack    = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route
    val t = MaterialTheme.gg

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 22.dp,
                    shape     = RoundedCornerShape(26.dp),
                    clip      = false,
                    ambientColor = BrandNavy.copy(alpha = 0.18f),
                    spotColor    = BrandNavy.copy(alpha = 0.28f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(t.surfaceElev)
                .border(1.dp, t.border, RoundedCornerShape(26.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navTabs.forEach { tab ->
                NavItem(
                    tab      = tab,
                    selected = currentRoute == tab.route,
                    onClick  = {
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                launchSingleTop = true
                                popUpTo("metrics") {
                                    inclusive = false
                                    saveState = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = MaterialTheme.gg
    val activeBrush = Brush.linearGradient(
        listOf(BrandNavy3.copy(alpha = 0.14f), BrandNavy3.copy(alpha = 0.04f))
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) activeBrush else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector       = tab.icon,
            contentDescription = tab.label,
            tint              = if (selected) MaterialTheme.colorScheme.primary else t.inkMute,
            modifier          = Modifier.size(20.dp)
        )
        Text(
            text          = tab.label.uppercase(),
            fontSize      = 9.5.sp,
            fontWeight    = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color         = if (selected) MaterialTheme.colorScheme.primary else t.inkMute,
            letterSpacing = 0.8.sp
        )
    }
}
