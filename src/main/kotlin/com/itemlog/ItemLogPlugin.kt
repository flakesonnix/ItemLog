package com.itemlog

import com.itemlog.db.DataSourceProvider
import com.itemlog.repository.ItemEventRepository
import com.itemlog.repository.MigrationRunner
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.ItemLogService
import org.bukkit.plugin.java.JavaPlugin
import javax.sql.DataSource

class ItemLogPlugin : JavaPlugin() {
    private lateinit var provider: DataSourceProvider
    private lateinit var dataSource: DataSource
    lateinit var itemLogService: ItemLogService
        private set

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
            return
        }
        val serializer = ItemSerializer()
        val repository = ItemEventRepository(dataSource)
        itemLogService = ItemLogService(this, serializer, repository)
        itemLogService.start()
        logger.info("ItemLog Stage 3 ready — service + buffer")
    }

    override fun onDisable() {
        if (::itemLogService.isInitialized) itemLogService.stop()
        provider.close()
    }
}