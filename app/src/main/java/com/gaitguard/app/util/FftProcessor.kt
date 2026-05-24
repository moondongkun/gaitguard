package com.gaitguard.app.util

import kotlin.math.*

/**
 * Cooley-Tukey radix-2 FFT on a real-valued input window.
 * Input is zero-padded to the next power of two (256 for 200-sample windows).
 * fs = 50 Hz assumed throughout.
 */
class FftProcessor(private val fs: Float = 50f) {

    private var magnitudes: FloatArray = FloatArray(0)

    /** Run FFT on the input signal. Must call this before query methods. */
    fun compute(signal: FloatArray) {
        val n = nextPow2(signal.size)
        val re = FloatArray(n) { if (it < signal.size) signal[it] else 0f }
        val im = FloatArray(n)
        fft(re, im)
        magnitudes = FloatArray(n / 2) { sqrt(re[it] * re[it] + im[it] * im[it]) }
    }

    /** Frequency of the bin with highest magnitude in [fMin, fMax] Hz. */
    fun dominantFrequency(fMin: Float = 0.5f, fMax: Float = 3f): Float {
        val binMin = freqToBin(fMin)
        val binMax = freqToBin(fMax)
        var maxMag = Float.NEGATIVE_INFINITY
        var maxBin = binMin
        for (i in binMin..min(binMax, magnitudes.size - 1)) {
            if (magnitudes[i] > maxMag) { maxMag = magnitudes[i]; maxBin = i }
        }
        return binToFreq(maxBin)
    }

    /**
     * Harmonic ratio: power in even harmonics vs odd harmonics of the dominant
     * stride frequency in the range [fMin, fMax] Hz.
     * Healthy walking > 5; fatigued < 3.
     */
    fun harmonicRatio(fMin: Float = 0.5f, fMax: Float = 3f): Float {
        val f0 = dominantFrequency(fMin, fMax)
        if (f0 <= 0f) return 0f
        var evenPower = 0.0
        var oddPower  = 0.0
        var k = 1
        while (k * f0 <= fs / 2) {
            val bin = freqToBin(k * f0)
            if (bin < magnitudes.size) {
                val power = magnitudes[bin].toDouble().pow(2)
                if (k % 2 == 0) evenPower += power else oddPower += power
            }
            k++
        }
        return if (oddPower == 0.0) 0f else (evenPower / oddPower).toFloat()
    }

    /** Total signal energy (sum of squared magnitudes) in [fLow, fHigh] Hz. */
    fun bandEnergy(fLow: Float, fHigh: Float): Float {
        val binLow  = freqToBin(fLow)
        val binHigh = freqToBin(fHigh)
        var energy = 0f
        for (i in binLow..min(binHigh, magnitudes.size - 1))
            energy += magnitudes[i] * magnitudes[i]
        return energy
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun freqToBin(f: Float): Int =
        (f * magnitudes.size * 2 / fs).roundToInt().coerceAtLeast(0)

    private fun binToFreq(bin: Int): Float =
        bin * fs / (magnitudes.size * 2)

    private fun nextPow2(n: Int): Int {
        var p = 1
        while (p < n) p = p shl 1
        return p
    }

    /** In-place Cooley-Tukey FFT. re and im must be power-of-two length. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { re[i] = re[j].also { re[j] = re[i] }; im[i] = im[j].also { im[j] = im[i] } }
        }
        // Butterfly stages
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f; var curIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]; val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val vIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = uRe + vRe; im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe; im[i + k + len / 2] = uIm - vIm
                    val newCurRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe; curRe = newCurRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
