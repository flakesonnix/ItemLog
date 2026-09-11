package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.ItemLogService
import java.util.UUID
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType

class ContainerListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inv = event.inventory
        val isContainer = inv.type != InventoryType.PLAYER && inv.type != InventoryType.CRAFTING
        if (!isContainer) return
        val item = event.currentItem ?: return
        if (item.type.isAir) return
        val isInsert = event.rawSlot < inv.size // top inventory
        val type = if (isInsert) EventType.CONTAINER_INSERT else EventType.CONTAINER_REMOVE
        val before = if (isInsert) ItemSnapshot(item.type.name, item.amount, serializer.serialize(item)) else null
        val after = if (!isInsert) ItemSnapshot(item.type.name, item.amount, serializer.serialize(item)) else null
        val loc = event.inventory.location?.let { LocationData.from(it) } ?: LocationData.from(player.location)
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = type,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = loc,
            before = before,
            after = after,
            source = inv.type.name,
        )
        service.log(e)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: InventoryMoveItemEvent) {
        val item = event.item
        if (item.type.isAir) return
        val loc = event.source.location?.let { LocationData.from(it) } ?: return
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.CONTAINER_INSERT,
            timestamp = System.currentTimeMillis(),
            playerId = null,
            location = loc,
            before = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item)),
            after = null,
            source = "HOPPER:${event.destination.type.name}",
        )
        service.log(e)
    }
}
