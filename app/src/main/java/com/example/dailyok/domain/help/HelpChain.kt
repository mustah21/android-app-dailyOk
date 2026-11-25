package com.example.dailyok.domain.help

/** Someone the person can call for help. */
data class HelpContact(val id: String, val name: String, val phone: String)

/** What "I need help" should do next. */
sealed interface HelpAction {
    val id: String

    data class CallContact(val contact: HelpContact) : HelpAction {
        override val id: String get() = contact.id
    }

    data class CallEmergency(val number: String) : HelpAction {
        override val id: String get() = EMERGENCY_ID
    }

    companion object {
        const val EMERGENCY_ID = "emergency"
    }
}

/**
 * Chain of responsibility: each link either takes the help request or passes it to the next one.
 * Order: contact 1 → contact 2 → … → emergency services, which always takes it.
 *
 * A contact passes the request on when they were already tried (no answer)
 * or their phone number can't be called.
 */
abstract class HelpHandler {
    private var next: HelpHandler? = null

    /** Links [handler] after this one and returns it, so calls can be chained. */
    fun then(handler: HelpHandler): HelpHandler {
        next = handler
        return handler
    }

    /** The first link that can help, given who has already been tried. */
    fun handle(alreadyTried: Set<String>): HelpAction? =
        tryHandle(alreadyTried) ?: next?.handle(alreadyTried)

    protected abstract fun tryHandle(alreadyTried: Set<String>): HelpAction?
}

class ContactHandler(private val contact: HelpContact) : HelpHandler() {
    override fun tryHandle(alreadyTried: Set<String>): HelpAction? = when {
        contact.id in alreadyTried -> null
        !HelpChain.isCallable(contact.phone) -> null
        else -> HelpAction.CallContact(contact)
    }
}

/** Last link: emergency services always answer. */
class EmergencyHandler(private val number: String = HelpChain.EMERGENCY_NUMBER) : HelpHandler() {
    override fun tryHandle(alreadyTried: Set<String>): HelpAction = HelpAction.CallEmergency(number)
}

object HelpChain {

    /** Finland and the rest of the EU. */
    const val EMERGENCY_NUMBER = "112"

    /** Builds the chain from the contacts in the order they are saved in Settings. */
    fun build(contacts: List<HelpContact>, emergencyNumber: String = EMERGENCY_NUMBER): HelpHandler {
        val links: List<HelpHandler> = contacts.map(::ContactHandler) + EmergencyHandler(emergencyNumber)
        links.zipWithNext { a, b -> a.then(b) }
        return links.first()
    }

    /** Every step of the chain in order, for showing the whole chain on screen. */
    fun steps(chain: HelpHandler): List<HelpAction> {
        val steps = mutableListOf<HelpAction>()
        val tried = mutableSetOf<String>()
        while (true) {
            val action = chain.handle(tried) ?: break
            steps += action
            if (action is HelpAction.CallEmergency) break
            tried += action.id
        }
        return steps
    }

    /** The contacts the chain will reach, in order (skips numbers that can't be used). */
    fun contactsInOrder(contacts: List<HelpContact>): List<HelpContact> =
        steps(build(contacts)).filterIsInstance<HelpAction.CallContact>().map { it.contact }

    fun isCallable(phone: String): Boolean = phone.count(Char::isDigit) >= 6
}
