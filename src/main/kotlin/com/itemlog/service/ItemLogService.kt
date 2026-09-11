package com.itemlog.service

import com.itemlog.db.DataSourceProvider
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.model.EventType
import com.itemlog.serialization.ItemSerializer
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.inventory.ItemStack
import org.bukkit.Location
import java.util.UUID

class ItemLogService(
    private val plugin: JavaPlugin,
    private val serializer: ItemSerializer
) {
    fun logPickup(playerId: UUID, location: Location, item: ItemStack) {
        val event = ItemEvent(
            eventId = UUID.randomUUID(),
            type = com.itemlog.model.EventType.PICKUP,
            timestamp = System.currentTimeMillis(),
            playerId = playerId,
            location = com.itemlog.model.LocationData.from(location),
            before = null,
            after = ItemSnapshot(
                material = item.type.name,
                amount = item.amount,
                itemJson = serializer.serialize(item)
            ),
            source = "PLAYER_PICKUP"
        )
        // TODO: persist to DB
    }

    fun logDrop(playerId: UUID, location: Location, item: ItemStack) {
        val event = ItemEvent(
            eventId = UUID.randomUUID(),
            type = com.itemlog.model.EventType.DROP,
            timestamp = System.currentTimeMillis(),
            playerId = playerId,
            location = com.itemlog.model.LocationData.from(location),
            before = ItemSnapshot(
                material = item.type.name,
                amount = item.amount,
                itemJson = serializer.serialize(item)
            ),
            after = null,
            source = "PLAYER_DROP"
        )
        // TODO: persist to DB
    }
}