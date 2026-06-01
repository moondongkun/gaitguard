package com.gaitguard.app.util

object GaitConstants {
    const val SAMPLE_RATE_HZ        = 50
    const val WINDOW_SAMPLES        = 200      // 4-second window
    const val WINDOW_OVERLAP        = 100      // inference every 2 seconds
    const val RING_BUFFER_CAPACITY  = 400
    const val MIN_STEP_INTERVAL_MS  = 250L
    const val LPF_CUTOFF_HZ         = 5f
    const val FREEFALL_MS2          = 4.9f     // 0.5g threshold
    const val IMPACT_MS2            = 29.4f    // 3g threshold
    const val FREEFALL_MIN_SAMPLES  = 10       // 200 ms at 50 Hz
    const val IMPACT_WINDOW_MS      = 500L
    const val FATIGUE_AMBER         = 40
    const val FATIGUE_RED           = 70
    const val SYMMETRY_WARN         = 90f
    const val ISI_CV_HEALTHY        = 3f
    const val ISI_CV_FATIGUED       = 8f
    const val HR_HEALTHY            = 5f       // harmonic ratio
    const val HR_FATIGUED           = 3f
    const val HIGH_RISK_SMS_DELAY   = 30L      // seconds
    const val BASELINE_DURATION_SEC = 120
    const val TERRAIN_UPDATE_SEC    = 8
    const val POSTURE_FPS           = 15
    const val POSTURE_CONFIDENCE    = 0.6f
    const val POSTURE_MIN_CONF      = 0.5f     // show instruction below this
    const val POSTURE_SHOW_CONF     = 0.7f     // dismiss instruction above this
    const val GRAVITY_ALPHA         = 0.8f     // low-pass gravity filter
    const val PEAK_THRESHOLD_MULT   = 1.2f     // dynamic step threshold multiplier
    const val MICRO_STUMBLE_MULT    = 4.5f     // lateral acc × baseline for stumble (higher = less sensitive)
    const val MICRO_STUMBLE_FLOOR   = 2.5f     // absolute lateral-acc floor (m/s²) to ignore tiny spikes
    const val MICRO_STUMBLE_COOLDOWN_MS = 2_000L // min gap between reported stumbles
    const val MICRO_STUMBLE_MS      = 200L
    const val GYRO_REVERSAL_MS      = 150L
    const val REST_DETECTION_SEC    = 60
    const val FATIGUE_ONSET_WINDOW  = 30       // seconds
    const val FATIGUE_ONSET_THRESH  = 40
    const val FATIGUE_ONSET_HYSTER  = 35
    const val GPS_TIMEOUT_MS        = 5000L

    // Terrain threshold adjustments
    const val TERRAIN_STAIRS_ASC_RELAX   = 0.08f  // relax symmetry by 8%
    const val TERRAIN_UNEVEN_RELAX       = 0.12f  // relax symmetry by 12%

    // Composite fatigue weights
    const val WEIGHT_TCN_FATIGUE   = 0.55f
    const val WEIGHT_ISI_CV        = 0.20f
    const val WEIGHT_HARMONIC      = 0.15f
    const val WEIGHT_TRUNK_TILT    = 0.10f
}
