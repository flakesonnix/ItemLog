package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.ItemLogService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceExtractEvent
import java.util.UUID

class CraftSmeltListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        val result = event.recipe.result.clone()
        val before = ItemSnapshot(result.type.name, result.amount, serializer.serialize(result))
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.CRAFT_RESULT,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(player.location),
            before = null,
            after = before,
            source = "CRAFT"
        )
        service.log(e)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSmelt(event: FurnaceExtractEvent) {
        val player = event.player
        val item = org.bukkit.inventory.ItemStack(event.itemType, event.itemAmount)
        val after = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.SMELT_RESULT,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(player.location),
            before = null,
            after = after,
            source = "FURNACE"
        )
        service.log(e)
    }
}
