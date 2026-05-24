package com.gaitguard.app.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "SmsHelper"

object SmsHelper {

    suspend fun sendEmergencySms(context: Context, phone: String, reason: String) {
        if (phone.isBlank()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SEND_SMS permission not granted")
            return
        }

        val location = getLastKnownLocation(context)
        val body = buildSmsBody(reason, location)

        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()
                val parts = smsManager.divideMessage(body)
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                Log.i(TAG, "Emergency SMS sent to $phone")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS", e)
            }
        }
    }

    private fun buildSmsBody(reason: String, location: Location?): String {
        val loc = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location unavailable"
        }
        return "⚠️ GaitGuard Alert: $reason. Location: $loc"
    }

    private suspend fun getLastKnownLocation(context: Context): Location? {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try last-known first (instant)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (provider in providers) {
            try {
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) return loc
            } catch (_: Exception) {}
        }

        // Request a single fresh fix with 5s timeout
        return withTimeoutOrNull(GaitConstants.GPS_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val handler = Handler(Looper.getMainLooper())
                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        lm.removeUpdates(this)
                        if (cont.isActive) cont.resume(loc)
                    }
                    @Deprecated("Deprecated in API 29")
                    override fun onStatusChanged(provider: String, status: Int, extras: android.os.Bundle?) {}
                }
                try {
                    @Suppress("MissingPermission")
                    lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, handler.looper)
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }
        }
    }
}
