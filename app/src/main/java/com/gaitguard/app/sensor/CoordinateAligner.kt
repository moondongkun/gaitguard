package com.gaitguard.app.sensor

import com.gaitguard.app.util.GaitConstants
import kotlin.math.sqrt

/**
 * Aligns raw sensor data into a body-fixed frame regardless of phone carry position.
 * Uses a low-pass filtered gravity estimate to compute the rotation matrix.
 *
 * Convention: body-fixed Z = gravity direction (vertical), X = lateral, Y = forward.
 */
class CoordinateAligner {

    private val alpha = GaitConstants.GRAVITY_ALPHA
    private val gravity = FloatArray(3) { 0f }
    private var isInitialized = false

    /** Call with every raw accelerometer reading (m/s²). */
    fun updateGravity(rawAcc: FloatArray) {
        for (i in 0..2) {
            gravity[i] = alpha * gravity[i] + (1f - alpha) * rawAcc[i]
        }
        isInitialized = true
    }

    /**
     * Rotate [rawAcc] and [rawGyro] into the body-fixed frame.
     * Returns FloatArray(6) = [AccX, AccY, AccZ, GyroX, GyroY, GyroZ] in body frame.
     */
    fun align(rawAcc: FloatArray, rawGyro: FloatArray): FloatArray {
        if (!isInitialized) return rawAcc + rawGyro

        // Estimate vertical (Z) axis from gravity vector
        val gravMag = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (gravMag < 1e-6f) return rawAcc + rawGyro

        val zAxis = floatArrayOf(gravity[0] / gravMag, gravity[1] / gravMag, gravity[2] / gravMag)

        // Choose a reference orthogonal to Z (world X or Y, whichever is more stable)
        val ref = if (Math.abs(zAxis[0]) < 0.9f) floatArrayOf(1f, 0f, 0f)
                  else floatArrayOf(0f, 1f, 0f)

        val xAxis = cross(ref, zAxis).normalized()
        val yAxis = cross(zAxis, xAxis)

        // Rotation matrix rows = body frame axes
        val rotatedAcc = floatArrayOf(
            dot(xAxis, rawAcc),
            dot(yAxis, rawAcc),
            dot(zAxis, rawAcc)
        )
        val rotatedGyro = floatArrayOf(
            dot(xAxis, rawGyro),
            dot(yAxis, rawGyro),
            dot(zAxis, rawGyro)
        )

        return floatArrayOf(
            rotatedAcc[0], rotatedAcc[1], rotatedAcc[2],
            rotatedGyro[0], rotatedGyro[1], rotatedGyro[2]
        )
    }

    private fun cross(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    )

    private fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    private fun FloatArray.normalized(): FloatArray {
        val mag = sqrt(this[0] * this[0] + this[1] * this[1] + this[2] * this[2])
        return if (mag < 1e-6f) this else floatArrayOf(this[0] / mag, this[1] / mag, this[2] / mag)
    }

    private operator fun FloatArray.plus(other: FloatArray): FloatArray =
        FloatArray(this.size + other.size).also { r ->
            this.copyInto(r, 0)
            other.copyInto(r, this.size)
        }
}
