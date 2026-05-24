package com.gaitguard.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gaitguard.app.MainActivity
import com.gaitguard.app.R
import com.gaitguard.app.data.db.GaitDatabase
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Daily background worker that compares this week's gait metrics
 * to last week's. Fires a notification if a meaningful decline is detected.
 */
class TrendCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID   = "gaitguard_trend_alerts"
        const val WORK_TAG     = "daily_trend_check"
        private const val NOTIF_ID = 2001

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<TrendCheckWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).addTag(WORK_TAG).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    req
                )
        }
    }

    override suspend fun doWork(): Result {
        val db = GaitDatabase.getInstance(applicationContext)
        val allSessions = db.sessionDao().getAllSessionsSync()
        if (allSessions.size < 6) return Result.success()  // not enough data

        val nowMs   = System.currentTimeMillis()
        val weekMs  = 7 * 24 * 60 * 60 * 1000L

        val thisWeek = allSessions.filter { it.date >= nowMs - weekMs }
        val lastWeek = allSessions.filter { it.date in (nowMs - 2 * weekMs) until (nowMs - weekMs) }

        if (thisWeek.isEmpty() || lastWeek.isEmpty()) return Result.success()

        val thisAvgSym  = thisWeek.map { it.avgSymmetryScore }.average().toFloat()
        val lastAvgSym  = lastWeek.map { it.avgSymmetryScore }.average().toFloat()
        val thisAvgFat  = thisWeek.map { it.peakFatigueScore }.average().toFloat()
        val lastAvgFat  = lastWeek.map { it.peakFatigueScore }.average().toFloat()

        val symDrop   = lastAvgSym - thisAvgSym   // positive = getting worse
        val fatRise   = thisAvgFat - lastAvgFat   // positive = more fatigued

        val message = buildString {
            if (symDrop >= 5f)  append("Gait symmetry dropped ${symDrop.toInt()}% vs last week. ")
            if (fatRise >= 10f) append("Peak fatigue rose ${fatRise.toInt()} pts vs last week. ")
            if (isEmpty()) return Result.success()  // nothing notable
            append("Consider a rest day or physiotherapy consultation.")
        }

        sendNotification(message)
        return Result.success()
    }

    private fun sendNotification(message: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tapIntent = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚠ GaitGuard Trend Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID, notif)
    }
}
