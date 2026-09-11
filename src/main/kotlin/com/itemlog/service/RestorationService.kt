package com.itemlog.service

import com.itemlog.model.LocationData
import com.itemlog.model.Restoration
import com.itemlog.repository.ItemEventRepository
import com.itemlog.repository.RestorationRepository
import com.itemlog.serialization.ItemSerializer
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class RestorationService(
    private val plugin: JavaPlugin,
    private val eventRepo: ItemEventRepository,
    private val restorationRepo: RestorationRepository,
    private val serializer: ItemSerializer
) {

    sealed class Result {
        data class Success(val restorationId: UUID, val itemGiven: Boolean) : Result()
        data class AlreadyRestored(val restorations: List<Restoration>) : Result()
        data class NotFound(val eventId: UUID) : Result()
        data class Failed(val reason: String) : Result()
    }

    fun restore(eventId: UUID, admin: Player, target: Player? = null): Result {
        val event = eventRepo.findById(eventId) ?: return Result.NotFound(eventId)
        if (restorationRepo.exists(eventId)) {
            return Result.AlreadyRestored(restorationRepo.findByEventId(eventId))
        }
        val targetPlayer = target ?: admin.server.getPlayer(event.playerId ?: return Result.Failed("no player")) ?: admin
        val snapshot = event.before ?: event.after ?: return Result.Failed("no snapshot")
        val item = try {
            serializer.deserialize(snapshot.itemJson) ?: return Result.Failed("deserialize failed")
        } catch (e: Exception) {
            return Result.Failed("deserialize: ${e.message}")
        }
        item.amount = snapshot.amount

        // must run on main thread for inventory
        val given = try {
            val loc: Location? = targetPlayer.location
            // try inventory first
            val leftover = targetPlayer.inventory.addItem(item)
            val success = leftover.isEmpty()
            if (!success && loc != null) {
                // drop at location if inventory full
                loc.world?.dropItemNaturally(loc, item)
            }
            success
        } catch (e: Exception) {
            return Result.Failed(e.message ?: "inventory failed")
        }

        val locData = targetPlayer.location.let { LocationData.from(it) }
        val restoration = Restoration(
            restorationId = UUID.randomUUID(),
            eventId = eventId,
            adminId = admin.uniqueId,
            targetId = targetPlayer.uniqueId,
            timestamp = System.currentTimeMillis(),
            restoreLocation = locData,
            resultJson = snapshot.itemJson,
            status = if (given) "SUCCESS" else "PARTIAL"
        )
        try {
            restorationRepo.insert(restoration)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to log restoration: ${e.message}")
        }
        return Result.Success(restoration.restorationId, given)
    }
}
