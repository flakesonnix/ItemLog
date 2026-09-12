package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.ItemLogService
import java.util.UUID
import org.bukkit.entity.AbstractVillager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.MerchantInventory

class TradingListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTrade(e: InventoryClickEvent) {
        if (e.inventory.type != InventoryType.MERCHANT) return
        if (e.slotType != org.bukkit.event.inventory.InventoryType.SlotType.RESULT) return
        if (e.currentItem == null || e.currentItem!!.type.isAir) return

        val player = e.whoClicked as? org.bukkit.entity.Player ?: return
        val merchant = e.inventory as? MerchantInventory ?: return
        val trader = merchant.holder as? AbstractVillager

        val result = e.currentItem!!.clone()
        val recipe = merchant.selectedRecipe ?: return

        // Log input items (what player gave)
        for (ingredient in recipe.ingredients) {
            if (ingredient != null && !ingredient.type.isAir) {
                val event = ItemEvent(
                    eventId = UUID.randomUUID(),
                    type = EventType.OTHER,
                    timestamp = System.currentTimeMillis(),
                    playerId = player.uniqueId,
                    location = trader?.location?.let { LocationData.from(it) } ?: LocationData.from(player.location),
                    before = ItemSnapshot(
                        material = ingredient.type.name,
                        amount = ingredient.amount,
                        itemJson = serializer.serialize(ingredient),
                    ),
                    after = null,
                    source = "TRADE_INPUT_${trader?.type?.name ?: "UNKNOWN"}",
                )
                service.log(event)
            }
        }

        // Log output item (what player got)
        val eventOut = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.OTHER,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = trader?.location?.let { LocationData.from(it) } ?: LocationData.from(player.location),
            before = null,
            after = ItemSnapshot(
                material = result.type.name,
                amount = result.amount,
                itemJson = serializer.serialize(result),
            ),
            source = "TRADE_RESULT_${trader?.type?.name ?: "UNKNOWN"}",
        )
        service.log(eventOut)
    }
}
