package com.example.dailyok.platform.check

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by the alarm that [CheckScheduler] sets. Works with the screen off and the app closed. */
class CheckAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CheckScheduler.ACTION_PROMPT_TIMEOUT -> CheckScheduler.onPromptTimeout(context)
            CheckScheduler.ACTION_ALARM_TIMEOUT -> CheckScheduler.onAlarmTimeout(context)
            else -> CheckScheduler.runCheck(context)
        }
        CheckService.start(context)
    }
}
