package com.itemlog.listener

import com.itemlog.service.ItemLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DropListenerTest {

    private lateinit var service: ItemLogService
    private lateinit var listener: DropListener

    @BeforeEach
    fun setup() {
        service = mockk(relaxed = true)
        listener = DropListener(service)
    }

    @Test
    fun `onDrop logs player drop`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns playerId
        every { player.location } returns Location(world, 15.0, 70.0, 25.0)

        val itemDrop = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.IRON_SWORD
        every { itemStack.amount } returns 1
        every { itemDrop.itemStack } returns itemStack

        val event = PlayerDropItemEvent(player, itemDrop)

        listener.onDrop(event)

        verify { service.logDrop(playerId, player.location, any()) }
    }

    @Test
    fun `onDrop clones item stack`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        val itemDrop = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        val clonedStack = mockk<ItemStack>(relaxed = true)
        every { itemDrop.itemStack } returns itemStack
        every { itemStack.clone() } returns clonedStack

        val event = PlayerDropItemEvent(player, itemDrop)

        listener.onDrop(event)

        verify { itemStack.clone() }
        verify { service.logDrop(any(), any(), clonedStack) }
    }

    @Test
    fun `onDrop handles multiple drops in sequence`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        for (i in 1..3) {
            val itemDrop = mockk<Item>(relaxed = true)
            val itemStack = mockk<ItemStack>(relaxed = true)
            every { itemStack.type } returns Material.DIAMOND
            every { itemStack.amount } returns i
            every { itemDrop.itemStack } returns itemStack
            
            val event = PlayerDropItemEvent(player, itemDrop)
            listener.onDrop(event)
        }

        verify(exactly = 3) { service.logDrop(any(), any(), any()) }
    }
}
