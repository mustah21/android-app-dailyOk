package com.example.dailyok.domain.useCase

import com.example.dailyok.domain.interfaces.AccelerometerSource
import com.example.dailyok.domain.service.PickupDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MonitorPickups(
    private val source: AccelerometerSource,
    private val detector: PickupDetector,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope): Boolean {
        detector.reset()
        if (!source.start()) return false   // no accelerometer like the phone doesn't have one
        job = scope.launch { source.readings().collect(detector::onReading) }
        return true
    }

    fun stop() {
        job?.cancel()
        source.stop()
    }

}