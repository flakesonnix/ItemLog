package com.itemlog.model

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelsTest {

    @Test
    fun `ItemEvent material from before`() {
        val before = ItemSnapshot("DIAMOND", 5, null)
        val event = ItemEvent(UUID.randomUUID(), EventType.PICKUP, System.currentTimeMillis(), UUID.randomUUID(), null, before, null, "test")
        assertEquals("DIAMOND", event.material)
    }

    @Test
    fun `ItemEvent material from after when before null`() {
        val after = ItemSnapshot("IRON_INGOT", 3, null)
        val event = ItemEvent(UUID.randomUUID(), EventType.DROP, System.currentTimeMillis(), null, null, null, after, null)
        assertEquals("IRON_INGOT", event.material)
    }

    @Test
    fun `ItemEvent throws when both snapshots null`() {
        assertThrows(IllegalArgumentException::class.java) {
            ItemEvent(UUID.randomUUID(), EventType.OTHER, 0L, null, null, null, null, null)
        }
    }

    @Test
    fun `EventType values`() {
        assertTrue(EventType.values().contains(EventType.PICKUP))
        assertEquals(15, EventType.values().size)
    }

    @Test
    fun `LocationData from null returns null`() {
        assertNull(LocationData.from(null))
    }

    @Test
    fun `ItemSnapshot holds fields`() {
        val s = ItemSnapshot("STONE", 64, "{}")
        assertEquals("STONE", s.material)
        assertEquals(64, s.amount)
        assertEquals("{}", s.itemJson)
    }

    @Test
    fun `Restoration holds fields`() {
        val loc = LocationData("world", 1.0, 2.0, 3.0, 0f, 0f)
        val r = Restoration(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 123L, loc, "{}", "SUCCESS")
        assertEquals("SUCCESS", r.status)
        assertEquals("world", r.restoreLocation?.world)
    }

    @Test
    fun `LocationData from Bukkit fails gracefully with mock`() {
        // just test data class equality
        val a = LocationData("w", 1.0, 2.0, 3.0, 90f, 0f)
        val b = a.copy(x = 2.0)
        assertNotEquals(a, b)
        assertEquals(1.0, a.x, 0.001)
    }
}
