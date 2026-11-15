package com.example.dailyok.platform.check

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.dailyok.data.CheckStore
import com.example.dailyok.domain.CheckDecision
import com.example.dailyok.domain.CheckEngine
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Runs the morning check and the daytime inactivity check.
 *
 * One alarm is always set for the next time a check could be due (from [CheckEngine.nextCheckTime]).
 * When it fires, [runCheck] asks the engine what to do; if a prompt is needed it starts the
 * "Are you OK?" flow, then sets the next alarm. New activity also moves the alarm (see [ActivityTracker]).
 */
object CheckScheduler {

    private const val TAG = "CheckScheduler"
    private const val REQUEST_CODE = 4201

    /** True while a check wants the prompt shown. MainActivity passes it to CheckInHost. */
    private val _promptPending = MutableStateFlow(false)
    val promptPending: StateFlow<Boolean> = _promptPending.asStateFlow()

    /** Ask the UI to show the "Are you OK?" prompt now. */
    fun requestPrompt() = _promptPending.update { true }

    /** Called by the UI once the prompt is on screen. */
    fun onPromptShown() = _promptPending.update { false }

    /** Evaluate the checks now, start the prompt if needed, and set the next alarm. */
    fun runCheck(context: Context) {
        val store = CheckStore(context)
        val now = LocalDateTime.now()
        val decision = CheckEngine.evaluate(now, store.lastActivity(), store.lastPrompt(), store.config())
        Log.d(TAG, "Check at $now: $decision")
        if (decision is CheckDecision.Prompt) {
            store.recordPrompt()
            requestPrompt()                                     // app already open: show it at once
            CheckNotifier.showPrompt(context, decision.reason)  // app closed or screen off
        }
        reschedule(context)
    }

    /** Set (or move) the alarm for the next time a check could be due. */
    fun reschedule(context: Context) {
        val store = CheckStore(context)
        val next = CheckEngine.nextCheckTime(
            LocalDateTime.now(), store.lastActivity(), store.lastPrompt(), store.config(),
        )
        val triggerAtMs = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            .coerceAtLeast(System.currentTimeMillis() + 1_000)

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
        } else {
            // Exact alarms not allowed (Android 14 default): may fire a few minutes late.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
        }
        Log.d(TAG, "Next check at $next (exact=$canExact)")
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, CheckAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
