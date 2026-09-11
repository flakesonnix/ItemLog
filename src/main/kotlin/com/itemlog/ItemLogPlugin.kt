package com.itemlog

import com.itemlog.db.DataSourceProvider
import com.itemlog.listener.BlockItemListener
import com.itemlog.listener.ConsumeDestroyListener
import com.itemlog.listener.ContainerListener
import com.itemlog.listener.CraftSmeltListener
import com.itemlog.listener.DeathListener
import com.itemlog.listener.DropListener
import com.itemlog.listener.InventoryListener
import com.itemlog.listener.PickupListener
import com.itemlog.repository.ItemEventRepository
import com.itemlog.repository.MigrationRunner
import com.itemlog.repository.RestorationRepository
import com.itemlog.serialization.ItemSerializer
import com.itemlog.service.EventDeduplicator
import com.itemlog.service.ItemLogService
import com.itemlog.service.RestorationService
import javax.sql.DataSource
import org.bukkit.plugin.java.JavaPlugin

class ItemLogPlugin : JavaPlugin() {
    private lateinit var provider: DataSourceProvider
    private lateinit var dataSource: DataSource
    lateinit var itemLogService: ItemLogService
        private set
    lateinit var restorationService: RestorationService
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
        val restorationRepo = RestorationRepository(dataSource)
        itemLogService = ItemLogService(this, serializer, repository)
        restorationService = RestorationService(this, repository, restorationRepo, serializer)
        itemLogService.start()
        val deduplicator = EventDeduplicator()
        server.pluginManager.registerEvents(PickupListener(itemLogService), this)
        server.pluginManager.registerEvents(DropListener(itemLogService), this)
        server.pluginManager.registerEvents(DeathListener(itemLogService, serializer), this)
        server.pluginManager.registerEvents(ContainerListener(itemLogService, serializer), this)
        server.pluginManager.registerEvents(CraftSmeltListener(itemLogService, serializer), this)
        server.pluginManager.registerEvents(InventoryListener(itemLogService, serializer), this)
        server.pluginManager.registerEvents(ConsumeDestroyListener(itemLogService, serializer), this)
        server.pluginManager.registerEvents(BlockItemListener(itemLogService, serializer, deduplicator), this)
        logger.info("ItemLog Stage 7 ready — Block/Spawn + Deduplicator (crash-safe, ordering)")
    }

    override fun onDisable() {
        if (::itemLogService.isInitialized) itemLogService.stop()
        provider.close()
    }
}
