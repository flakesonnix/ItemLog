package com.itemlog.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import javax.sql.DataSource

class DataSourceProvider(private val plugin: JavaPlugin) {
    private var ds: HikariDataSource? = null

    fun getDataSource(): DataSource {
        if (ds != null && !ds!!.isClosed) return ds!!
        val type = plugin.config.getString("database.type", "sqlite")
        val cfg = HikariConfig().apply {
            poolName = "ItemLog-Hikari"
            maximumPoolSize = plugin.config.getInt("database.pool.maximum-pool-size", 10)
            minimumIdle = plugin.config.getInt("database.pool.minimum-idle", 2)
        }

        when (type.lowercase()) {
            "mysql" -> {
                val host = plugin.config.getString("database.mysql.host", "localhost")!!
                val port = plugin.config.getInt("database.mysql.port", 3306)
                val db = plugin.config.getString("database.mysql.database", "itemlog")!!
                val user = plugin.config.getString("database.mysql.user", "root")!!
                val pass = plugin.config.getString("database.mysql.password", "")!!
                val params = plugin.config.getString("database.mysql.params", "useSSL=false&allowPublicKeyRetrieval=true")!!
                jdbcUrl = "jdbc:mysql://$host:$port/$db?$params"
                username = user
                password = pass
                driverClassName = "com.mysql.cj.jdbc.Driver"
            }
            else -> {
                val fileName = plugin.config.getString("database.sqlite.file", "database.db")!!
                val file = File(plugin.dataFolder, fileName)
                file.parentFile?.mkdirs()
                jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                driverClassName = "org.sqlite.JDBC"
                maximumPoolSize = 1
                minimumIdle = 1
                connectionTestQuery = "SELECT 1"
            }
        }

        cfg.addDataSourceProperty("cachePrepStmts", "true")
        ds = HikariDataSource(cfg)
        return ds!!
    }

    fun close() {
        ds?.let { if (!it.isClosed) it.close() }
    }
}