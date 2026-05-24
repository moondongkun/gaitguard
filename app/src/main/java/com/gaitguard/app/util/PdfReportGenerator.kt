package com.gaitguard.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gaitguard.app.data.db.SessionRecord
import com.gaitguard.app.data.model.GaitResult
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a multi-page PDF Gait Health Summary from a list of sessions.
 * Uses Android's built-in PdfDocument — no extra library needed.
 * Saves to Downloads via MediaStore (API 29+).
 */
object PdfReportGenerator {

    private const val PAGE_W = 595  // A4 width pts
    private const val PAGE_H = 842  // A4 height pts
    private const val MARGIN = 48f

    fun generate(context: Context, sessions: List<SessionRecord>, current: GaitResult? = null): Uri? {
        if (sessions.isEmpty() && current == null) return null

        val document = PdfDocument()
        try {
            // ── Page 1: Cover + Summary ──────────────────────────────────────
            val p1 = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
            drawCoverPage(p1.canvas, sessions, current)
            document.finishPage(p1)

            // ── Page 2: Session Table ────────────────────────────────────────
            val p2 = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 2).create())
            drawSessionTable(p2.canvas, sessions)
            document.finishPage(p2)

            // ── Page 3: Trend Analysis ───────────────────────────────────────
            val p3 = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 3).create())
            drawTrendPage(p3.canvas, sessions, current)
            document.finishPage(p3)

            return saveToDownloads(context, document)
        } finally {
            document.close()
        }
    }

    // ── Drawing helpers ────────────────────────────────────────────────────────

    private val paintTitle = Paint().apply {
        color = Color.parseColor("#003178"); textSize = 28f; isFakeBoldText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val paintH2 = Paint().apply {
        color = Color.parseColor("#003178"); textSize = 18f; isFakeBoldText = true
    }
    private val paintBody = Paint().apply {
        color = Color.parseColor("#191C1E"); textSize = 12f
    }
    private val paintSub = Paint().apply {
        color = Color.parseColor("#737783"); textSize = 11f
    }
    private val paintLine = Paint().apply {
        color = Color.parseColor("#E6E8EB"); strokeWidth = 1f; style = Paint.Style.STROKE
    }
    private val paintAccent = Paint().apply {
        color = Color.parseColor("#44DDC1"); style = Paint.Style.FILL
    }
    private val paintPrimary = Paint().apply {
        color = Color.parseColor("#003178"); style = Paint.Style.FILL
    }

    private fun drawCoverPage(canvas: Canvas, sessions: List<SessionRecord>, current: GaitResult?) {
        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())

        // Header band
        paintPrimary.alpha = 255
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 160f, paintPrimary)
        val wPaint = Paint(paintTitle).also { it.color = Color.WHITE; it.textSize = 30f }
        canvas.drawText("GaitGuard Health Report", MARGIN, 80f, wPaint)
        val wSub = Paint(paintSub).also { it.color = Color.WHITE; it.alpha = 180 }
        canvas.drawText("Generated $dateStr", MARGIN, 105f, wSub)
        canvas.drawText("${sessions.size} sessions analysed", MARGIN, 125f, wSub)

        // Accent strip
        paintAccent.alpha = 255
        canvas.drawRect(0f, 160f, PAGE_W.toFloat(), 165f, paintAccent)

        var y = 200f

        // Summary stats
        val avgFatigue  = sessions.map { it.peakFatigueScore }.ifEmpty { listOf(current?.fatigueScore ?: 0f) }.average()
        val avgSymmetry = sessions.map { it.avgSymmetryScore }.ifEmpty { listOf(current?.symmetryScore ?: 0f) }.average()
        val totalMin    = sessions.sumOf { it.durationMinutes }
        val fallEvents  = sessions.sumOf { it.fallRiskEventCount }
        val bestEnd     = sessions.maxOfOrNull { it.enduranceScore } ?: currentGlobalScore(current)

        canvas.drawText("SUMMARY STATISTICS", MARGIN, y, paintH2); y += 32f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, paintLine); y += 20f

        val stats = buildList {
            add(
            "Total Sessions" to "${sessions.size}",
            )
            add(
            "Total Walk Time" to "$totalMin minutes",
            )
            add(
            "Avg Peak Fatigue" to String.format("%.1f / 100", avgFatigue),
            )
            add(
            "Avg Gait Symmetry" to String.format("%.1f%%", avgSymmetry),
            )
            add(
            "Fall Risk Events" to "$fallEvents",
            )
            add(
                "Best Endurance Score" to String.format("%.1f%%", bestEnd)
            )
            current?.let { add("Current Global Score" to "${currentGlobalScore(it).toInt()} / 100") }
        }
        stats.forEach { (k, v) ->
            canvas.drawText(k, MARGIN, y, paintSub)
            canvas.drawText(v, PAGE_W - MARGIN - paintBody.measureText(v), y, paintBody)
            canvas.drawLine(MARGIN, y + 6f, PAGE_W - MARGIN, y + 6f, paintLine)
            y += 28f
        }

        y += 20f
        // Clinical notes section
        canvas.drawText("CLINICAL NOTES", MARGIN, y, paintH2); y += 32f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, paintLine); y += 20f

        val notes = buildList {
            if (avgFatigue > 60) add("⚠ Average fatigue exceeds 60 — sustained high-intensity walking may indicate fatigue accumulation.")
            if (avgSymmetry < 90) add("⚠ Average gait symmetry below 90% — possible compensatory gait pattern; physiotherapy evaluation recommended.")
            if (fallEvents > 2) add("⚠ Multiple fall-risk events recorded — consider fall prevention programme.")
            if (bestEnd > 80) add("✓ Excellent endurance score — gait quality is well-maintained over time.")
            if (isEmpty()) add("✓ All key metrics are within expected ranges. Continue regular monitoring.")
        }

        notes.forEach { note ->
            wrapText(canvas, note, MARGIN, y, PAGE_W - 2 * MARGIN, paintBody); y += 40f
        }

        // Footer
        current?.let {
            y += 12f
            canvas.drawText("CURRENT STATUS", MARGIN, y, paintH2); y += 28f
            canvas.drawText("Global gait score: ${currentGlobalScore(it).toInt()} / 100", MARGIN, y, paintBody); y += 20f
            canvas.drawText("Cadence: ${it.cadenceSpm.toInt()} spm, symmetry: ${it.symmetryScore.toInt()}%, fatigue: ${it.fatigueScore.toInt()} / 100", MARGIN, y, paintBody)
        }

        val footerY = PAGE_H - 40f
        canvas.drawLine(MARGIN, footerY - 12f, PAGE_W - MARGIN, footerY - 12f, paintLine)
        canvas.drawText("GaitGuard — Real-time Gait & Posture Monitor | Page 1 of 3", MARGIN, footerY, paintSub)
    }

    private fun drawSessionTable(canvas: Canvas, sessions: List<SessionRecord>) {
        val fmt = SimpleDateFormat("MMM d", Locale.US)
        var y = MARGIN + 30f

        // Header
        paintPrimary.alpha = 255
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 60f, paintPrimary)
        val wPaint = Paint(paintH2).also { it.color = Color.WHITE }
        canvas.drawText("Session Log", MARGIN, 40f, wPaint)

        y = 80f
        // Table header
        val cols = listOf("Date", "Duration", "Peak Fatigue", "Symmetry", "Endurance", "Falls")
        val colX = listOf(48f, 140f, 230f, 320f, 410f, 500f)

        val headerPaint = Paint(paintSub).also { it.isFakeBoldText = true; it.color = Color.parseColor("#003178") }
        cols.forEachIndexed { i, h -> canvas.drawText(h, colX[i], y, headerPaint) }
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, paintLine)
        y += 18f

        // Table rows — at most 20 on this page
        sessions.take(20).forEachIndexed { idx, s ->
            val rowPaint = if (idx % 2 == 0) paintBody else Paint(paintBody).also { it.color = Color.parseColor("#191C1E") }
            val bgPaint  = Paint().also { it.style = Paint.Style.FILL }
            if (idx % 2 == 1) {
                bgPaint.color = Color.parseColor("#F7F9FC")
                canvas.drawRect(MARGIN - 4f, y - 14f, PAGE_W - MARGIN + 4f, y + 6f, bgPaint)
            }
            canvas.drawText(fmt.format(Date(s.date)), colX[0], y, rowPaint)
            canvas.drawText("${s.durationMinutes}m", colX[1], y, rowPaint)
            canvas.drawText(String.format("%.0f", s.peakFatigueScore), colX[2], y, rowPaint)
            canvas.drawText(String.format("%.0f%%", s.avgSymmetryScore), colX[3], y, rowPaint)
            canvas.drawText(String.format("%.0f%%", s.enduranceScore), colX[4], y, rowPaint)
            canvas.drawText("${s.fallRiskEventCount}", colX[5], y, rowPaint)
            y += 22f
            if (y > PAGE_H - 60f) return@forEachIndexed
        }

        // Footer
        val footerY = PAGE_H - 40f
        canvas.drawLine(MARGIN, footerY - 12f, PAGE_W - MARGIN, footerY - 12f, paintLine)
        canvas.drawText("GaitGuard — Real-time Gait & Posture Monitor | Page 2 of 3", MARGIN, footerY, paintSub)
    }

    private fun drawTrendPage(canvas: Canvas, sessions: List<SessionRecord>, current: GaitResult?) {
        // Header
        paintPrimary.alpha = 255
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 60f, paintPrimary)
        val wPaint = Paint(paintH2).also { it.color = Color.WHITE }
        canvas.drawText("Trend Analysis", MARGIN, 40f, wPaint)

        var y = 90f
        val recent = sessions.take(7).reversed()  // chronological for trend
        val currentScore = currentGlobalScore(current)

        // Draw simple bar chart for fatigue trend
        canvas.drawText("PEAK FATIGUE — LAST 7 SESSIONS", MARGIN, y, paintH2); y += 30f
        drawBarChart(canvas, recent.map { it.peakFatigueScore }, Color.parseColor("#BA1A1A"),
            MARGIN, y, PAGE_W - 2 * MARGIN, 120f); y += 140f

        // Symmetry trend
        canvas.drawText("AVG SYMMETRY — LAST 7 SESSIONS", MARGIN, y, paintH2); y += 30f
        drawBarChart(canvas, recent.map { it.avgSymmetryScore }, Color.parseColor("#44DDC1"),
            MARGIN, y, PAGE_W - 2 * MARGIN, 120f); y += 140f

        // Endurance trend
        canvas.drawText("ENDURANCE SCORE — LAST 7 SESSIONS", MARGIN, y, paintH2); y += 30f
        drawBarChart(canvas, recent.map { it.enduranceScore }, Color.parseColor("#003178"),
            MARGIN, y, PAGE_W - 2 * MARGIN, 120f); y += 160f

        // Interpretation
        canvas.drawText("TREND INTERPRETATION", MARGIN, y, paintH2); y += 30f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, paintLine); y += 20f

        val fatTrend  = if (recent.size >= 2) recent.last().peakFatigueScore - recent.first().peakFatigueScore else 0f
        val symTrend  = if (recent.size >= 2) recent.last().avgSymmetryScore - recent.first().avgSymmetryScore else 0f
        val endTrend  = if (recent.size >= 2) recent.last().enduranceScore - recent.first().enduranceScore else 0f

        val lines = listOf(
            "Fatigue trend: ${if (fatTrend > 5) "↑ Increasing (monitor closely)" else if (fatTrend < -5) "↓ Decreasing (good progress)" else "→ Stable"}",
            "Symmetry trend: ${if (symTrend > 2) "↑ Improving" else if (symTrend < -2) "↓ Declining (consult physio)" else "→ Stable"}",
            "Endurance trend: ${if (endTrend > 5) "↑ Improving" else if (endTrend < -5) "↓ Declining" else "→ Stable"}"
        )
        lines.forEach { line ->
            canvas.drawText(line, MARGIN, y, paintBody); y += 22f
        }
        if (current != null) {
            y += 12f
            canvas.drawText("Current global gait score: ${currentScore.toInt()} / 100", MARGIN, y, paintBody)
        }

        // Footer
        val footerY = PAGE_H - 40f
        canvas.drawLine(MARGIN, footerY - 12f, PAGE_W - MARGIN, footerY - 12f, paintLine)
        canvas.drawText("GaitGuard — Real-time Gait & Posture Monitor | Page 3 of 3", MARGIN, footerY, paintSub)
    }

    private fun drawBarChart(
        canvas: Canvas, values: List<Float>, barColor: Int,
        left: Float, top: Float, width: Float, height: Float
    ) {
        if (values.isEmpty()) return
        val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val barW = width / (values.size * 1.5f)
        val gap  = barW * 0.5f
        val barPaint = Paint().also { it.color = barColor; it.style = Paint.Style.FILL }
        val bgPaint  = Paint().also { it.color = Color.parseColor("#F0F4F8"); it.style = Paint.Style.FILL }

        canvas.drawRect(left, top, left + width, top + height, bgPaint)
        values.forEachIndexed { i, v ->
            val x = left + i * (barW + gap) + gap
            val barH = (v / maxVal) * (height - 20f)
            barPaint.alpha = 200
            canvas.drawRect(x, top + height - barH, x + barW, top + height, barPaint)
            val label = String.format("%.0f", v)
            val lp = Paint(paintSub).also { it.textSize = 9f }
            canvas.drawText(label, x + barW / 2 - lp.measureText(label) / 2,
                top + height - barH - 4f, lp)
        }
    }

    private fun wrapText(canvas: Canvas, text: String, x: Float, y: Float, maxW: Float, paint: Paint) {
        var curY = y
        val words = text.split(" ")
        var line = ""
        words.forEach { word ->
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxW) {
                canvas.drawText(line, x, curY, paint)
                curY += paint.textSize * 1.4f
                line = word
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, curY, paint)
    }

    private fun currentGlobalScore(current: GaitResult?): Float {
        if (current == null) return 0f
        val fatigueHealth = 100f - current.fatigueScore.coerceIn(0f, 100f)
        val symmetry = current.symmetryScore.coerceIn(0f, 100f)
        val harmonic = (current.harmonicRatio / 6f * 100f).coerceIn(0f, 100f)
        val fallRisk = 100f - (current.fallRiskHigh * 100f).coerceIn(0f, 100f)
        return (fatigueHealth * 0.35f + symmetry * 0.30f + harmonic * 0.20f + fallRisk * 0.15f).coerceIn(0f, 100f)
    }

    // ── MediaStore save ───────────────────────────────────────────────────────

    private fun saveToDownloads(context: Context, document: PdfDocument): Uri? {
        val fileName = "GaitGuard_Report_${System.currentTimeMillis()}.pdf"
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
            }
            uri
        } catch (e: IOException) {
            null
        }
    }
}
