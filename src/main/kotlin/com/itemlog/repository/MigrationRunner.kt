package com.itemlog.repository

import javax.sql.DataSource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.stream.Collectors

class MigrationRunner(private val ds: DataSource) {

    fun migrate() {
        try (val c = ds.connection) {
            // ensure migrations table exists (for fresh DB)
            c.createStatement().use { s ->
                s.execute("CREATE TABLE IF NOT EXISTS schema_migrations (version INT PRIMARY KEY, applied_at BIGINT NOT NULL)")
            }
            val current = currentVersion(c)
            if (current >= 1) return

            // load V1
            val `in` = javaClass.getResourceAsStream("/db/migrations/V1__initial.sql")
                ?: throw IllegalStateException("V1__initial.sql not found")
            val sql = BufferedReader(InputStreamReader(`in`, StandardCharsets.UTF_8)).use { r ->
                r.lines().collect(Collectors.joining("\n"))
            }

            // split by ; and execute in one transaction
            c.autoCommit = false
            try {
                c.createStatement().use { s ->
                    for (stmt in sql.split(";")) {
                        val t = stmt.trim()
                        if (t.isNotEmpty()) s.execute(t)
                    }
                    c.prepareStatement("INSERT INTO schema_migrations (version, applied_at) VALUES (1, ?)").use { ps ->
                        ps.setLong(1, System.currentTimeMillis())
                        ps.executeUpdate()
                    }
                }
                c.commit()
            } catch (e: Exception) {
                c.rollback()
                throw e
            } finally {
                c.autoCommit = true
            }
        }
    }

    private fun currentVersion(c: Connection): Int {
        return c.createStatement().use { s ->
            s.executeQuery("SELECT MAX(version) FROM schema_migrations").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}