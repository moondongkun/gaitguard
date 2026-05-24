package com.gaitguard.app.sensor

import android.content.Context
import com.gaitguard.app.data.db.SessionRecord
import kotlin.math.max
import kotlin.math.min

/**
 * On-device personalized gait adaptation.
 *
 * After the user has collected ≥ MIN_SESSIONS sessions, this engine:
 *  1. Analyses their personal fatigue-onset patterns
 *  2. Derives user-specific amber/red thresholds (instead of population defaults)
 *  3. Saves them to SharedPreferences for use by FatigueScoreCalculator
 *
 * This is "personalised ML" without model retraining — we adapt the
 * decision thresholds to the user's personal baseline.
 */
object PersonalAdaptationEngine {

    private const val PREFS_NAME      = "gaitguard_adaptation"
    private const val KEY_AMBER       = "personal_amber_thresh"
    private const val KEY_RED         = "personal_red_thresh"
    private const val KEY_ENABLED     = "adaptation_enabled"
    private const val KEY_SESSION_CNT = "adapted_session_count"
    const val MIN_SESSIONS = 5

    data class PersonalThresholds(
        val amberThreshold: Int,
        val redThreshold: Int,
        val sessionCount: Int,
        val isEnabled: Boolean
    )

    /** Load persisted thresholds (or null = not enough data yet). */
    fun getThresholds(context: Context): PersonalThresholds {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PersonalThresholds(
            amberThreshold = prefs.getInt(KEY_AMBER, 40),
            redThreshold   = prefs.getInt(KEY_RED, 70),
            sessionCount   = prefs.getInt(KEY_SESSION_CNT, 0),
            isEnabled      = prefs.getBoolean(KEY_ENABLED, false)
        )
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Recompute personal thresholds from [sessions].
     * Called from HistoryViewModel after a session is added.
     * No-op if fewer than MIN_SESSIONS sessions exist.
     */
    fun adapt(context: Context, sessions: List<SessionRecord>) {
        if (sessions.size < MIN_SESSIONS) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return

        // Strategy: Use the 30th percentile of peak fatigue scores as the user's
        // personal "amber" threshold, and the 70th percentile as "red".
        // This ensures the thresholds fit the user's actual fatigue distribution.
        val sorted = sessions.sortedBy { it.peakFatigueScore }
        val p30idx = (sorted.size * 0.30f).toInt().coerceAtLeast(0)
        val p70idx = (sorted.size * 0.70f).toInt().coerceIn(0, sorted.lastIndex)

        val amber = sorted[p30idx].peakFatigueScore.toInt()
            .coerceIn(20, 55)  // never go below 20 or above 55
        val red   = sorted[p70idx].peakFatigueScore.toInt()
            .coerceIn(max(amber + 10, 45), 85)  // always at least amber+10

        prefs.edit()
            .putInt(KEY_AMBER, amber)
            .putInt(KEY_RED, red)
            .putInt(KEY_SESSION_CNT, sessions.size)
            .apply()
    }

    /** Reset to default population thresholds. */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_AMBER, 40)
            .putInt(KEY_RED, 70)
            .putInt(KEY_SESSION_CNT, 0)
            .remove(KEY_ENABLED)
            .apply()
    }
}
