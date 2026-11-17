package com.example.dailyok.platform.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.dailyok.domain.interfaces.ActivitySource
import java.time.Instant

class UsageActivitySource(private val context: Context) : ActivitySource {

    override fun hasPermission(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun lastActivityBetween(from: Instant, to: Instant): Instant? {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val events = usm.queryEvents(from.toEpochMilli(), to.toEpochMilli())
        val e = UsageEvents.Event()
        var latest = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.SCREEN_INTERACTIVE ||
                e.eventType == UsageEvents.Event.KEYGUARD_HIDDEN
            ) latest = maxOf(latest, e.timeStamp)
        }
        return if (latest > 0) Instant.ofEpochMilli(latest) else null
    }
}