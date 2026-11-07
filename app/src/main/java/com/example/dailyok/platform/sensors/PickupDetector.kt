package com.example.dailyok.platform.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.acos
import kotlin.math.sqrt

class PickupDetector(
    context: Context,
    private val onPickup: () -> Unit,
    private val onDebug: ((String) -> Unit)? = null,
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravity = FloatArray(3)          // current orientation (low-pass filtered)
    private var restGravity: FloatArray? = null  // orientation while the phone was last still
    private var haveGravity = false
    private var stillSinceNs: Long = 0               // when the phone last became still
    private var motionStartNs: Long = 0               // 0 = no motion in progress
    private var lastMotionNs: Long = 0
    private var armed = false                    // true once the phone has been still for a while

    fun start(): Boolean {
        val sensor = accelerometer ?: return false
        reset()
        return sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    private fun reset() {
        haveGravity = false
        restGravity = null
        stillSinceNs = 0L
        motionStartNs = 0L
        lastMotionNs = 0L
        armed = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val t = event.timestamp
        val v = event.values

        if (!haveGravity) {
            for (i in 0..2) gravity[i] = v[i]
            haveGravity = true
            return
        }

        for (i in 0..2) gravity[i] = ALPHA * gravity[i] + (1 - ALPHA) * v[i]
        val lx = v[0] - gravity[0]
        val ly = v[1] - gravity[1]
        val lz = v[2] - gravity[2]
        val linear = sqrt(lx * lx + ly * ly + lz * lz)

        if (linear < STILL_THRESHOLD) {
            if (stillSinceNs == 0L) stillSinceNs = t
            if (t - stillSinceNs >= STILL_NS) {
                restGravity = gravity.copyOf()
                armed = true
            }
        } else {
            stillSinceNs = 0L
        }

        if (linear > LIFT_THRESHOLD) {
            if (motionStartNs == 0L) motionStartNs = t
            lastMotionNs = t
        } else if (motionStartNs != 0L && t - lastMotionNs > GAP_NS) {
            onDebug?.invoke("movement ended after ${(lastMotionNs - motionStartNs) / MS} ms (no pickup)")
            motionStartNs = 0L
        }

        val rest = restGravity
        if (armed && rest != null && motionStartNs != 0L) {
            val motionNs = lastMotionNs - motionStartNs
            val tilt = angleDeg(gravity, rest)
            if (motionNs >= MIN_MOTION_NS && tilt >= MIN_TILT_DEG) {
                onDebug?.invoke("PICKUP: motion=${motionNs / MS} ms, tilt=${"%.0f".format(tilt)} deg")
                armed = false
                motionStartNs = 0L
                onPickup()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun angleDeg(a: FloatArray, b: FloatArray): Float {
        val dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
        val na = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
        val nb = sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2])
        if (na == 0f || nb == 0f) return 0f
        val cos = (dot / (na * nb)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    private companion object {
        const val MS: Long = 1_000_000               // nanoseconds per millisecond
        const val ALPHA = 0.8f                   // gravity low-pass filter
        const val STILL_THRESHOLD = 0.4f         // m/s^2: below this, the phone counts as still
        const val STILL_NS = 1_000 * MS          // still this long before arming
        const val LIFT_THRESHOLD = 1.2f          // m/s^2: above this, it counts as movement
        const val GAP_NS = 250 * MS              // pause that ends a movement
        const val MIN_MOTION_NS = 300 * MS       // a bump is shorter than this
        const val MIN_TILT_DEG = 20f             // orientation change needed for a pickup
    }
}



// !!!!!!?! implementation of this class, I think it should be fine

//val detector = PickupDetector(
//    context,
//    onPickup = { Log.d("Pickup", "picked up") },
//    onDebug = { Log.d("PickupDebug", it) },
//)
//if (!detector.start()) { /* no accelerometer */ }
// later: detector.stop()