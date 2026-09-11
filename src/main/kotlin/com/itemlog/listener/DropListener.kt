package com.itemlog.listener

import com.itemlog.service.ItemLogService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent

class DropListener(private val service: ItemLogService) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        val player = event.player
        val item = event.itemDrop.itemStack.clone()
        service.logDrop(player.uniqueId, player.location, item)
    }
}
