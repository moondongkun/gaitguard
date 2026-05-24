package com.gaitguard.app.util

/**
 * 4th-order Butterworth low-pass filter implemented as two cascaded biquad (SOS) sections.
 * Pre-computed coefficients for fc = 5 Hz, fs = 50 Hz.
 *
 * Coefficients generated via bilinear transform (scipy.signal.butter equivalent):
 *   b, a = butter(4, 5/(50/2), btype='low')
 *   sos  = zpk2sos(*tf2zpk(b, a))
 *
 * Section 0:  b = [0.02008337, 0.04016673, 0.02008337]
 *             a = [1.0, -1.56101808, 0.64135154]
 * Section 1:  b = [1.0, 2.0, 1.0]
 *             a = [1.0, -1.72377617, 0.75702032]
 *
 * Supports multiple channels; each channel maintains its own delay state.
 */
class ButterworthFilter(private val numChannels: Int = 1) {

    // Normalised SOS (second-order sections)
    private val b0 = floatArrayOf(0.02008337f, 1.0f)
    private val b1 = floatArrayOf(0.04016673f, 2.0f)
    private val b2 = floatArrayOf(0.02008337f, 1.0f)
    private val a1 = floatArrayOf(-1.56101808f, -1.72377617f)
    private val a2 = floatArrayOf(0.64135154f,  0.75702032f)

    // Delay registers: [channel][section][z1, z2]
    private val z = Array(numChannels) { Array(2) { FloatArray(2) } }

    fun reset() {
        for (ch in 0 until numChannels)
            for (s in 0..1)
                z[ch][s].fill(0f)
    }

    /** Filter a single sample for the given channel. */
    fun filter(sample: Float, channel: Int = 0): Float {
        var x = sample
        for (s in 0..1) {
            val w  = x - a1[s] * z[channel][s][0] - a2[s] * z[channel][s][1]
            val y  = b0[s] * w + b1[s] * z[channel][s][0] + b2[s] * z[channel][s][1]
            z[channel][s][1] = z[channel][s][0]
            z[channel][s][0] = w
            x = y
        }
        return x
    }

    /** Filter an entire array in-place for the given channel. */
    fun filterArray(samples: FloatArray, channel: Int = 0): FloatArray {
        return FloatArray(samples.size) { i -> filter(samples[i], channel) }
    }
}
