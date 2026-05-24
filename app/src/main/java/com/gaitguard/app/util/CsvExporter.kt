package com.gaitguard.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.gaitguard.app.data.db.SessionRecord
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "CsvExporter"

object CsvExporter {

    fun exportSessions(context: Context, sessions: List<SessionRecord>): Uri? {
        val filename = "GaitGuard_sessions_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        }.csv"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                out.write(csvHeader().toByteArray())
                sessions.forEach { session ->
                    out.write(sessionToRow(session).toByteArray())
                }
            }
            Log.i(TAG, "Exported ${sessions.size} sessions to $filename")
            uri
        } catch (e: IOException) {
            Log.e(TAG, "Export failed", e)
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun csvHeader() =
        "id,date,duration_min,peak_fatigue,onset_min,avg_symmetry,fall_events,stumbles,endurance\n"

    private fun sessionToRow(s: SessionRecord): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(s.date))
        return "${s.id},\"$date\",${s.durationMinutes},${s.peakFatigueScore}," +
               "${s.fatigueOnsetMinute},${s.avgSymmetryScore},${s.fallRiskEventCount}," +
               "${s.microStumbleCount},${s.enduranceScore}\n"
    }
}
