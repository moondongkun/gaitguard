package com.gaitguard.app.sensor

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.gaitguard.app.util.GaitConstants
import kotlin.math.sqrt

data class BaselineProfile(
    val mean: FloatArray = FloatArray(12),
    val std: FloatArray  = FloatArray(12) { 1f }
)

class BaselineCalibrator(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // Collected feature vectors during calibration walk
    private val featureBuffer = mutableListOf<FloatArray>()

    fun isCalibrated(): Boolean = prefs.contains(KEY_PROFILE)

    fun getProfile(): BaselineProfile {
        val json = prefs.getString(KEY_PROFILE, null) ?: return BaselineProfile()
        return try { gson.fromJson(json, BaselineProfile::class.java) } catch (_: Exception) { BaselineProfile() }
    }

    fun addSample(features: FloatArray) {
        featureBuffer.add(features.copyOf())
    }

    fun complete(): BaselineProfile {
        if (featureBuffer.isEmpty()) return BaselineProfile()

        val n = featureBuffer.size
        val mean = FloatArray(12) { i -> featureBuffer.sumOf { it[i].toDouble() }.toFloat() / n }
        val std  = FloatArray(12) { i ->
            val v = featureBuffer.sumOf { ((it[i] - mean[i]) * (it[i] - mean[i])).toDouble() } / n
            maxOf(sqrt(v).toFloat(), 1e-6f)
        }
        val profile = BaselineProfile(mean, std)
        save(profile)
        featureBuffer.clear()
        return profile
    }

    fun reset() {
        featureBuffer.clear()
        prefs.edit().remove(KEY_PROFILE).apply()
    }

    private fun save(profile: BaselineProfile) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply()
    }

    companion object {
        private const val PREFS_NAME = "gaitguard_baseline"
        private const val KEY_PROFILE = "profile_v1"
    }
}
