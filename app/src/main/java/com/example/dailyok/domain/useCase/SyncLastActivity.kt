package com.example.dailyok.domain.useCase

import com.example.dailyok.domain.interfaces.ActivityRepository
import com.example.dailyok.domain.interfaces.ActivitySource
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

class SyncLastActivity(
    private val source: ActivitySource,
    private val repo: ActivityRepository,
) {
    suspend operator fun invoke() {
        if (!source.hasPermission()) return
        val now = Instant.now()
        val found = source.lastActivityBetween(now.minus(24, ChronoUnit.HOURS), now) ?: return
        val saved = repo.lastActivity.first()
        if (saved == null || found.isAfter(saved)) repo.save(found)
    }
}