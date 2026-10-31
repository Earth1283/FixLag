package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.TestFixture
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.Arrow
import org.bukkit.entity.Trident
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import net.kyori.adventure.text.Component
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.io.File
import kotlin.test.*

class ConfigAndCleanupTest : TestFixture() {
    private fun tasks() = TaskManager(plugin, config, messages, mock(PerformanceMonitor::class.java),
        mock(DeletedItemsManager::class.java), DynamicDistanceManager(featurePlugin), mock(LagNotifier::class.java))

    @Test
    fun `reload respects disk edits rather than overwriting them`() {
        val file = File(plugin.dataFolder, "config.yml")
        val edited = YamlConfiguration.loadConfiguration(file)
        edited.set("smart-clear.tps-threshold", 12.0)
        edited.save(file)
        config.loadConfig()
        assertEquals(12.0, config.smartClearTpsThreshold)
        assertEquals(12.0, YamlConfiguration.loadConfiguration(file).getDouble("smart-clear.tps-threshold"))
    }

    @Test
    fun `dynamic distance can be restarted repeatedly`() {
        configure("dynamic-distance.enabled", true)
        val tasks = tasks()
        tasks.startDynamicDistanceTask()
        tasks.startDynamicDistanceTask()
        tasks.startDynamicDistanceTask()
        tasks.stop()
    }

    @Test
    fun `renamed dropped items survive cleanup`() {
        val stack = ItemStack(Material.DIAMOND).apply {
            itemMeta = itemMeta.apply { displayName(Component.text("Valuable")) }
        }
        val entity = mock(Item::class.java)
        `when`(entity.isValid).thenReturn(true)
        `when`(entity.type).thenReturn(EntityType.ITEM)
        `when`(entity.itemStack).thenReturn(stack)
        val tasks = tasks()
        assertFalse(tasks.shouldDeleteEntity(entity))
        configure("ignore-custom-named-items", false)
        assertTrue(tasks.shouldDeleteEntity(entity))
    }

    @Test
    fun `projectiles must be explicitly configured`() {
        configure("entities-to-delete", listOf("ITEM"))
        val arrow = mock(Arrow::class.java)
        `when`(arrow.type).thenReturn(EntityType.ARROW)
        `when`(arrow.isValid).thenReturn(true)
        val trident = mock(Trident::class.java)
        `when`(trident.type).thenReturn(EntityType.TRIDENT)
        `when`(trident.isValid).thenReturn(true)
        `when`(trident.itemStack).thenReturn(ItemStack(Material.TRIDENT))
        val tasks = tasks()
        assertFalse(tasks.shouldDeleteEntity(arrow))
        assertFalse(tasks.shouldDeleteEntity(trident))
        configure("entities-to-delete", listOf("arrow", "trident"))
        assertTrue(tasks.shouldDeleteEntity(arrow))
        assertTrue(tasks.shouldDeleteEntity(trident))
    }

    @Test
    fun `invalid intervals and unreachable recovery thresholds are corrected`() {
        configure("panic-mode.check-interval-seconds", 0)
        configure("xp-orb-merger.check-interval-seconds", -1)
        configure("panic-mode.tps-threshold", 20)
        configure("panic-mode.recover-tps", 20)
        assertTrue(config.panicModeCheckIntervalTicks > 0)
        assertTrue(config.xpOrbMergerCheckIntervalTicks > 0)
        assertTrue(config.panicModeTpsThreshold < config.panicModeRecoverTps)
        assertTrue(config.panicModeRecoverTps <= 20)
    }

    @Test
    fun `config optimizer increases intervals instead of accelerating existing throttles`() {
        val optimizer = ServerConfigOptimizer(plugin, messages)
        val check = ServerConfigOptimizer::class.java.getDeclaredMethod("checkSetting", MutableList::class.java,
            File::class.java, YamlConfiguration::class.java, String::class.java, Any::class.java, Any::class.java)
        check.isAccessible = true
        val changes = mutableListOf<Any>()
        val yaml = YamlConfiguration()
        yaml.set("tick-rates.grass-spread", 8)
        check.invoke(optimizer, changes, File("unused.yml"), yaml, "tick-rates.grass-spread", 1, 4)
        assertTrue(changes.isEmpty())
        yaml.set("tick-rates.grass-spread", 1)
        check.invoke(optimizer, changes, File("unused.yml"), yaml, "tick-rates.grass-spread", 1, 4)
        assertEquals(1, changes.size)
    }
}
