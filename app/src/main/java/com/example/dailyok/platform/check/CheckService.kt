package com.example.dailyok.platform.check

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.dailyok.MainActivity
import com.example.dailyok.R
import com.example.dailyok.domain.service.PickupDetector
import com.example.dailyok.domain.useCase.MonitorPickups
import com.example.dailyok.platform.sensors.AndroidAccelerometerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground service that ensures DailyOK stays active in the background.
 *
 * Keeps the process alive so system signals (unlocks, charger unplugged) and sensor events
 * (pickup detection) are received continuously, even after phone restart (BOOT_COMPLETED)
 * or when the app is closed.
 */
class CheckService : Service() {

    private var monitorPickups: MonitorPickups? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating CheckService")
        createNotificationChannel()
        if (!startForegroundServiceNotification()) {
            stopSelf()
            return
        }

        ActivityTracker.registerSystemSignals(applicationContext)
        CheckScheduler.reschedule(applicationContext)

        val detector = PickupDetector(
            onPickup = { ActivityTracker.record(applicationContext, "phone picked up") },
            onDebug = { Log.d("PickupDebug", it) },
        )
        val monitor = MonitorPickups(
            source = AndroidAccelerometerSource(applicationContext),
            detector = detector,
        )
        if (monitor.start(serviceScope)) {
            monitorPickups = monitor
        } else {
            Log.w(TAG, "PickupDetector failed to start (no accelerometer sensor)")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting CheckService")
        if (!startForegroundServiceNotification()) {
            stopSelf()
            return START_NOT_STICKY
        }
        CheckScheduler.reschedule(applicationContext)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying CheckService")
        monitorPickups?.stop()
        monitorPickups = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Shows the ongoing notification. Returns false if Android refused, instead of crashing the app. */
    private fun startForegroundServiceNotification(): Boolean {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_dailyok)
            .setContentTitle("DailyOK protection active")
            .setContentText("Monitoring daily activity and scheduling checks")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException when started from the background (Android 12+)
            Log.e(TAG, "Could not start foreground notification", e)
            false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Background Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps DailyOK monitoring active in the background"
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "CheckService"
        private const val CHANNEL_ID = "dailyok_service"
        private const val NOTIFICATION_ID = 4203

        fun start(context: Context) {
            try {
                val intent = Intent(context, CheckService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start CheckService", e)
            }
        }
    }
}
