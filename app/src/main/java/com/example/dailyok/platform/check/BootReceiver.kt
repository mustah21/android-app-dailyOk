package com.example.dailyok.platform.check

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Restarts DailyOK background checks and foreground service after phone restarts (BOOT_COMPLETED).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received boot event action=$action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            CheckScheduler.reschedule(context)
            CheckService.start(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
