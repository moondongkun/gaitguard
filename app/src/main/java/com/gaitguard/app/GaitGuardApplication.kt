package com.gaitguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.gaitguard.app.data.db.GaitDatabase
import com.gaitguard.app.util.DemoDataSeeder
import com.gaitguard.app.util.TrendCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GaitGuardApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Warm up Room database singleton on startup
        GaitDatabase.getInstance(this)
        appScope.launch {
            DemoDataSeeder.seedIfEmpty(this@GaitGuardApplication)
        }

        // Create notification channel for trend alerts
        createNotificationChannels()

        // Schedule daily trend check worker
        TrendCheckWorker.schedule(this)

        Log.i("GaitGuard", "Application initialized")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Trend alerts channel
            nm.createNotificationChannel(
                NotificationChannel(
                    TrendCheckWorker.CHANNEL_ID,
                    "Gait Trend Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Weekly gait health trend notifications"
                }
            )

            // Fall alerts channel (already used by SmsHelper vibration path)
            nm.createNotificationChannel(
                NotificationChannel(
                    "fall_alerts",
                    "Fall Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Immediate fall detection notifications"
                }
            )
        }
    }
}
