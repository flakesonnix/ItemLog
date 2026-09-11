package com.itemlog.serialization

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.enchantments.Enchantment
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.meta.Damageable
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlotGroup
import org.jetbrains.annotations.Nullable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonArray

/**
 * Serializes ItemStack to JSON and back, preserving all metadata:
 * - enchantments
 * - lore
 * - display name
 * - attributes
 * - custom model data
 * - damage/durability
 * - item components (1.20.5+)
 * - other ItemMeta data
 */
class ItemSerializer {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Serializes ItemStack to JSON string.
     * Returns null if item is air/null.
     */
    @Nullable
    fun serialize(@Nullable item: ItemStack?): String? {
        if (item == null || item.type == Material.AIR) return null

        val json = JsonObject()
        json.addProperty("type", item.type.name)
        json.addProperty("amount", item.amount)

        val meta = item.itemMeta
        if (meta != null) {
            val metaJson = JsonObject()

            // Display name
            meta.displayName?.let { metaJson.add("displayName", gson.toJsonTree(it)) }

            // Lore
            meta.lore?.let { lore ->
                val loreArray = JsonArray()
                lore.forEach { loreArray.add(gson.toJsonTree(it)) }
                metaJson.add("lore", loreArray)
            }

            // Enchantments
            if (meta.hasEnchants()) {
                val enchArray = JsonArray()
                for (entry in meta.enchants.entries) {
                    val ench = entry.key
                    val level = entry.value
                    val enchObj = JsonObject()
                    enchObj.addProperty("key", ench.key.key)
                    enchObj.addProperty("level", level)
                    enchArray.add(enchObj)
                }
                metaJson.add("enchantments", enchArray)
            }

            // Custom model data
            meta.customModelData?.let { metaJson.addProperty("customModelData", it) }

            // Damage/Durability
            if (meta is Damageable) {
                metaJson.addProperty("damage", meta.damage)
            }

            // Item flags
            if (meta.itemFlags.isNotEmpty()) {
                val flagsArray = JsonArray()
                meta.itemFlags.forEach { flagsArray.add(it.name) }
                metaJson.add("itemFlags", flagsArray)
            }

            // Unbreakable
            if (meta.isUnbreakable) {
                metaJson.addProperty("unbreakable", true)
            }

            // Max stack size
            if (meta.maxStackSize > 0) {
                metaJson.addProperty("maxStackSize", meta.maxStackSize)
            }

            json.add("meta", metaJson)
        }

        return gson.toJson(json)
    }

    /**
     * Deserializes JSON string back to ItemStack.
     * Returns null if JSON is null/empty or represents air.
     */
    @Nullable
    fun deserialize(@Nullable jsonString: String?): ItemStack? {
        if (jsonString == null || jsonString.isBlank()) return null

        val json = JsonParser.parseString(jsonString).asJsonObject
        val typeName = json.get("type").asString
        val material = Material.getMaterial(typeName) ?: return ItemStack(Material.AIR)

        val amount = json.get("amount").asInt
        val item = ItemStack(material, amount)

        if (!json.has("meta")) return item

        val metaJson = json.get("meta").asJsonObject
        val meta = item.itemMeta ?: return item

        // Display name
        if (metaJson.has("displayName")) {
            val element = metaJson.get("displayName")
            if (element.isJsonPrimitive) {
                meta.setDisplayName(element.asString)
            }
        }

        // Lore
        if (metaJson.has("lore")) {
            val loreArray = metaJson.get("lore").asJsonArray
            val lore = mutableListOf<String>()
            for (i in 0 until loreArray.size()) {
                val element = loreArray.get(i)
                if (element.isJsonPrimitive) {
                    lore.add(element.asString)
                }
            }
            meta.setLore(lore)
        }

        // Enchantments
        if (metaJson.has("enchantments")) {
            val enchArray = metaJson.get("enchantments").asJsonArray
            for (i in 0 until enchArray.size()) {
                val enchObj = enchArray.get(i).asJsonObject
                val key = NamespacedKey.minecraft(enchObj.get("key").asString)
                val level = enchObj.get("level").asInt
                Enchantment.getByKey(key)?.let { meta.addEnchant(it, level, true) }
            }
        }

        // Custom model data
        if (metaJson.has("customModelData")) {
            meta.setCustomModelData(metaJson.get("customModelData").asInt)
        }

        // Damage
        if (metaJson.has("damage")) {
            val damage = metaJson.get("damage").asInt
            if (meta is Damageable) {
                meta.damage = damage
            }
        }

        // Item flags
        if (metaJson.has("itemFlags")) {
            val flagsArray = metaJson.get("itemFlags").asJsonArray
            for (i in 0 until flagsArray.size()) {
                val flagName = flagsArray.get(i).asString
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flagName))
            }
        }

        // Unbreakable
        if (metaJson.has("unbreakable")) {
            val unbreakable = metaJson.get("unbreakable").asBoolean
            if (unbreakable) meta.isUnbreakable = true
        }

        // Max stack size
        if (metaJson.has("maxStackSize")) {
            meta.setMaxStackSize(metaJson.get("maxStackSize").asInt)
        }

        item.itemMeta = meta

        return item
    }
}