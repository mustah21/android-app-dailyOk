package com.example.dailyok.domain.interfaces

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ActivityRepository {
    val lastActivity: Flow<Instant?>
    suspend fun save(time: Instant)
}