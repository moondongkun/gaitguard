package com.gaitguard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.gaitguard.app.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    private val requiredPermissions: Array<String> get() = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.VIBRATE)
        // POST_NOTIFICATIONS requires runtime request on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        setContent {
            // Task #3 fix: track cameraGranted as Compose state so permission updates
            // re-compose the existing NavController instead of rebuilding the whole tree.
            var cameraGranted by remember { mutableStateOf(initialCamera) }

            val permLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                cameraGranted = results[Manifest.permission.CAMERA] == true
            }

            LaunchedEffect(Unit) {
                val missing = requiredPermissions.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
            }

            // GaitGuardTheme is now applied inside AppNavigation (with dark mode support)
            AppNavigation(hasCameraPermission = cameraGranted)
        }
    }
}
