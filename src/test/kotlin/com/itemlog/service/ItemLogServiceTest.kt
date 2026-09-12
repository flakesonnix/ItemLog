package com.itemlog.service

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.repository.ItemEventRepository
import com.itemlog.serialization.ItemSerializer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ItemLogServiceTest {

    private lateinit var plugin: JavaPlugin
    private lateinit var serializer: ItemSerializer
    private lateinit var repository: ItemEventRepository
    private lateinit var service: ItemLogService

    @BeforeEach
    fun setup() {
        plugin = mockk(relaxed = true)
        serializer = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        
        val server = mockk<Server>(relaxed = true)
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        every { scheduler.runTaskAsynchronously(any<JavaPlugin>(), any<Runnable>()) } returns mockk(relaxed = true)
        every { scheduler.runTaskTimerAsynchronously(any<JavaPlugin>(), any<Runnable>(), any<Long>(), any<Long>()) } returns mockk(relaxed = true) {
            every { taskId } returns 1
        }
        every { plugin.logger } returns mockk(relaxed = true)
        every { serializer.serialize(any()) } returns """{"type":"DIAMOND","amount":1}"""
        
        service = ItemLogService(plugin, serializer, repository)
    }

    @Test
    fun `logPickup creates PICKUP event with correct data`() {
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 100.0, 64.0, 200.0)
        
        val item = mockk<ItemStack>(relaxed = true)
        every { item.type } returns Material.DIAMOND
        every { item.amount } returns 5
        
        var capturedEvents: List<ItemEvent>? = null
        every { repository.insertBatch(any()) } answers {
            capturedEvents = arg(0)
            Unit
        }

        service.logPickup(playerId, location, item)
        service.flush()

        verify { repository.insertBatch(any()) }
        assertNotNull(capturedEvents)
        val captured = capturedEvents!!.first()
        
        assertEquals(EventType.PICKUP, captured.type)
        assertEquals(playerId, captured.playerId)
        assertEquals("world", captured.location?.world)
        assertEquals(100.0, captured.location?.x ?: 0.0, 0.001)
        assertNull(captured.before)
        assertNotNull(captured.after)
        assertEquals("DIAMOND", captured.after?.material)
        assertEquals(5, captured.after?.amount)
        assertEquals("PLAYER_PICKUP", captured.source)
        assertTrue(captured.timestamp > 0)
    }

    @Test
    fun `logDrop creates DROP event with correct data`() {
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 50.0, 70.0, 150.0)
        
        val item = mockk<ItemStack>(relaxed = true)
        every { item.type } returns Material.GOLD_INGOT
        every { item.amount } returns 10
        
        var capturedEvents: List<ItemEvent>? = null
        every { repository.insertBatch(any()) } answers {
            capturedEvents = arg(0)
            Unit
        }

        service.logDrop(playerId, location, item)
        service.flush()

        verify { repository.insertBatch(any()) }
        assertNotNull(capturedEvents)
        val captured = capturedEvents!!.first()
        
        assertEquals(EventType.DROP, captured.type)
        assertEquals(playerId, captured.playerId)
        assertNotNull(captured.before)
        assertEquals("GOLD_INGOT", captured.before?.material)
        assertEquals(10, captured.before?.amount)
        assertNull(captured.after)
        assertEquals("PLAYER_DROP", captured.source)
    }

    @Test
    fun `log adds event to buffer`() {
        val event = ItemEvent(
            eventId = UUID.randomUUID(),
            type = EventType.PICKUP,
            timestamp = System.currentTimeMillis(),
            playerId = UUID.randomUUID(),
            location = null,
            before = null,
            after = mockk(relaxed = true),
            source = "TEST"
        )
        
        every { repository.insertBatch(any()) } returns Unit
        
        service.log(event)
        service.flush()
        
        verify { repository.insertBatch(match { it.contains(event) }) }
    }

    @Test
    fun `serializer is called when logging items`() {
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 0.0, 0.0, 0.0)
        
        val item = mockk<ItemStack>(relaxed = true)
        every { item.type } returns Material.DIAMOND_SWORD
        every { item.amount } returns 1
        
        every { repository.insertBatch(any()) } returns Unit
        
        service.logPickup(playerId, location, item)
        
        verify { serializer.serialize(item) }
    }

    @Test
    fun `multiple events are batched together`() {
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 0.0, 0.0, 0.0)
        
        val item1 = mockk<ItemStack>(relaxed = true)
        every { item1.type } returns Material.DIAMOND
        every { item1.amount } returns 1
        
        val item2 = mockk<ItemStack>(relaxed = true)
        every { item2.type } returns Material.GOLD_INGOT
        every { item2.amount } returns 2
        
        val item3 = mockk<ItemStack>(relaxed = true)
        every { item3.type } returns Material.IRON_INGOT
        every { item3.amount } returns 3
        
        every { repository.insertBatch(any()) } returns Unit
        
        service.logPickup(playerId, location, item1)
        service.logPickup(playerId, location, item2)
        service.logDrop(playerId, location, item3)
        
        service.flush()
        
        verify(exactly = 1) { repository.insertBatch(match { it.size == 3 }) }
    }

    @Test
    fun `start and stop manage buffer lifecycle`() {
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        val server = mockk<Server>(relaxed = true)
        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        
        val task = mockk<org.bukkit.scheduler.BukkitTask>(relaxed = true)
        every { task.taskId } returns 42
        every { scheduler.runTaskTimerAsynchronously(any<JavaPlugin>(), any<Runnable>(), any<Long>(), any<Long>()) } returns task
        every { scheduler.cancelTask(any()) } returns Unit
        every { repository.insertBatch(any()) } returns Unit
        
        service.start()
        verify { scheduler.runTaskTimerAsynchronously(plugin, any<Runnable>(), any<Long>(), any<Long>()) }
        
        service.stop()
        verify { scheduler.cancelTask(42) }
    }
}
