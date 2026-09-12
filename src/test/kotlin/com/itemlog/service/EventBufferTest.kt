package com.itemlog.service

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.repository.ItemEventRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventBufferTest {

    private fun makeBuffer(repo: ItemEventRepository, batchSize: Int = 2): Pair<EventBuffer, JavaPlugin> {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val server = mockk<org.bukkit.Server>(relaxed = true)
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        every { scheduler.runTaskAsynchronously(any<JavaPlugin>(), any<Runnable>()) } answers { mockk(relaxed = true) }
        every { scheduler.runTaskTimerAsynchronously(any<JavaPlugin>(), any<Runnable>(), any<Long>(), any<Long>()) } returns mockk(relaxed = true) {
            every { taskId } returns 1
        }
        every { scheduler.cancelTask(any<Int>()) } returns Unit
        every { plugin.logger } returns mockk(relaxed = true)
        val buffer = EventBuffer(plugin, repo, batchSize = batchSize, flushIntervalTicks = 20)
        return buffer to plugin
    }

    private fun event(): ItemEvent = ItemEvent(UUID.randomUUID(), EventType.PICKUP, System.currentTimeMillis(), UUID.randomUUID(), null, ItemSnapshot("DIAMOND", 1, null), null, null)

    @Test
    fun `add increases size and flush clears`() {
        val repo = mockk<ItemEventRepository>(relaxed = true)
        every { repo.insertBatch(any()) } returns Unit
        val (buffer, _) = makeBuffer(repo, batchSize = 10)
        assertEquals(0, buffer.size())
        buffer.add(event())
        buffer.add(event())
        assertEquals(2, buffer.size())
        buffer.flush()
        assertEquals(0, buffer.size())
        verify { repo.insertBatch(any()) }
    }

    @Test
    fun `flush requeues on failure`() {
        val repo = mockk<ItemEventRepository>(relaxed = true)
        every { repo.insertBatch(any()) } throws RuntimeException("db fail")
        val (buffer, _) = makeBuffer(repo, batchSize = 10)
        buffer.add(event())
        buffer.flush()
        // after failure, event should be re-queued
        assertEquals(1, buffer.size())
    }

    @Test
    fun `add triggers async when batch full`() {
        val repo = mockk<ItemEventRepository>(relaxed = true)
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val server = mockk<org.bukkit.Server>(relaxed = true)
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        every { scheduler.runTaskAsynchronously(any<JavaPlugin>(), any<Runnable>()) } returns mockk(relaxed = true)
        every { scheduler.runTaskTimerAsynchronously(any<JavaPlugin>(), any<Runnable>(), any<Long>(), any<Long>()) } returns mockk(relaxed = true) { every { taskId } returns 1 }
        every { plugin.logger } returns mockk(relaxed = true)
        val buffer = EventBuffer(plugin, repo, batchSize = 2)
        buffer.add(event())
        // second add should trigger async
        buffer.add(event())
        // verify async was called
        verify { scheduler.runTaskAsynchronously(any<JavaPlugin>(), any<Runnable>()) }
    }
}
