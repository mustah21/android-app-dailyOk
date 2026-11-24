package com.example.dailyok.platform.meds

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dailyok.MainActivity
import com.example.dailyok.R
import com.example.dailyok.data.MedicationStore
import com.example.dailyok.domain.meds.DoseStatus
import com.example.dailyok.domain.meds.MedicationSchedule
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Medication reminders: one alarm is always set for the next dose time
 * ([MedicationSchedule.nextReminder]). When it fires, a notification lists the medicines
 * for that time, with an "I took it" button. Then the next alarm is set.
 *
 * Rescheduled after every change to the medicines (MedicationStore.onChanged),
 * on app start and after the phone restarts (BootReceiver).
 */
object MedicationReminders {

    private const val TAG = "MedicationReminders"
    private const val CHANNEL_ID = "medication"
    private const val NOTIFICATION_ID = 4300
    private const val REQUEST_ALARM = 4301
    private const val REQUEST_TAKEN = 4302
    private const val REQUEST_OPEN = 4303

    const val ACTION_REMIND = "com.example.dailyok.MED_REMIND"
    const val ACTION_TAKEN = "com.example.dailyok.MED_TAKEN"
    const val EXTRA_AT = "at"   // dose date and time, ISO text

    /** Set (or move) the alarm for the next dose. */
    fun schedule(context: Context) {
        val data = MedicationStore.current(context)
        val now = LocalDateTime.now()
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        // Nothing due any more (taken in the app): remove a reminder that is still showing.
        if (MedicationSchedule.dosesOn(now.toLocalDate(), data.medications, data.taken, now)
                .none { it.status == DoseStatus.DUE }
        ) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }

        val next = MedicationSchedule.nextReminder(data.medications, data.taken, now)
        if (next == null) {
            alarmManager.cancel(alarmIntent(context, null))
            Log.d(TAG, "No medications: reminder cancelled")
            return
        }
        val triggerAtMs = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pending = alarmIntent(context, next)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
        }
        Log.d(TAG, "Next medication reminder at $next (exact=$canExact)")
    }

    /** The alarm fired: show what to take now. */
    @SuppressLint("MissingPermission") // checked below on Android 13+
    fun showReminder(context: Context, at: LocalDateTime) {
        val data = MedicationStore.current(context)
        val doses = MedicationSchedule.dueAt(at, data.medications, data.taken, LocalDateTime.now())
        if (doses.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted: medication reminder not shown")
            return
        }
        createChannel(context)

        val text = doses.joinToString("\n") { d ->
            if (d.medication.dose.isBlank()) d.medication.name else "${d.medication.name} (${d.medication.dose})"
        }
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            // Opens the app; the home screen shows the medicines due now with "I took it".
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val taken = PendingIntent.getBroadcast(
            context,
            REQUEST_TAKEN,
            Intent(context, MedicationReminderReceiver::class.java)
                .setAction(ACTION_TAKEN)
                .putExtra(EXTRA_AT, at.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_dailyok)
            .setContentTitle("Time for your medicine")
            .setContentText(text.replace("\n", ", "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(0, "I took it", taken)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** "I took it" on the notification: mark every dose of that time as taken. */
    fun markTakenFromNotification(context: Context, at: LocalDateTime) {
        val data = MedicationStore.current(context)
        MedicationSchedule.dueAt(at, data.medications, data.taken, LocalDateTime.now()).forEach { dose ->
            MedicationStore.markTaken(context, dose.medication.id, dose.date, dose.time)
        }
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun alarmIntent(context: Context, at: LocalDateTime?): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            if (at != null) putExtra(EXTRA_AT, at.toString())
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Medication reminders", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Reminds you when it is time to take your medicine" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
