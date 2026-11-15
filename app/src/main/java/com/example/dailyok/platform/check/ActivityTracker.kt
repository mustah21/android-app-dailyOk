package com.example.dailyok.platform.check

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.dailyok.data.CheckStore

/**
 * Records "signs of normal life" and moves the next check accordingly.
 *
 * Sources today:
 *  - the phone is unlocked (ACTION_USER_PRESENT)
 *  - the phone is taken off the charger (ACTION_POWER_DISCONNECTED)
 *  - the app is opened, or "I'm OK" is pressed (called from MainActivity / CheckInHost)
 *  - a pickup, once PickupDetector is wired in (call [record] from its onPickup)
 *
 * Note: the system broadcasts are registered at runtime, so they are only heard while the app's
 * process is alive. The "Resume after restart" card (foreground service) will keep it alive
 * all the time; until then, the morning and daytime checks still fire on time, but they can
 * only see activity from while the app was running.
 */
object ActivityTracker {

    private const val TAG = "ActivityTracker"
    private var registered = false

    fun record(context: Context, source: String) {
        Log.d(TAG, "Activity: $source")
        CheckStore(context).recordActivity()
        CheckNotifier.cancelPrompt(context)
        CheckScheduler.reschedule(context)
    }

    /** Call once from DailyOkApp.onCreate. */
    fun registerSystemSignals(context: Context) {
        if (registered) return
        registered = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            context.applicationContext,
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val source = when (intent.action) {
                        Intent.ACTION_USER_PRESENT -> "phone unlocked"
                        Intent.ACTION_POWER_DISCONNECTED -> "charger unplugged"
                        else -> return
                    }
                    record(ctx, source)
                }
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
