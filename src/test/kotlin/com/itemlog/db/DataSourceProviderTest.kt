package com.itemlog.db

import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataSourceProviderTest {

    @TempDir lateinit var tempDir: File

    @Test
    fun `sqlite datasource connects`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "test.db")
        c.set("database.pool.maximum-pool-size", 1)
        every { plugin.config } returns c
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        val provider = DataSourceProvider(plugin)
        val ds = provider.getDataSource()
        assertNotNull(ds)
        ds.connection.use { conn ->
            conn.createStatement().use { st ->
                st.execute("SELECT 1").let { assertTrue(true) }
            }
        }
        provider.close()
    }

    @Test
    fun `same datasource returned when not closed`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "test2.db")
        every { plugin.config } returns c
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        val provider = DataSourceProvider(plugin)
        val ds1 = provider.getDataSource()
        val ds2 = provider.getDataSource()
        assertTrue(ds1 === ds2)
        provider.close()
    }
}
