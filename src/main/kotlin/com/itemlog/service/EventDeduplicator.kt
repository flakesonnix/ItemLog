package com.itemlog.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Avoid duplicate events (e.g. InventoryClick fires twice, hopper move + click).
 * Key = playerId + material + amount + type + 50ms window
 */
class EventDeduplicator(private val windowMs: Long = 50) {
    private val seen = ConcurrentHashMap<String, Long>()

    fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = seen[key]
        if (last != null && now - last < windowMs) return true
        seen[key] = now
        // cleanup old
        if (seen.size > 1000) {
            val cutoff = now - windowMs * 10
            seen.entries.removeIf { it.value < cutoff }
        }
        return false
    }

    fun key(playerId: String?, type: String, material: String, amount: Int): String = "$playerId|$type|$material|$amount"
}
