package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.SpawnerSpawnEvent

class SpawnerOptimizer(plugin: FixLag) : Listener {

    private val config = plugin.configManager

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSpawnerSpawn(event: SpawnerSpawnEvent) {
        if (!config.isSpawnerOptimizerEnabled) return

        val tps = Bukkit.getServer().tps[0]
        if (tps < config.spawnerOptimizerTpsThreshold) {
            event.isCancelled = true
        }
    }
}
