package com.itemlog.service

import com.itemlog.repository.ItemEventRepository
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

/**
 * Manages automatic deletion of old events based on retention policy.
 * Runs periodically to keep database size manageable.
 */
class RetentionService(
    private val plugin: JavaPlugin,
    private val repository: ItemEventRepository,
) {
    private var task: BukkitTask? = null
    private var enabled = false
    private var retentionDays = 30
    private var checkIntervalHours = 24

    fun start() {
        enabled = plugin.config.getBoolean("retention.enabled", false)
        retentionDays = plugin.config.getInt("retention.days", 30)
        checkIntervalHours = plugin.config.getInt("retention.check-interval-hours", 24)

        if (!enabled) {
            plugin.logger.info("Retention policy disabled")
            return
        }

        if (retentionDays <= 0) {
            plugin.logger.warning("Invalid retention.days: $retentionDays - disabling retention")
            return
        }

        // Convert hours to ticks (1 hour = 72000 ticks)
        val intervalTicks = checkIntervalHours * 72000L

        // Run first check after 1 minute, then periodically
        task = plugin.server.scheduler.runTaskTimerAsynchronously(
            plugin,
            Runnable { performCleanup() },
            1200L, // 1 minute delay
            intervalTicks,
        )

        plugin.logger.info("Retention policy enabled: delete events older than $retentionDays days (check every ${checkIntervalHours}h)")
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun performCleanup() {
        try {
            val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
            val deleted = repository.deleteBefore(cutoffTime)

            if (deleted > 0) {
                plugin.logger.info("Retention cleanup: deleted $deleted events older than $retentionDays days")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Retention cleanup failed: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Manual cleanup trigger (e.g., from command)
     */
    fun triggerCleanup(): Int {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        return repository.deleteBefore(cutoffTime)
    }
}
