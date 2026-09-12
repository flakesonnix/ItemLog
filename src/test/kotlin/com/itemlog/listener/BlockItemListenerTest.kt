package com.itemlog.listener

import com.itemlog.model.EventType
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.EventDeduplicator
import com.itemlog.service.ItemLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class BlockItemListenerTest {

    private lateinit var service: ItemLogService
    private lateinit var serializer: ItemSerializer
    private lateinit var deduplicator: EventDeduplicator
    private lateinit var listener: BlockItemListener

    @BeforeEach
    fun setup() {
        service = mockk(relaxed = true)
        serializer = mockk(relaxed = true)
        deduplicator = mockk(relaxed = true)
        
        every { serializer.serialize(any()) } returns """{"type":"DIAMOND"}"""
        every { deduplicator.isDuplicate(any()) } returns false
        
        listener = BlockItemListener(service, serializer, deduplicator)
    }

    @Test
    fun `onBlockDrop logs block break events`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        every { player.uniqueId } returns playerId

        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        
        val block = mockk<Block>(relaxed = true)
        every { block.type } returns Material.DIAMOND_ORE
        every { block.location } returns Location(world, 100.0, 64.0, 200.0)

        val itemEntity = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.DIAMOND
        every { itemStack.amount } returns 2
        every { itemEntity.itemStack } returns itemStack

        val event = mockk<BlockDropItemEvent>(relaxed = true)
        every { event.player } returns player
        every { event.block } returns block
        every { event.items } returns listOf(itemEntity)

        listener.onBlockDrop(event)

        verify { service.log(match { 
            it.type == EventType.DESTROY && 
            it.playerId == playerId &&
            it.source?.contains("BLOCK_BREAK:DIAMOND_ORE") == true
        }) }
    }

    @Test
    fun `onBlockDrop handles multiple item drops from same block`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        
        val block = mockk<Block>(relaxed = true)
        every { block.type } returns Material.CHEST
        every { block.location } returns Location(world, 0.0, 0.0, 0.0)

        val items = (1..3).map {
            mockk<Item>(relaxed = true).apply {
                val stack = mockk<ItemStack>(relaxed = true)
                every { stack.type } returns Material.DIAMOND
                every { stack.amount } returns it
                every { itemStack } returns stack
            }
        }

        val event = mockk<BlockDropItemEvent>(relaxed = true)
        every { event.player } returns player
        every { event.block } returns block
        every { event.items } returns items

        listener.onBlockDrop(event)

        verify(exactly = 3) { service.log(any()) }
    }

    @Test
    fun `onBlockDrop skips duplicates`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        
        val block = mockk<Block>(relaxed = true)
        every { block.type } returns Material.STONE
        every { block.location } returns Location(world, 0.0, 0.0, 0.0)

        val itemEntity = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.COBBLESTONE
        every { itemStack.amount } returns 1
        every { itemEntity.itemStack } returns itemStack

        every { deduplicator.isDuplicate(any()) } returns true

        val event = mockk<BlockDropItemEvent>(relaxed = true)
        every { event.player } returns player
        every { event.block } returns block
        every { event.items } returns listOf(itemEntity)

        listener.onBlockDrop(event)

        verify(exactly = 0) { service.log(any()) }
    }

    @Test
    fun `onBlockDrop creates correct deduplication key`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        every { player.uniqueId } returns playerId

        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        
        val block = mockk<Block>(relaxed = true)
        every { block.type } returns Material.COAL_ORE
        every { block.location } returns Location(world, 0.0, 0.0, 0.0)

        val itemEntity = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.COAL
        every { itemStack.amount } returns 5
        every { itemEntity.itemStack } returns itemStack

        val event = mockk<BlockDropItemEvent>(relaxed = true)
        every { event.player } returns player
        every { event.block } returns block
        every { event.items } returns listOf(itemEntity)

        listener.onBlockDrop(event)

        verify { deduplicator.key(playerId.toString(), "BLOCK_DROP", "COAL", 5) }
    }

    @Test
    fun `onItemSpawn logs world spawned items`() {
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 50.0, 70.0, 100.0)

        val itemEntity = mockk<org.bukkit.entity.Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.APPLE
        every { itemStack.amount } returns 1
        every { itemEntity.itemStack } returns itemStack
        every { itemEntity.location } returns location

        val event = mockk<ItemSpawnEvent>(relaxed = true)
        every { event.entity } returns itemEntity
        every { event.location } returns location

        listener.onItemSpawn(event)

        verify { service.log(match { 
            it.type == EventType.OTHER &&
            it.source == "ITEM_SPAWN" &&
            it.playerId == null
        }) }
    }

    @Test
    fun `onItemSpawn skips air items`() {
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 0.0, 0.0, 0.0)

        val itemEntity = mockk<org.bukkit.entity.Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.AIR
        every { itemStack.amount } returns 0
        every { itemEntity.itemStack } returns itemStack
        every { itemEntity.location } returns location

        val event = mockk<ItemSpawnEvent>(relaxed = true)
        every { event.entity } returns itemEntity
        every { event.location } returns location

        listener.onItemSpawn(event)

        verify(exactly = 0) { service.log(any()) }
    }

    @Test
    fun `onItemSpawn skips duplicates`() {
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 0.0, 0.0, 0.0)

        val itemEntity = mockk<org.bukkit.entity.Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.STICK
        every { itemStack.amount } returns 1
        every { itemEntity.itemStack } returns itemStack
        every { itemEntity.location } returns location

        every { deduplicator.isDuplicate(any()) } returns true

        val event = mockk<ItemSpawnEvent>(relaxed = true)
        every { event.entity } returns itemEntity
        every { event.location } returns location

        listener.onItemSpawn(event)

        verify(exactly = 0) { service.log(any()) }
    }

    @Test
    fun `onItemSpawn creates correct deduplication key for spawns`() {
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        val location = Location(world, 0.0, 0.0, 0.0)

        val itemEntity = mockk<org.bukkit.entity.Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.EMERALD
        every { itemStack.amount } returns 3
        every { itemEntity.itemStack } returns itemStack
        every { itemEntity.location } returns location

        val event = mockk<ItemSpawnEvent>(relaxed = true)
        every { event.entity } returns itemEntity
        every { event.location } returns location

        listener.onItemSpawn(event)

        verify { deduplicator.key(null, "SPAWN", "EMERALD", 3) }
    }
}
