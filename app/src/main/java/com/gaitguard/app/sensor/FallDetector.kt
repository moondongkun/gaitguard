package com.gaitguard.app.sensor

import com.gaitguard.app.util.GaitConstants
import kotlin.math.sqrt

sealed class FallEvent {
    object TrueFall : FallEvent()
    object MicroStumble : FallEvent()
}

class FallDetector(private val onEvent: (FallEvent) -> Unit) {

    // ── Fall detection FSM ──────────────────────────────────────────────────
    private enum class FallState { NORMAL, FREEFALL, IMPACT_WINDOW }

    private var fallState = FallState.NORMAL
    private var freefallSampleCount = 0
    private var impactWindowStartMs = 0L
    private var lastFallEventMs = 0L

    // ── Micro-stumble detection ─────────────────────────────────────────────
    private val latBaseline = ArrayDeque<Float>(400)
    private var stumbleStartMs = 0L
    private var inStumble = false
    private var stumbleMaxLatAcc = 0f

    // Gyro roll reversal detection
    private var prevRollGy = 0f
    private var rollReversalMs = 0L

    /**
     * Feed one aligned sensor sample. [sample] = FloatArray(6).
     * Must be called at 50 Hz from GaitSensorManager.
     */
    fun processSample(sample: FloatArray, timestampMs: Long) {
        val accX = sample[0]; val accY = sample[1]; val accZ = sample[2]
        val gyroX = sample[3]  // roll (body-frame)

        val magnitude = sqrt(accX * accX + accY * accY + accZ * accZ)

        processFallFSM(magnitude, timestampMs)
        processMicroStumble(accX, gyroX, timestampMs)

        // Maintain lateral baseline
        latBaseline.addLast(accX)
        if (latBaseline.size > 400) latBaseline.removeFirst()

        prevRollGy = gyroX
    }

    // ── Fall FSM ────────────────────────────────────────────────────────────

    private fun processFallFSM(magnitude: Float, ts: Long) {
        if (ts - lastFallEventMs < 8_000L) return

        when (fallState) {
            FallState.NORMAL -> {
                if (magnitude < GaitConstants.FREEFALL_MS2) {
                    freefallSampleCount = 1
                    fallState = FallState.FREEFALL
                }
            }
            FallState.FREEFALL -> {
                if (magnitude < GaitConstants.FREEFALL_MS2) {
                    freefallSampleCount++
                    if (freefallSampleCount >= GaitConstants.FREEFALL_MIN_SAMPLES) {
                        fallState = FallState.IMPACT_WINDOW
                        impactWindowStartMs = ts
                    }
                } else {
                    fallState = FallState.NORMAL
                    freefallSampleCount = 0
                }
            }
            FallState.IMPACT_WINDOW -> {
                val elapsed = ts - impactWindowStartMs
                if (magnitude > GaitConstants.IMPACT_MS2 * 1.20f) {
                    lastFallEventMs = ts
                    onEvent(FallEvent.TrueFall)
                    fallState = FallState.NORMAL
                    freefallSampleCount = 0
                } else if (elapsed > GaitConstants.IMPACT_WINDOW_MS) {
                    fallState = FallState.NORMAL
                    freefallSampleCount = 0
                }
            }
        }
    }

    // Micro-stumble detection ─────────────────────────────────────────────

    private fun processMicroStumble(latAcc: Float, rollGy: Float, ts: Long) {
        val baseline = if (latBaseline.isNotEmpty())
            latBaseline.map { kotlin.math.abs(it) }.average().toFloat() else 1f
        val threshold = (baseline * GaitConstants.MICRO_STUMBLE_MULT).coerceAtLeast(1.2f)

        if (!inStumble) {
            if (kotlin.math.abs(latAcc) > threshold) {
                inStumble = true
                stumbleStartMs = ts
                stumbleMaxLatAcc = latAcc
            }
        } else {
            val duration = ts - stumbleStartMs
            if (duration > GaitConstants.MICRO_STUMBLE_MS) {
                // Lasted too long — not a stumble
                inStumble = false
            } else if (kotlin.math.abs(latAcc) <= threshold) {
                // Stumble ended — check for gyro roll reversal
                rollReversalMs = ts
                inStumble = false
            }
        }

        // Check for roll reversal within 150ms of stumble end
        if (rollReversalMs > 0) {
            val sinceSt = ts - rollReversalMs
            if (sinceSt <= GaitConstants.GYRO_REVERSAL_MS) {
                if (prevRollGy * rollGy < 0f) {  // sign reversal
                    onEvent(FallEvent.MicroStumble)
                    rollReversalMs = 0L
                }
            } else {
                rollReversalMs = 0L
            }
        }
    }

    fun reset() {
        fallState = FallState.NORMAL
        freefallSampleCount = 0
        lastFallEventMs = 0L
        inStumble = false
        latBaseline.clear()
        rollReversalMs = 0L
    }
}
