package com.example.dailyok.platform.check

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.dailyok.data.CheckStore
import com.example.dailyok.data.DemoSettings
import com.example.dailyok.domain.CheckDecision
import com.example.dailyok.domain.CheckEngine
import com.example.dailyok.platform.usage.UsageActivitySource
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.temporal.ChronoUnit

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
    private const val REQUEST_CODE_PROMPT_TIMEOUT = 4204
    private const val REQUEST_CODE_ALARM_TIMEOUT = 4205

    const val ACTION_PROMPT_TIMEOUT = "com.example.dailyok.PROMPT_TIMEOUT"
    const val ACTION_ALARM_TIMEOUT = "com.example.dailyok.ALARM_TIMEOUT"

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
        // checks for latest sync btw it gets the usage after 12 hrs
        syncPhoneActivity(context, store, now)

        val decision = CheckEngine.evaluate(now, store.lastActivity(), store.lastPrompt(), store.config())
        Log.d(TAG, "Check at $now: $decision")
        if (decision is CheckDecision.Prompt) {
            val promptSeconds = DemoSettings.current(context).promptSeconds
            store.startPromptFlow()
            requestPrompt()                                     // app already open: show it at once
            CheckNotifier.showPrompt(context, decision.reason)  // app closed or screen off
            scheduleTimeout(context, ACTION_PROMPT_TIMEOUT, REQUEST_CODE_PROMPT_TIMEOUT, promptSeconds)
        }
        reschedule(context)
    }

    fun onPromptTimeout(context: Context) {
        val store = CheckStore(context)
        if (store.checkInStep() == "NONE") return
        Log.d(TAG, "Prompt timeout: starting Alarm phase")
        val alarmSeconds = DemoSettings.current(context).alarmSeconds
        store.startAlarmFlow()
        requestPrompt()
        CheckNotifier.showAlarm(context)
        scheduleTimeout(context, ACTION_ALARM_TIMEOUT, REQUEST_CODE_ALARM_TIMEOUT, alarmSeconds)
    }

    fun onAlarmTimeout(context: Context) {
        val store = CheckStore(context)
        if (store.checkInStep() == "NONE") return
        Log.d(TAG, "Alarm timeout: starting HelpSent phase")
        store.startHelpSentFlow()
        requestPrompt()
        CheckNotifier.showHelpSent(context)
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

    private fun syncPhoneActivity(context: Context, store: CheckStore, now: LocalDateTime) {
        runCatching {
            val source = UsageActivitySource(context)
            if (!source.hasPermission()) return

            val zone = ZoneId.systemDefault()
            val nowInstant = now.atZone(zone).toInstant()

            // lastActivity() returns LocalDateTime? -> convert, don't cast
            val known: Instant? = store.lastActivity()?.atZone(zone)?.toInstant()
            val from = known ?: nowInstant.minus(1, ChronoUnit.DAYS)

            val latest = source.lastActivityBetween(from, nowInstant) ?: return

            store.recordActivity(latest.toEpochMilli())
            Log.d(TAG, "Recorded phone activity at ${LocalDateTime.ofInstant(latest, zone)}")
        }.onFailure { Log.w(TAG, "Could not read phone activity", it) }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(alarmIntent(context))
        am.cancel(timeoutIntent(context, ACTION_PROMPT_TIMEOUT, REQUEST_CODE_PROMPT_TIMEOUT))
        am.cancel(timeoutIntent(context, ACTION_ALARM_TIMEOUT, REQUEST_CODE_ALARM_TIMEOUT))
    }

    private fun scheduleTimeout(context: Context, action: String, requestCode: Int, delaySeconds: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMs = System.currentTimeMillis() + delaySeconds * 1_000L
        val pending = timeoutIntent(context, action, requestCode)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
        }
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, CheckAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun timeoutIntent(context: Context, action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, CheckAlarmReceiver::class.java).apply { this.action = action },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
