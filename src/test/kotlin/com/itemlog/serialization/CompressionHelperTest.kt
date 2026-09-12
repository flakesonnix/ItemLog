package com.itemlog.serialization

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CompressionHelperTest {

    @Test
    fun `compress and decompress should work correctly`() {
        // Large enough + repetitive enough that gzip clearly wins over the 20% threshold
        val lore = (1..30).joinToString(",") { """"Lore line number $it with some repeated flavor text"""" }
        val enchants = (1..10).joinToString(",") { """{"key":"sharpness","level":5}""" }
        val original = """{"type":"DIAMOND_SWORD","amount":1,"meta":{"displayName":"Epic Sword","lore":[$lore],"enchantments":[$enchants]}}"""

        val compressed = CompressionHelper.compress(original)
        assertNotNull(compressed)
        assertTrue(compressed!!.startsWith("gzip:"))

        val decompressed = CompressionHelper.decompress(compressed)
        assertEquals(original, decompressed)
    }

    @Test
    fun `should not compress small data`() {
        val small = "small"
        val result = CompressionHelper.compress(small)
        assertEquals(small, result) // Should return original
    }

    @Test
    fun `should handle null and blank input`() {
        assertNull(CompressionHelper.compress(null))
        assertNull(CompressionHelper.compress(""))
        assertNull(CompressionHelper.compress("   "))

        assertNull(CompressionHelper.decompress(null))
        assertNull(CompressionHelper.decompress(""))
    }

    @Test
    fun `should decompress plain JSON without gzip prefix`() {
        val plain = """{"test":"value"}"""
        val result = CompressionHelper.decompress(plain)
        assertEquals(plain, result)
    }

    @Test
    fun `compression should save space for large JSON`() {
        val largeJson = buildString {
            append("""{"items":[""")
            repeat(100) {
                append("""{"name":"item$it","value":$it},""")
            }
            append("""]}""")
        }

        val compressed = CompressionHelper.compress(largeJson)
        assertNotNull(compressed)
        assertTrue(compressed!!.startsWith("gzip:"))

        val originalSize = largeJson.toByteArray(Charsets.UTF_8).size
        val compressedSize = java.util.Base64.getDecoder().decode(compressed.substring(5)).size

        // Should save at least 20%
        assertTrue(compressedSize < originalSize * 0.8, "Compression should save space: original=$originalSize, compressed=$compressedSize")
    }

    @Test
    fun `compression ratio should be calculated correctly`() {
        val original = "a".repeat(1000)
        val compressed = CompressionHelper.compress(original)!!

        val ratio = CompressionHelper.compressionRatio(original, compressed)
        assertTrue(ratio < 0.5, "Should compress very well for repetitive data")
    }

    @Test
    fun `isCompressed should detect compressed data`() {
        val original = "x".repeat(200)
        val compressed = CompressionHelper.compress(original)

        assertTrue(CompressionHelper.isCompressed(compressed))
        assertFalse(CompressionHelper.isCompressed(original))
        assertFalse(CompressionHelper.isCompressed(null))
    }
}
