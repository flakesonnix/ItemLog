package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.ItemLogService
import java.util.UUID
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.AnvilInventory

class AnvilListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAnvil(e: InventoryClickEvent) {
        if (e.inventory.type != InventoryType.ANVIL) return
        if (e.slotType != org.bukkit.event.inventory.InventoryType.SlotType.RESULT) return
        if (e.currentItem == null || e.currentItem!!.type.isAir) return

        val player = e.whoClicked as? org.bukkit.entity.Player ?: return
        val anvil = e.inventory as? AnvilInventory ?: return

        val before1 = anvil.getItem(0)
        val before2 = anvil.getItem(1)
        val result = e.currentItem!!.clone()

        // Log the combination/repair
        val event = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.OTHER,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(player.location),
            before = before1?.let {
                ItemSnapshot(
                    material = it.type.name,
                    amount = it.amount,
                    itemJson = serializer.serialize(it),
                )
            },
            after = ItemSnapshot(
                material = result.type.name,
                amount = result.amount,
                itemJson = serializer.serialize(result),
            ),
            source = "ANVIL_${if (before2 != null) "COMBINE" else "RENAME"}",
        )
        service.log(event)
    }
}
