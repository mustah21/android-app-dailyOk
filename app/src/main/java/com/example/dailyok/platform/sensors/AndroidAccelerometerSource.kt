package com.example.dailyok.platform.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.dailyok.domain.interfaces.AccelerometerSource
import com.example.dailyok.domain.model.AccelerometerReading
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class AndroidAccelerometerSource(context: Context): AccelerometerSource, SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _readings = MutableSharedFlow<AccelerometerReading>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun start(): Boolean {
        val sensor = accelerometer ?: return false
        return sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun stop() = sensorManager.unregisterListener(this)
    override fun readings(): Flow<AccelerometerReading> = _readings

    override fun onSensorChanged(e: SensorEvent) {
        _readings.tryEmit(AccelerometerReading(e.values[0], e.values[1], e.values[2], e.timestamp))
    }

    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit


}




