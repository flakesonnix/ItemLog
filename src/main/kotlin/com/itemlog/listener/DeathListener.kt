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
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val loc = LocationData.from(player.location)
        val now = System.currentTimeMillis()
        // drops
        for (item in event.drops) {
            if (item.type.isAir) continue
            val snapshot = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
            val e = ItemEvent(
                eventId = UUID.randomUUID(),
                type = EventType.DEATH_DROP,
                timestamp = now,
                playerId = player.uniqueId,
                location = loc,
                before = snapshot,
                after = null,
                source = "DEATH",
            )
            service.log(e)
        }
        // items kept (e.g. keepInventory)
        for (item in event.itemsToKeep) {
            if (item.type.isAir) continue
            val snapshot = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
            val e = ItemEvent(
                eventId = UUID.randomUUID(),
                type = EventType.DEATH_KEEP,
                timestamp = now,
                playerId = player.uniqueId,
                location = loc,
                before = snapshot,
                after = snapshot,
                source = "DEATH_KEEP",
            )
            service.log(e)
        }
    }
}
