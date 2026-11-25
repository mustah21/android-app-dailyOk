package com.example.dailyok.domain.help

import org.junit.Assert.assertEquals
import org.junit.Test

class HelpChainTest {

    private val anna = HelpContact("1", "Anna (daughter)", "+358 40 123 4567")
    private val matti = HelpContact("2", "Matti (neighbour)", "+358 50 765 4321")
    private val emergency = HelpAction.CallEmergency("112")

    @Test fun `first contact is asked first`() =
        assertEquals(HelpAction.CallContact(anna), HelpChain.build(listOf(anna, matti)).handle(emptySet()))

    @Test fun `no answer from first contact - passes to the second`() =
        assertEquals(HelpAction.CallContact(matti), HelpChain.build(listOf(anna, matti)).handle(setOf("1")))

    @Test fun `nobody answers - emergency services`() =
        assertEquals(emergency, HelpChain.build(listOf(anna, matti)).handle(setOf("1", "2")))

    @Test fun `no contacts saved - straight to emergency services`() =
        assertEquals(emergency, HelpChain.build(emptyList()).handle(emptySet()))

    @Test fun `contact without a callable number is skipped`() {
        val broken = anna.copy(phone = "123")
        assertEquals(HelpAction.CallContact(matti), HelpChain.build(listOf(broken, matti)).handle(emptySet()))
    }

    @Test fun `steps list the whole chain in order`() =
        assertEquals(
            listOf(HelpAction.CallContact(anna), HelpAction.CallContact(matti), emergency),
            HelpChain.steps(HelpChain.build(listOf(anna, matti))),
        )

    @Test fun `steps leave out contacts that can't be called`() =
        assertEquals(
            listOf(HelpAction.CallContact(matti), emergency),
            HelpChain.steps(HelpChain.build(listOf(anna.copy(phone = ""), matti))),
        )

    @Test fun `help request goes to contacts in the order chosen in Settings`() =
        assertEquals(listOf(matti, anna), HelpChain.contactsInOrder(listOf(matti, anna)))

    @Test fun `help request skips contacts without a usable number`() =
        assertEquals(listOf(matti), HelpChain.contactsInOrder(listOf(anna.copy(phone = "12"), matti)))
}
