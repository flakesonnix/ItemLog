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
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class PickupListenerTest {

    private lateinit var service: ItemLogService
    private lateinit var listener: PickupListener

    @BeforeEach
    fun setup() {
        service = mockk(relaxed = true)
        listener = PickupListener(service)
    }

    @Test
    fun `onPickup logs player pickup`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns playerId
        every { player.location } returns Location(world, 10.0, 64.0, 20.0)

        val itemEntity = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.DIAMOND
        every { itemStack.amount } returns 3
        every { itemEntity.itemStack } returns itemStack

        val event = EntityPickupItemEvent(player, itemEntity, 0)

        listener.onPickup(event)

        verify { service.logPickup(playerId, player.location, any()) }
    }

    @Test
    fun `onPickup ignores non-player entities`() {
        val zombie = mockk<Zombie>(relaxed = true)
        val itemEntity = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        every { itemStack.type } returns Material.ROTTEN_FLESH
        every { itemStack.amount } returns 1
        every { itemEntity.itemStack } returns itemStack

        val event = EntityPickupItemEvent(zombie, itemEntity, 0)

        listener.onPickup(event)

        verify(exactly = 0) { service.logPickup(any(), any(), any()) }
    }

    @Test
    fun `onPickup clones item stack`() {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        every { world.name } returns "world"
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.location } returns Location(world, 0.0, 0.0, 0.0)

        val itemEntity = mockk<Item>(relaxed = true)
        val itemStack = mockk<ItemStack>(relaxed = true)
        val clonedStack = mockk<ItemStack>(relaxed = true)
        every { itemEntity.itemStack } returns itemStack
        every { itemStack.clone() } returns clonedStack

        val event = EntityPickupItemEvent(player, itemEntity, 0)

        listener.onPickup(event)

        verify { itemStack.clone() }
        verify { service.logPickup(any(), any(), clonedStack) }
    }
}
