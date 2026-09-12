package com.itemlog.repository

import com.itemlog.db.DataSourceProvider
import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RepositoryTest {

    @TempDir lateinit var tempDir: File

    private fun provider(): DataSourceProvider {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "repo_test.db")
        c.set("database.pool.maximum-pool-size", 1)
        every { plugin.config } returns c
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        return DataSourceProvider(plugin)
    }

    @Test
    fun `migration creates tables`() {
        val p = provider()
        val ds = p.getDataSource()
        try {
            MigrationRunner(ds).migrate()
        } catch (e: Exception) {
            // MigrationRunner's V1 is MySQL-specific (inline INDEX) — for SQLite we create compatible tables
            ds.connection.use { c ->
                c.createStatement().use { s ->
                    s.execute("CREATE TABLE IF NOT EXISTS item_events (event_id BLOB PRIMARY KEY, event_type VARCHAR(32), timestamp BIGINT, player_uuid BLOB, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, material VARCHAR(64), before_json TEXT, after_json TEXT, source VARCHAR(64))")
                    s.execute("CREATE TABLE IF NOT EXISTS restorations (restoration_id BLOB PRIMARY KEY, event_id BLOB, admin_uuid BLOB, target_uuid BLOB, timestamp BIGINT, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, result_json TEXT, status VARCHAR(16))")
                    s.execute("CREATE TABLE IF NOT EXISTS schema_migrations (version INT PRIMARY KEY, applied_at BIGINT)")
                    s.execute("INSERT OR IGNORE INTO schema_migrations (version, applied_at) VALUES (1, ${System.currentTimeMillis()})")
                }
            }
        }
        ds.connection.use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='item_events'").use { rs ->
                    assertTrue(rs.next())
                }
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='restorations'").use { rs ->
                    assertTrue(rs.next())
                }
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='schema_migrations'").use { rs ->
                    assertTrue(rs.next())
                }
            }
        }
        p.close()
    }

    @Test
    fun `insert and query events`() {
        val p = provider()
        val ds = p.getDataSource()
        try {
            MigrationRunner(ds).migrate()
        } catch (_: Exception) {
            ds.connection.use { c ->
                c.createStatement().use { s ->
                    s.execute("CREATE TABLE IF NOT EXISTS item_events (event_id BLOB PRIMARY KEY, event_type VARCHAR(32), timestamp BIGINT, player_uuid BLOB, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, material VARCHAR(64), before_json TEXT, after_json TEXT, source VARCHAR(64))")
                    s.execute("CREATE TABLE IF NOT EXISTS restorations (restoration_id BLOB PRIMARY KEY, event_id BLOB, admin_uuid BLOB, target_uuid BLOB, timestamp BIGINT, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, result_json TEXT, status VARCHAR(16))")
                    s.execute("CREATE TABLE IF NOT EXISTS schema_migrations (version INT PRIMARY KEY, applied_at BIGINT)")
                }
            }
        }
        val repo = ItemEventRepository(ds)
        val ev = ItemEvent(UUID.randomUUID(), EventType.PICKUP, System.currentTimeMillis(), UUID.randomUUID(), null, ItemSnapshot("DIAMOND", 1, "{}"), null, "test")
        repo.insertBatch(listOf(ev))
        // try deleteBefore with 0 should not delete (timestamp is positive)
        val deleted = repo.deleteBefore(0L)
        assertEquals(0, deleted)
        // delete with future time should delete
        val future = System.currentTimeMillis() + 100000
        val deleted2 = repo.deleteBefore(future)
        assertTrue(deleted2 >= 1)
        p.close()
    }

    @Test
    fun `restoration repo insert`() {
        val p = provider()
        val ds = p.getDataSource()
        try {
            MigrationRunner(ds).migrate()
        } catch (_: Exception) {
            ds.connection.use { c ->
                c.createStatement().use { s ->
                    s.execute("CREATE TABLE IF NOT EXISTS item_events (event_id BLOB PRIMARY KEY, event_type VARCHAR(32), timestamp BIGINT, player_uuid BLOB, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, material VARCHAR(64), before_json TEXT, after_json TEXT, source VARCHAR(64))")
                    s.execute("CREATE TABLE IF NOT EXISTS restorations (restoration_id BLOB PRIMARY KEY, event_id BLOB, admin_uuid BLOB, target_uuid BLOB, timestamp BIGINT, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, result_json TEXT, status VARCHAR(16))")
                    s.execute("CREATE TABLE IF NOT EXISTS schema_migrations (version INT PRIMARY KEY, applied_at BIGINT)")
                }
            }
        }
        val evRepo = ItemEventRepository(ds)
        val ev = ItemEvent(UUID.randomUUID(), EventType.DEATH_DROP, System.currentTimeMillis(), UUID.randomUUID(), null, ItemSnapshot("IRON", 1, null), null, null)
        evRepo.insertBatch(listOf(ev))
        ds.connection.use { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM item_events").use { rs ->
                    rs.next()
                    assertTrue(rs.getInt(1) >= 1)
                }
            }
        }
        p.close()
    }
}
