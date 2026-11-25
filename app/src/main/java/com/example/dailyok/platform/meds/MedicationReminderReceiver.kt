package com.example.dailyok.platform.meds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalDateTime

/** Receives the medication alarm and the "I took it" button on the reminder. */
class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val at = intent.getStringExtra(MedicationReminders.EXTRA_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        when (intent.action) {
            MedicationReminders.ACTION_REMIND -> at?.let { MedicationReminders.showReminder(context, it) }
            MedicationReminders.ACTION_TAKEN -> at?.let { MedicationReminders.markTakenFromNotification(context, it) }
            else -> Log.w("MedicationReminder", "Unknown action ${intent.action}")
        }
        MedicationReminders.schedule(context)
    }
}
