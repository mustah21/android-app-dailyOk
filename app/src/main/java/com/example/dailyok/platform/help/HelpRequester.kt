package com.example.dailyok.platform.help

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.dailyok.data.CheckStore
import com.example.dailyok.domain.help.HelpChain
import com.example.dailyok.domain.help.HelpContact

/** An active request for help: when it was sent and who was asked. */
data class HelpRequest(val requestedAtMs: Long, val contacts: List<HelpContact>)

/** Sends the actual messages. The "SMS alert to family" card replaces [HelpRequester.sender] with SmsManager. */
fun interface HelpAlertSender {
    fun send(contacts: List<HelpContact>, message: String)
}

/**
 * One place that reaches out to the contacts, used by:
 *  - the "I need help" button (Get help screen), right away
 *  - the alarm flow, when the alarm countdown runs out with no answer
 *
 * Contacts are asked in the order set in Settings > Contacts (see [HelpChain]).
 */
object HelpRequester {

    private const val TAG = "HelpRequester"
    private const val PREFS_NAME = "help_prefs"
    private const val KEY_REQUESTED_MS = "help_requested_ms"

    const val HELP_MESSAGE = "DailyOK: I need help. Please call me or come over as soon as you can."
    const val CANCEL_MESSAGE = "DailyOK: All OK, sorry for the alarm."

    /** Placeholder until the SMS card is done: only logs what would be sent. */
    var sender: HelpAlertSender = HelpAlertSender { contacts, message ->
        contacts.forEachIndexed { i, c -> Log.i(TAG, "Message ${i + 1} to ${c.name} (${c.phone}): $message") }
    }

    /** Asks all contacts for help now and remembers the request. Returns the active request if one is already open. */
    fun requestHelp(context: Context): HelpRequest {
        current(context)?.let { return it }
        val now = System.currentTimeMillis()
        prefs(context).edit { putLong(KEY_REQUESTED_MS, now) }
        val contacts = contacts(context)
        Log.d(TAG, "Help requested from ${contacts.size} contact(s)")
        runCatching { sender.send(contacts, HELP_MESSAGE) }
            .onFailure { Log.e(TAG, "Could not send help request", it) }
        return HelpRequest(now, contacts)
    }

    /** The open request, or null if there is none. */
    fun current(context: Context): HelpRequest? {
        val at = prefs(context).getLong(KEY_REQUESTED_MS, 0L)
        return if (at > 0L) HelpRequest(at, contacts(context)) else null
    }

    /** Pressed by mistake or the person is fine: close the request and tell the contacts. */
    fun cancel(context: Context) {
        val request = current(context) ?: return
        prefs(context).edit { remove(KEY_REQUESTED_MS) }
        runCatching { sender.send(request.contacts, CANCEL_MESSAGE) }
            .onFailure { Log.e(TAG, "Could not send cancel message", it) }
    }

    private fun contacts(context: Context): List<HelpContact> =
        HelpChain.contactsInOrder(CheckStore(context).contacts().map { HelpContact(it.id, it.name, it.phone) })

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
