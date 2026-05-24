package com.gaitguard.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaitguard.app.data.db.SessionRecord
import com.gaitguard.app.data.repository.GaitRepository
import com.gaitguard.app.data.repository.PostureRepository
import com.gaitguard.app.sensor.PersonalAdaptationEngine
import com.gaitguard.app.util.CsvExporter
import com.gaitguard.app.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class HistoryFilter { ALL, HIGH_FATIGUE, RECENT }

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository        = GaitRepository(app.applicationContext)
    private val postureRepository = PostureRepository(app.applicationContext)

    private val _weeklyPostureHealth = MutableStateFlow<List<Float>>(emptyList())
    val weeklyPostureHealth: StateFlow<List<Float>> = _weeklyPostureHealth

    private val _activeFilter = MutableStateFlow(HistoryFilter.ALL)
    val activeFilter: StateFlow<HistoryFilter> = _activeFilter

    private val _sessions = MutableStateFlow<List<SessionRecord>>(emptyList())
    val sessions: StateFlow<List<SessionRecord>> = _sessions

    val adaptation = PersonalAdaptationEngine.getThresholds(app.applicationContext)

    init {
        viewModelScope.launch {
            combine(
                repository.allSessions,
                _activeFilter
            ) { all, filter ->
                when (filter) {
                    HistoryFilter.ALL          -> all
                    HistoryFilter.HIGH_FATIGUE -> all.filter { it.peakFatigueScore >= 40f }
                    HistoryFilter.RECENT       -> all.take(10)
                }
            }.collect { _sessions.value = it }
        }
        viewModelScope.launch {
            postureRepository.recentSnapshots.collect { snapshots ->
                val now   = System.currentTimeMillis()
                val dayMs = 24L * 60 * 60 * 1000
                _weeklyPostureHealth.value = (6 downTo 0).map { daysAgo ->
                    val start = now - (daysAgo + 1) * dayMs
                    val end   = now - daysAgo * dayMs
                    val avg   = snapshots.filter { it.timestamp in start..end }.map { s ->
                        val k  = when { s.kyphosisAngleDeg > 50f -> 30f; s.kyphosisAngleDeg > 40f -> 65f; else -> 100f }
                        val sh = when { abs(s.shoulderLevelDeg) > 5f -> 35f; abs(s.shoulderLevelDeg) > 2f -> 70f; else -> 100f }
                        val p  = when { abs(s.pelvicTiltDeg) > 4f -> 35f; abs(s.pelvicTiltDeg) > 2f -> 70f; else -> 100f }
                        val a  = if (s.axialAligned) 100f else 50f
                        k * 0.35f + sh * 0.25f + p * 0.25f + a * 0.15f
                    }.average()
                    if (avg.isNaN()) 0f else avg.toFloat()
                }
            }
        }
    }

    fun setFilter(filter: HistoryFilter) { _activeFilter.value = filter }

    fun exportCsv(): Uri? = CsvExporter.exportSessions(getApplication(), _sessions.value)

    /** Generate a 3-page PDF report and return its Uri (null on failure). */
    fun generatePdfReport(): Uri? =
        PdfReportGenerator.generate(getApplication(), _sessions.value)

    fun getWeeklyEndurance(): List<Float> = _sessions.value.take(7).map { it.enduranceScore }
    fun getWeeklySymmetry():  List<Float> = _sessions.value.take(7).map { it.avgSymmetryScore }
}
