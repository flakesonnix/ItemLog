package com.itemlog.service

import com.itemlog.repository.ItemEventRepository
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class RetentionService(
    private val plugin: JavaPlugin,
    private val repository: ItemEventRepository
) {
    // retention in days, 0 = keep forever
    fun cleanIfNeeded() {
        val days = plugin.config.getInt("retention.days", 30)
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                val deleted = repository.deleteBefore(cutoff)
                if (deleted > 0) plugin.logger.info("Retention: deleted $deleted events older than $days days")
            } catch (e: Exception) {
                plugin.logger.warning("Retention failed: ${e.message}")
            }
        })
    }

    fun schedule() {
        val enabled = plugin.config.getBoolean("retention.enabled", true)
        if (!enabled) return
        // run 1 hour after start, then every 24h
        plugin.server.scheduler.runTaskTimerAsynchronously(
            plugin,
            Runnable { cleanIfNeeded() },
            20L * 60 * 60,
            20L * 60 * 60 * 24
        )
    }
}
