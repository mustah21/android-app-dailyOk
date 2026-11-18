package com.example.dailyok.domain.interfaces

import com.example.dailyok.domain.model.AccelerometerReading
import kotlinx.coroutines.flow.Flow

interface AccelerometerSource {
    fun start(): Boolean
    fun stop()
    fun readings(): Flow<AccelerometerReading>
}
