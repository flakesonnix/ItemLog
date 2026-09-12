package com.itemlog.service

import com.itemlog.repository.ItemEventRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Test

class RetentionServiceTest {

    @Test
    fun `start respects disabled retention`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val config = YamlConfiguration()
        config.set("retention.enabled", false)
        every { plugin.config } returns config
        every { plugin.logger } returns mockk(relaxed = true)
        val repo = mockk<ItemEventRepository>(relaxed = true)
        val service = RetentionService(plugin, repo)
        service.start()
        verify(exactly = 0) { plugin.server }
    }

    @Test
    fun `start schedules task when enabled`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val config = YamlConfiguration()
        config.set("retention.enabled", true)
        config.set("retention.days", 30)
        every { plugin.config } returns config
        val server = mockk<org.bukkit.Server>(relaxed = true)
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        every { scheduler.runTaskTimerAsynchronously(any(), any<Runnable>(), any(), any()) } returns mockk(relaxed = true)
        every { plugin.logger } returns mockk(relaxed = true)
        val repo = mockk<ItemEventRepository>(relaxed = true)
        val service = RetentionService(plugin, repo)
        service.start()
        verify { scheduler.runTaskTimerAsynchronously(plugin, any<Runnable>(), 1200L, any()) }
    }

    @Test
    fun `start respects zero retention days`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val config = YamlConfiguration()
        config.set("retention.enabled", true)
        config.set("retention.days", 0)
        every { plugin.config } returns config
        every { plugin.logger } returns mockk(relaxed = true)
        val repo = mockk<ItemEventRepository>(relaxed = true)
        val service = RetentionService(plugin, repo)
        service.start()
        verify(exactly = 0) { plugin.server }
    }
}
