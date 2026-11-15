package com.example.dailyok.platform.check

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dailyok.MainActivity
import com.example.dailyok.R
import com.example.dailyok.domain.CheckReason

/**
 * Shows the "Are you OK?" prompt when the app is closed or the screen is off:
 * a high-priority notification whose full-screen intent opens MainActivity on the prompt.
 */
object CheckNotifier {

    const val EXTRA_START_CHECKIN = "com.example.dailyok.START_CHECKIN"
    private const val CHANNEL_ID = "checkin"
    private const val NOTIFICATION_ID = 4202

    @SuppressLint("MissingPermission") // checked below on Android 13+
    fun showPrompt(context: Context, reason: CheckReason) {
        createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return // MainActivity asks for this permission on first start

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_START_CHECKIN, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = when (reason) {
            CheckReason.MORNING_NO_ACTIVITY -> "Good morning! Pick up your phone or tap here."
            CheckReason.DAYTIME_INACTIVITY -> "We haven't seen you for a while. Tap here."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Are you OK?")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancelPrompt(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Daily check-in", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Asks if you are OK when the phone hasn't been used for a while" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
