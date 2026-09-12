package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.ItemLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeathListenerTest {

    private lateinit var service: ItemLogService
    private lateinit var serializer: ItemSerializer
    private lateinit var listener: DeathListener

    @BeforeEach
    fun setup() {
        service = mockk(relaxed = true)
        serializer = mockk(relaxed = true)
        
        every { serializer.serialize(any()) } returns """{"type":"DIAMOND"}"""
        
        listener = DeathListener(service, serializer)
    }

    @Test
    fun `onDeath logs dropped items`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns playerId
        every { player.location } returns Location(world, 10.0, 64.0, 20.0)

        val drops = listOf(
            mockk<ItemStack>(relaxed = true).apply {
                val mat = mockk<Material>(relaxed = true)
                every { mat.name } returns "DIAMOND_SWORD"
                every { mat.isAir } returns false
                every { type } returns mat
                every { amount } returns 1
            },
            mockk<ItemStack>(relaxed = true).apply {
                val mat = mockk<Material>(relaxed = true)
                every { mat.name } returns "DIAMOND"
                every { mat.isAir } returns false
                every { type } returns mat
                every { amount } returns 10
            }
        )

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns drops
        every { event.itemsToKeep } returns emptyList()

        listener.onDeath(event)

        verify(exactly = 2) { service.log(match { 
            it.type == EventType.DEATH_DROP &&
            it.source == "DEATH" &&
            it.playerId == playerId &&
            it.before != null &&
            it.after == null
        }) }
    }

    @Test
    fun `onDeath logs kept items with keepInventory`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns playerId
        every { player.location } returns Location(world, 10.0, 64.0, 20.0)

        val kept = listOf(
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.NETHERITE_SWORD
                every { amount } returns 1
            },
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.GOLDEN_APPLE
                every { amount } returns 5
            }
        )

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns emptyList()
        every { event.itemsToKeep } returns kept

        listener.onDeath(event)

        verify(exactly = 2) { service.log(match { 
            it.type == EventType.DEATH_KEEP &&
            it.source == "DEATH_KEEP" &&
            it.playerId == playerId &&
            it.before != null &&
            it.after != null
        }) }
    }

    @Test
    fun `onDeath logs both drops and kept items`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        val drops = listOf(mockk<ItemStack>(relaxed = true).apply {
            every { type } returns Material.DIAMOND
            every { amount } returns 5
        })
        val kept = listOf(mockk<ItemStack>(relaxed = true).apply {
            every { type } returns Material.EMERALD
            every { amount } returns 3
        })

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns drops
        every { event.itemsToKeep } returns kept

        listener.onDeath(event)

        verify(exactly = 1) { service.log(match { it.type == EventType.DEATH_DROP }) }
        verify(exactly = 1) { service.log(match { it.type == EventType.DEATH_KEEP }) }
    }

    @Test
    fun `onDeath skips air items in drops`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        val drops = listOf(
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.DIAMOND
                every { amount } returns 1
            },
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.AIR
                every { amount } returns 0
            },
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.GOLD_INGOT
                every { amount } returns 2
            }
        )

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns drops
        every { event.itemsToKeep } returns emptyList()

        listener.onDeath(event)

        verify(exactly = 2) { service.log(any()) }
    }

    @Test
    fun `onDeath skips air items in kept items`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        val kept = listOf(
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.NETHERITE_PICKAXE
                every { amount } returns 1
            },
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.AIR
                every { amount } returns 0
            }
        )

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns emptyList()
        every { event.itemsToKeep } returns kept

        listener.onDeath(event)

        verify(exactly = 1) { service.log(any()) }
    }

    @Test
    fun `onDeath uses same timestamp for all items`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        val drops = listOf(
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.DIAMOND
                every { amount } returns 1
            },
            mockk<ItemStack>(relaxed = true).apply {
                every { type } returns Material.GOLD_INGOT
                every { amount } returns 2
            }
        )

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns drops
        every { event.itemsToKeep } returns emptyList()

        val startTime = System.currentTimeMillis()
        listener.onDeath(event)
        val endTime = System.currentTimeMillis()

        // All events should have been logged with same timestamp within test duration
        verify(exactly = 2) { service.log(match { 
            it.timestamp in startTime..endTime
        }) }
    }

    @Test
    fun `onDeath includes location data`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "nether"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 123.5, 64.0, -456.7)

        val drops = listOf(mockk<ItemStack>(relaxed = true).apply {
            every { type } returns Material.DIAMOND
            every { amount } returns 1
        })

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns drops
        every { event.itemsToKeep } returns emptyList()

        listener.onDeath(event)

        verify { service.log(match { 
            it.location?.world == "nether" &&
            it.location?.x == 123.5 &&
            it.location?.y == 64.0 &&
            it.location?.z == -456.7
        }) }
    }

    @Test
    fun `onDeath serializes items`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        val item1 = mockk<ItemStack>(relaxed = true).apply {
            every { type } returns Material.DIAMOND_SWORD
            every { amount } returns 1
        }
        val item2 = mockk<ItemStack>(relaxed = true).apply {
            every { type } returns Material.GOLDEN_APPLE
            every { amount } returns 5
        }

        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        every { event.drops } returns listOf(item1)
        every { event.itemsToKeep } returns listOf(item2)

        listener.onDeath(event)

        verify { serializer.serialize(item1) }
        verify { serializer.serialize(item2) }
    }
}
