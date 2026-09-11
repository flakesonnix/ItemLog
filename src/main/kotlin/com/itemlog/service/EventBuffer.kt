package com.itemlog.service

import com.itemlog.model.ItemEvent
import com.itemlog.repository.ItemEventRepository
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import org.bukkit.plugin.java.JavaPlugin

class EventBuffer(
    private val plugin: JavaPlugin,
    private val repository: ItemEventRepository,
    private val batchSize: Int = 100,
    // 1s
    private val flushIntervalTicks: Long = 20L,
) {
    private val queue = ConcurrentLinkedQueue<ItemEvent>()
    private val flushing = AtomicBoolean(false)
    private var taskId: Int = -1

    fun start() {
        taskId = plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable { flush() }, flushIntervalTicks, flushIntervalTicks).taskId
    }

    fun stop() {
        if (taskId != -1) plugin.server.scheduler.cancelTask(taskId)
        flush() // final flush
    }

    fun add(event: ItemEvent) {
        queue.add(event)
        if (queue.size >= batchSize) {
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { flush() })
        }
    }

    fun flush() {
        if (!flushing.compareAndSet(false, true)) return
        try {
            val batch = mutableListOf<ItemEvent>()
            while (batch.size < batchSize) {
                val e = queue.poll() ?: break
                // deduplication: skip if same eventId already in batch (should not happen, eventId is UUID)
                batch.add(e)
            }
            if (batch.isNotEmpty()) {
                try {
                    repository.insertBatch(batch)
                } catch (e: Exception) {
                    plugin.logger.log(Level.SEVERE, "Failed to flush ${batch.size} events", e)
                    // re-queue on failure (avoid loss, but risk duplicates)
                    batch.forEach { queue.offer(it) }
                }
            }
        } finally {
            flushing.set(false)
        }
    }

    fun size(): Int = queue.size
}
