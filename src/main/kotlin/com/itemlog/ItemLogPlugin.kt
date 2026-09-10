package com.itemlog

import com.itemlog.db.DataSourceProvider
import com.itemlog.repository.MigrationRunner
import org.bukkit.plugin.java.JavaPlugin
import javax.sql.DataSource

class ItemLogPlugin : JavaPlugin() {
    private lateinit var provider: DataSourceProvider
    private lateinit var dataSource: DataSource

    override fun onEnable() {
        saveDefaultConfig()
        provider = DataSourceProvider(this)
        dataSource = provider.getDataSource()
        try {
            MigrationRunner(dataSource).migrate()
            logger.info("ItemLog Stage 1 ready — tables exist")
        } catch (e: Exception) {
            logger.severe("Stage 1 failed: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        provider.close()
    }
}