package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.EventDeduplicator
import com.itemlog.service.ItemLogService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.ItemSpawnEvent
import java.util.UUID

class BlockItemListener(
    private val service: ItemLogService,
    private val serializer: ItemSerializer,
    private val deduplicator: EventDeduplicator
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockDrop(event: BlockDropItemEvent) {
        val player = event.player
        val loc = LocationData.from(event.block.location)
        for (entity in event.items) {
            val item = entity.itemStack.clone()
            val key = deduplicator.key(player.uniqueId.toString(), "BLOCK_DROP", item.type.name, item.amount)
            if (deduplicator.isDuplicate(key)) continue
            val snapshot = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
            val e = ItemEvent(
                eventId = UUID.randomUUID(),
                type = EventType.DESTROY,
                timestamp = System.currentTimeMillis(),
                playerId = player.uniqueId,
                location = loc,
                before = snapshot,
                after = null,
                source = "BLOCK_BREAK:${event.block.type.name}"
            )
            service.log(e)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemSpawn(event: ItemSpawnEvent) {
        // Item spawned in world (e.g. block break without player, dispenser)
        // Only log if not already logged via PlayerDrop/BlockDrop (dedup)
        val item = event.entity.itemStack.clone()
        if (item.type.isAir) return
        val loc = LocationData.from(event.location)
        val key = deduplicator.key(null, "SPAWN", item.type.name, item.amount)
        if (deduplicator.isDuplicate(key)) return
        val snapshot = ItemSnapshot(item.type.name, item.amount, serializer.serialize(item))
        val e = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.OTHER,
            timestamp = System.currentTimeMillis(),
            playerId = null,
            location = loc,
            before = null,
            after = snapshot,
            source = "ITEM_SPAWN"
        )
        service.log(e)
    }
}
