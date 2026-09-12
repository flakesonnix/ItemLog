package com.itemlog.serialization

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Helper for compressing JSON data to reduce database size.
 * Uses Gzip compression with Base64 encoding for TEXT column compatibility.
 * Format: "gzip:BASE64_DATA" for compressed data, plain JSON otherwise.
 */
object CompressionHelper {

    private const val COMPRESSION_PREFIX = "gzip:"
    private const val MIN_SIZE_FOR_COMPRESSION = 100 // bytes
    private const val MIN_COMPRESSION_RATIO = 0.8 // Only use if saves ≥20%

    /**
     * Compresses JSON string using Gzip and encodes as Base64.
     * Returns: "gzip:BASE64_DATA" if compression is beneficial, otherwise original JSON.
     * Returns null if input is null/blank.
     */
    fun compress(json: String?): String? {
        if (json.isNullOrBlank()) return null

        val bytes = json.toByteArray(Charsets.UTF_8)

        // Skip compression for small data - overhead not worth it
        if (bytes.size < MIN_SIZE_FOR_COMPRESSION) return json

        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(bytes)
        }
        val compressed = baos.toByteArray()

        // Only use compression if it saves at least 20%
        return if (compressed.size < bytes.size * MIN_COMPRESSION_RATIO) {
            COMPRESSION_PREFIX + Base64.getEncoder().encodeToString(compressed)
        } else {
            json // Return original if compression not beneficial
        }
    }

    /**
     * Decompresses "gzip:BASE64_DATA" or returns plain JSON as-is.
     * This allows backward compatibility with existing uncompressed data.
     */
    fun decompress(data: String?): String? {
        if (data.isNullOrBlank()) return null
        if (!data.startsWith(COMPRESSION_PREFIX)) return data // Plain JSON

        try {
            val base64 = data.substring(COMPRESSION_PREFIX.length)
            val compressed = Base64.getDecoder().decode(base64)

            val bais = ByteArrayInputStream(compressed)
            val baos = ByteArrayOutputStream()
            GZIPInputStream(bais).use { gzip ->
                gzip.copyTo(baos)
            }
            return baos.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            // If decompression fails, try to return original (might be corrupted)
            return null
        }
    }

    /**
     * Calculates compression ratio for monitoring/stats.
     * Returns value between 0.0 (perfect compression) and >1.0 (expansion).
     */
    fun compressionRatio(original: String, compressed: String): Double {
        if (!compressed.startsWith(COMPRESSION_PREFIX)) return 1.0

        val originalSize = original.toByteArray(Charsets.UTF_8).size
        val base64 = compressed.substring(COMPRESSION_PREFIX.length)
        val compressedSize = Base64.getDecoder().decode(base64).size

        return compressedSize.toDouble() / originalSize
    }

    /**
     * Check if data is compressed
     */
    fun isCompressed(data: String?): Boolean = data?.startsWith(COMPRESSION_PREFIX) == true
}
