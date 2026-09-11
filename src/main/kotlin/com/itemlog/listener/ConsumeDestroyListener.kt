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
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import java.util.UUID

class ConsumeDestroyListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item.clone()
        val before = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.CONSUME,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(player.location),
            before = before,
            after = null,
            source = "CONSUME"
        )
        service.log(e)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: PlayerItemBreakEvent) {
        val player = event.player
        val item = event.brokenItem.clone()
        val before = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.DESTROY,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(player.location),
            before = before,
            after = null,
            source = "BREAK"
        )
        service.log(e)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDespawn(event: ItemDespawnEvent) {
        val item = event.entity.itemStack.clone()
        val before = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
        val loc = event.entity.location.let { LocationData.from(it) }
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.DESTROY,
            timestamp = System.currentTimeMillis(),
            playerId = null,
            location = loc,
            before = before,
            after = null,
            source = "DESPAWN"
        )
        service.log(e)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamage(event: PlayerItemDamageEvent) {
        // durability change — log as DESTROY with before/after damage
        val player = event.player
        val item = event.item.clone()
        val before = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
        // after would be same item with increased damage, but we log before for audit
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.DESTROY,
            timestamp = System.currentTimeMillis(),
            playerId = player.uniqueId,
            location = LocationData.from(player.location),
            before = before,
            after = null,
            source = "DAMAGE"
        )
        service.log(e)
    }
}
