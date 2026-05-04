package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent

class ArmorStandOptimizer(plugin: FixLag) : Listener {

    private val config = plugin.configManager

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        if (!config.isArmorStandOptimizerEnabled) return

        val entity = event.entity
        if (entity is ArmorStand) {
            if (config.isArmorStandDisableGravity) {
                entity.setGravity(false)
            }
            if (config.isArmorStandDisableCollisions) {
                entity.isCollidable = false
            }
        }
    }
}
