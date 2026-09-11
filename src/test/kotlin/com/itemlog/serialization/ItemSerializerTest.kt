package com.itemlog.serialization

import com.itemlog.model.ItemSnapshot
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemSerializerTest {

    private val serializer = ItemSerializer()

    @Test
    fun `serialize and deserialize simple item`() {
        val item = ItemStack(Material.DIAMOND_SWORD)
        val json = serializer.serialize(item)
        assertNotNull(json)
        val deserialized = serializer.deserialize(json!!)
        assertNotNull(deserialized)
        assertEquals(Material.DIAMOND_SWORD, deserialized.type)
        assertEquals(1, deserialized.amount)
    }

    @Test
    fun `serialize and deserialize item with enchantments`() {
        val item = ItemStack(Material.DIAMOND_SWORD)
        val meta = item.itemMeta!!
        meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true)
        meta.addEnchant(Enchantment.DURABILITY, 3, true)
        item.itemMeta = meta

        val json = serializer.serialize(item)
        assertNotNull(json)
        assertTrue(json!!.contains("DAMAGE_ALL"))
        assertTrue(json.contains("DURABILITY"))

        val deserialized = serializer.deserialize(json!!)
        assertNotNull(deserialized)
        val meta2 = deserialized.itemMeta!!
        assertEquals(5, meta2.getEnchantLevel(Enchantment.DAMAGE_ALL))
        assertEquals(3, meta2.getEnchantLevel(Enchantment.DURABILITY))
    }

    @Test
    fun `serialize and deserialize item with lore and display name`() {
        val item = ItemStack(Material.DIAMOND_SWORD)
        val meta = item.itemMeta!!
        meta.displayName = org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cExcalibur")
        meta.lore = listOf("&7Legendary sword", "&7Forged in fire")
        item.itemMeta = meta

        val json = serializer.serialize(item)
        assertNotNull(json)
        assertTrue(json!!.contains("Excalibur"))
        assertTrue(json.contains("Legendary sword"))

        val deserialized = serializer.deserialize(json!!)
        assertNotNull(deserialized)
        val meta2 = deserialized.itemMeta!!
        assertNotNull(meta2.displayName)
        assertNotNull(meta2.lore)
        assertEquals(2, meta2.lore!!.size)
    }

    @Test
    fun `serialize and deserialize item with custom model data`() {
        val item = ItemStack(Material.DIAMOND_SWORD)
        val meta = item.itemMeta!!
        meta.customModelData = 12345
        item.itemMeta = meta

        val json = serializer.serialize(item)
        assertNotNull(json)
        assertTrue(json!!.contains("12345"))

        val deserialized = serializer.deserialize(json!!)
        assertNotNull(deserialized)
        assertEquals(12345, deserialized.itemMeta?.customModelData)
    }

    @Test
    fun `serialize and deserialize item with damage`() {
        val item = ItemStack(Material.DIAMOND_SWORD)
        val meta = item.itemMeta!!
        (meta as org.bukkit.inventory.meta.Damageable).damage = 100
        item.itemMeta = meta

        val json = serializer.serialize(item)
        assertNotNull(json)
        assertTrue(json!!.contains("100"))

        val deserialized = serializer.deserialize(json!!)
        assertNotNull(deserialized)
        val meta2 = deserialized.itemMeta as org.bukkit.inventory.meta.Damageable
        assertEquals(100, meta2.damage)
    }

    @Test
    fun `serialize null returns null`() {
        assertNull(serializer.serialize(null))
    }

    @Test
    fun `serialize air returns null`() {
        val air = org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR)
        assertNull(serializer.serialize(air))
    }

    @Test
    fun `deserialize null returns null`() {
        assertNull(serializer.deserialize(null))
        assertNull(serializer.deserialize(""))
    }
}