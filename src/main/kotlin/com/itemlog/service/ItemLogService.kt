package com.itemlog.service

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.repository.ItemEventRepository
import com.itemlog.serialization.ItemSerializer
import java.util.UUID
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class ItemLogService(
    private val plugin: JavaPlugin,
    private val serializer: ItemSerializer,
    private val repository: ItemEventRepository,
) {
    private val buffer = EventBuffer(plugin, repository)

    fun start() = buffer.start()
    fun stop() = buffer.stop()

    fun log(event: ItemEvent) {
        // must be called on main thread, but persists async via buffer
        buffer.add(event)
    }

    // Ensure ordering: timestamp is set at event creation, not flush time

    fun logPickup(playerId: UUID, location: Location, item: ItemStack) {
        val event = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.PICKUP,
            timestamp = System.currentTimeMillis(),
            playerId = playerId,
            location = LocationData.from(location),
            before = null,
            after = ItemSnapshot(
                material = item.type.name,
                amount = item.amount,
                itemJson = serializer.serialize(item),
            ),
            source = "PLAYER_PICKUP",
        )
        log(event)
    }

    fun logDrop(playerId: UUID, location: Location, item: ItemStack) {
        val event = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.DROP,
            timestamp = System.currentTimeMillis(),
            playerId = playerId,
            location = LocationData.from(location),
            before = ItemSnapshot(
                material = item.type.name,
                amount = item.amount,
                itemJson = serializer.serialize(item),
            ),
            after = null,
            source = "PLAYER_DROP",
        )
        log(event)
    }

    // For testing: flush synchronously
    fun flush() = buffer.flush()
}
