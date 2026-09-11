package com.itemlog.listener

import com.itemlog.service.ItemLogService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.entity.Player

class PickupListener(private val service: ItemLogService) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val item = event.item.itemStack.clone()
        // MONITOR: event already applied, log after state
        service.logPickup(entity.uniqueId, entity.location, item)
    }
}
