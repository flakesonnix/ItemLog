package com.itemlog.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventDeduplicatorTest {

    @Test
    fun `isDuplicate within window`() {
        val d = EventDeduplicator(windowMs = 100)
        val key = d.key("player1", "PICKUP", "DIAMOND", 1)
        assertFalse(d.isDuplicate(key)) // first time not duplicate
        assertTrue(d.isDuplicate(key)) // second time within window is duplicate
    }

    @Test
    fun `not duplicate after window`() {
        val d = EventDeduplicator(windowMs = 50)
        val key = d.key("p", "DROP", "STONE", 64)
        assertFalse(d.isDuplicate(key))
        Thread.sleep(60)
        assertFalse(d.isDuplicate(key))
    }

    @Test
    fun `different keys not duplicate`() {
        val d = EventDeduplicator()
        val k1 = d.key("p1", "PICKUP", "DIAMOND", 1)
        val k2 = d.key("p2", "PICKUP", "DIAMOND", 1)
        assertFalse(d.isDuplicate(k1))
        assertFalse(d.isDuplicate(k2))
    }

    @Test
    fun `key format`() {
        val d = EventDeduplicator()
        val k = d.key("uuid", "TYPE", "MAT", 5)
        assertTrue(k.contains("uuid"))
        assertTrue(k.contains("TYPE"))
        assertTrue(k.contains("MAT"))
        assertTrue(k.contains("5"))
    }

    @Test
    fun `null playerId handled`() {
        val d = EventDeduplicator()
        val k = d.key(null, "OTHER", "AIR", 0)
        assertFalse(d.isDuplicate(k))
        assertTrue(d.isDuplicate(k))
    }
}
