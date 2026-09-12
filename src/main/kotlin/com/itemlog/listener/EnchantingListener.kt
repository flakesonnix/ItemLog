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
import org.bukkit.event.enchantment.EnchantItemEvent

class EnchantingListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEnchant(e: EnchantItemEvent) {
        val player = e.enchanter
        val before = e.item.clone()
        val after = e.item.clone()

        // Apply enchantments to after
        e.enchantsToAdd.forEach { (ench, level) ->
            after.addUnsafeEnchantment(ench, level)
        }

        val event = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.OTHER,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(e.enchantBlock.location),
            before = ItemSnapshot(
                material = before.type.name,
                amount = before.amount,
                itemJson = serializer.serialize(before),
            ),
            after = ItemSnapshot(
                material = after.type.name,
                amount = after.amount,
                itemJson = serializer.serialize(after),
            ),
            source = "ENCHANT_TABLE",
        )
        service.log(event)
    }
}
