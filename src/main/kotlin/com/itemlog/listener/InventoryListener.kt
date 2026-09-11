package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.ItemLogService
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import java.util.UUID

class InventoryListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        // ignore container clicks already handled by ContainerListener (top inventory)
        if (event.view.topInventory.type != org.bukkit.event.inventory.InventoryType.CRAFTING &&
            event.view.topInventory.type != org.bukkit.event.inventory.InventoryType.PLAYER) {
            // if click in top container, ContainerListener already handled
            if (event.rawSlot < event.view.topInventory.size) return
        }
        val current = event.currentItem
        val cursor = event.cursor
        // Inventory changes: if current is taken or cursor placed
        if (current != null && !current.type.isAir) {
            val before = ItemSnapshot(current.type.name, current.amount, serializer.serialize(current))
            val e = ItemEvent(
                eventId = UUID.randomUUID(),
                type = EventType.INVENTORY_REMOVE,
                timestamp = System.currentTimeMillis(),
                playerId = player.uniqueId,
                location = LocationData.from(player.location),
                before = before,
                after = null,
                source = "INVENTORY_CLICK"
            )
            service.log(e)
        }
        if (cursor != null && !cursor.type.isAir && event.isLeftClick) {
            val after = ItemSnapshot(cursor.type.name, cursor.amount, serializer.serialize(cursor))
            val e = ItemEvent(
                eventId = UUID.randomUUID(),
                type = EventType.INVENTORY_ADD,
                timestamp = System.currentTimeMillis(),
                playerId = player.uniqueId,
                location = LocationData.from(player.location),
                before = null,
                after = after,
                source = "INVENTORY_CLICK"
            )
            service.log(e)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val item = event.oldCursor
        if (item.type.isAir) return
        val before = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.INVENTORY_ADD,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(player.location),
            before = null,
            after = before,
            source = "INVENTORY_DRAG"
        )
        service.log(e)
    }
}
