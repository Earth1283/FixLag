package io.github.Earth1283.fixLag

import be.seeseemelk.mockbukkit.MockBukkit
import org.bukkit.entity.Dolphin
import org.bukkit.event.entity.EntityDamageEvent
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.io.File
import kotlin.test.*

/** Keep the real plugin lifecycle, but make update checks local and deterministic. */
open class LocalUpdatePlugin : FixLag() {
    override fun onEnable() {
        dataFolder.mkdirs()
        saveDefaultConfig()
        val versions = File(dataFolder, "test-versions.json").apply { writeText("[]") }
        config.set("update-url", versions.toURI().toString())
        config.set("deleted-items-persistence.enabled", false)
        config.set("bug-fixes.dolphin-drowning.enabled", false)
        saveConfig()
        super.onEnable()
    }
}

class PluginReloadTest : TestFixture() {
    @Test
    fun `real plugin reload replaces tasks and enables initially disabled listeners`() {
        val live = FixLag::class.java.getResourceAsStream("/plugin.yml")!!.use {
            MockBukkit.loadWith(LocalUpdatePlugin::class.java, it)
        }
        try {
            live.config.set("dynamic-distance.enabled", true)
            live.config.set("xp-orb-merger.enabled", true)
            live.config.set("bug-fixes.dolphin-drowning.enabled", true)
            live.saveConfig()
            live.configManager.loadConfig()
            live.restartTasks()
            // getPendingTasks is unimplemented in this MockBukkit release; isQueued is real.
            fun queuedIds() = (0..500).filter { server.scheduler.isQueued(it) }.toSet()
            val before = queuedIds()
            assertTrue(before.isNotEmpty())
            repeat(3) { live.restartTasks() }
            assertEquals(before.size, queuedIds().size)
            assertTrue(before.none { server.scheduler.isQueued(it) })
            val damage = mock(EntityDamageEvent::class.java)
            `when`(damage.entity).thenReturn(mock(Dolphin::class.java))
            `when`(damage.cause).thenReturn(EntityDamageEvent.DamageCause.DROWNING)
            `when`(damage.handlers).thenReturn(EntityDamageEvent.getHandlerList())
            server.pluginManager.callEvent(damage)
            verify(damage).setCancelled(true)

            val player = server.addPlayer()
            player.addAttachment(live, "fixlag.retrieve", true)
            assertFalse(player.hasPermission("fixlag.command"))
            live.deletedItemsManager.addDeletedItems(listOf(org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND)))
            assertTrue(server.dispatchCommand(player, "fixlag retrieve"))
            assertEquals(org.bukkit.Material.DIAMOND, player.openInventory.topInventory.getItem(0)!!.type)
        } finally {
            server.pluginManager.disablePlugin(live)
        }
    }
}
