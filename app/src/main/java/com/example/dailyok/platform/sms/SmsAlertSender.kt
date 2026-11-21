package com.example.dailyok.platform.sms

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Sends SMS alerts to emergency contacts.
 *
 * All public functions are fire-and-forget: they log failures but never throw,
 * so callers don't have to wrap them in try/catch.
 */
object SmsAlertSender {

    private const val TAG = "SmsAlertSender"

    /**
     * Called when the check-in alarm expires without an answer.
     * Sends sequentially: contact 1 first, then contact 2 (if present).
     *
     * @param contacts     phone numbers to alert (1 or 2 entries, in order)
     * @param lastActivity human-readable time of last known activity, e.g. "08:34"
     * @param lastLocation human-readable last known location, or null if unavailable
     */
    fun sendNoAnswerAlert(
        context: Context,
        contacts: List<String>,
        lastActivity: String,
        lastLocation: String? = null,
    ) {
        val locationPart = if (lastLocation != null) "\nLast known location: $lastLocation" else ""
        val body = "⚠️ DailyOK alert: no response since $lastActivity.$locationPart Please check on them."
        contacts.forEach { phone -> sendSms(context, phone, body) }
    }

    /**
     * Called when the user taps "I need help" on the home screen.
     * Sends to ALL contacts simultaneously.
     */
    fun sendHelpRequest(
        context: Context,
        contacts: List<String>,
        lastLocation: String? = null,
    ) {
        val locationPart = if (lastLocation != null) "\nLast known location: $lastLocation" else ""
        val body = "🆘 DailyOK: I need help! Please come now.$locationPart"
        contacts.forEach { phone -> sendSms(context, phone, body) }
    }

    /**
     * Cancels a previous alert by sending an "All OK" follow-up to all contacts.
     */
    fun sendCancellation(
        context: Context,
        contacts: List<String>,
    ) {
        val body = "✅ DailyOK: All OK — sorry for the alarm. Everything is fine."
        contacts.forEach { phone -> sendSms(context, phone, body) }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    @Suppress("DEPRECATION") // SmsManager.getDefault() is fine below API 31
    private fun sendSms(context: Context, phone: String, body: String) {
        if (phone.isBlank()) return
        try {
            val manager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            // splitMessage handles texts longer than 160 characters automatically.
            val parts = manager.divideMessage(body)
            if (parts.size == 1) {
                manager.sendTextMessage(phone, null, body, null, null)
            } else {
                manager.sendMultipartTextMessage(phone, null, parts, null, null)
            }
            Log.d(TAG, "SMS sent to $phone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phone: ${e.message}")
        }
    }

    /** Format epoch-millis as "H:mm" for use in alert messages. */
    fun formatActivityTime(epochMs: Long): String {
        if (epochMs <= 0L) return "unknown time"
        val ldt = java.time.Instant.ofEpochMilli(epochMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        return ldt.format(DateTimeFormatter.ofPattern("H:mm"))
    }
}
