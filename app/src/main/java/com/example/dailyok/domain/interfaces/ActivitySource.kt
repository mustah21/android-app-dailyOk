package com.example.dailyok.domain.interfaces

import java.time.Instant

interface ActivitySource {
    fun hasPermission(): Boolean
    fun lastActivityBetween(from: Instant, to: Instant): Instant?
}