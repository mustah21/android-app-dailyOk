package com.example.dailyok.ui.checkin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Makes the alarm noticeable from another room:
 * - repeating alarm tone (on the alarm volume stream)
 * - strong repeating vibration (needs <uses-permission android:name="android.permission.VIBRATE" />)
 * - screen kept on at full brightness
 * Everything stops automatically when the alarm screen leaves the screen.
 */
@Composable
fun AlarmEffects() {
    // Android Studio previews can't play sounds or vibrate.
    if (LocalInspectionMode.current) return
    val context = LocalContext.current

    // Screen on + full brightness while the alarm is showing.
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val oldBrightness = window?.attributes?.screenBrightness
        window?.let { w ->
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            w.attributes = w.attributes.apply { screenBrightness = 1f }
        }
        onDispose {
            window?.let { w ->
                w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (oldBrightness != null) {
                    w.attributes = w.attributes.apply { screenBrightness = oldBrightness }
                }
            }
        }
    }

    // Vibration: 0.8 s on, 0.4 s off, repeating.
    DisposableEffect(Unit) {
        val vibrator = context.vibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 800, 400), 0)
        }
        onDispose { vibrator?.cancel() }
    }

    // Alarm tone, repeated every 2 seconds.
    LaunchedEffect(Unit) {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        try {
            while (isActive) {
                tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
                delay(2000)
            }
        } finally {
            tone.release()
        }
    }
}

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
