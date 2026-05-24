package com.gaitguard.app.sensor

import com.gaitguard.app.util.GaitConstants
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe circular ring buffer.
 * Each slot holds FloatArray(6) = [AccX, AccY, AccZ, GyroX, GyroY, GyroZ].
 */
class RingBuffer(private val capacity: Int = GaitConstants.RING_BUFFER_CAPACITY) {

    private val buffer = Array(capacity) { FloatArray(6) }
    private var head = 0
    private var size = 0
    private val lock = ReentrantReadWriteLock()

    val count: Int get() = lock.read { size }
    val isFull: Boolean get() = lock.read { size == capacity }

    fun push(sample: FloatArray) = lock.write {
        buffer[head].also { slot -> sample.copyInto(slot) }
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    /**
     * Copy the last [n] samples into a new array, oldest first.
     * Returns an empty array if fewer than [n] samples are available.
     */
    fun copyWindow(n: Int): Array<FloatArray> = lock.read {
        if (size < n) return@read emptyArray()
        val result = Array(n) { FloatArray(6) }
        val startIdx = (head - n + capacity) % capacity
        for (i in 0 until n) {
            val src = buffer[(startIdx + i) % capacity]
            src.copyInto(result[i])
        }
        result
    }

    fun clear() = lock.write { head = 0; size = 0 }
}
