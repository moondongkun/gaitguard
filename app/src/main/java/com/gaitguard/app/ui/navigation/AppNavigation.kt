package com.gaitguard.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gaitguard.app.ui.components.BottomNavBar
import com.gaitguard.app.ui.screens.CalibrationScreen
import com.gaitguard.app.ui.screens.EngineScreen
import com.gaitguard.app.ui.screens.HistoryScreen
import com.gaitguard.app.ui.screens.LiveScreen
import com.gaitguard.app.ui.screens.MetricsScreen
import com.gaitguard.app.ui.screens.PostureCaptureResultScreen
import com.gaitguard.app.ui.screens.PostureHealthScreen
import com.gaitguard.app.ui.screens.PostureScreen
import com.gaitguard.app.ui.screens.SessionReplayScreen
import com.gaitguard.app.ui.screens.SessionReplayViewModel
import com.gaitguard.app.ui.screens.SettingsScreen
import com.gaitguard.app.ui.screens.WalkTestScreen
import com.gaitguard.app.ui.theme.GaitGuardTheme
import com.gaitguard.app.ui.theme.isDarkModeEnabled
import com.gaitguard.app.ui.viewmodel.GaitViewModel
import com.gaitguard.app.ui.viewmodel.HistoryViewModel
import com.gaitguard.app.ui.viewmodel.PostureViewModel

private val noNavBarRoutes = setOf("calibration", "settings", "walktest", "replay", "posture", "posture_health", "posture_result")

@Composable
fun AppNavigation(hasCameraPermission: Boolean) {
    val context       = LocalContext.current
    var darkMode      by rememberSaveable { mutableStateOf(isDarkModeEnabled(context)) }

    GaitGuardTheme(darkMode = darkMode) {
        val navController = rememberNavController()
        val gaitVm:    GaitViewModel         = viewModel()
        val postureVm: PostureViewModel      = viewModel()
        val historyVm: HistoryViewModel      = viewModel()
        val replayVm:  SessionReplayViewModel = viewModel()

        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val showNavBar = currentRoute !in noNavBarRoutes &&
            !currentRoute.orEmpty().startsWith("replay")

        // First-launch calibration guard
        LaunchedEffect(Unit) {
            if (!gaitVm.isCalibrated()) {
                navController.navigate("calibration") {
                    popUpTo("metrics") { inclusive = false }
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (showNavBar) BottomNavBar(navController)
            }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = "metrics",
                modifier         = Modifier.padding(innerPadding)
            ) {
                composable("metrics") {
                    MetricsScreen(
                        viewModel            = gaitVm,
                        onNavigateToLive     = { navController.navigate("live") },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("live") {
                    LiveScreen(
                        viewModel = gaitVm,
                        onNavigateToPosture = { navController.navigate("posture") }
                    )
                }
                composable("posture") {
                    PostureScreen(
                        viewModel           = postureVm,
                        hasCameraPermission = hasCameraPermission,
                        onNavigateBackToLive = {
                            navController.navigate("live") {
                                popUpTo("live") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToHealth  = { navController.navigate("posture_health") },
                        onNavigateToResult  = { navController.navigate("posture_result") }
                    )
                }
                composable("posture_result") {
                    PostureCaptureResultScreen(
                        viewModel    = postureVm,
                        onBack       = { navController.popBackStack() },
                        onViewReport = { navController.navigate("posture_health") }
                    )
                }
                composable("posture_health") {
                    PostureHealthScreen(
                        viewModel = postureVm,
                        onBack    = { navController.popBackStack() }
                    )
                }
                composable("history") {
                    HistoryScreen(
                        viewModel      = historyVm,
                        onSessionClick = { sessionId ->
                            navController.navigate("replay/$sessionId")
                        }
                    )
                }
                composable("engine") {
                    EngineScreen(gaitViewModel = gaitVm)
                }
                composable("calibration") {
                    CalibrationScreen(
                        viewModel  = gaitVm,
                        onComplete = {
                            navController.navigate("metrics") {
                                popUpTo("calibration") { inclusive = true }
                            }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        gaitViewModel        = gaitVm,
                        onBack               = { navController.popBackStack() },
                        onNavigateToWalkTest = { navController.navigate("walktest") },
                        onDarkModeChanged    = { enabled -> darkMode = enabled }
                    )
                }
                composable("walktest") {
                    WalkTestScreen(
                        viewModel = gaitVm,
                        onBack    = { navController.popBackStack() }
                    )
                }
                composable(
                    route     = "replay/{sessionId}",
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                    SessionReplayScreen(
                        sessionId = sessionId,
                        viewModel = replayVm,
                        onBack    = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
