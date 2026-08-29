package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.entity.TraderLlama
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent

// MC-153831: bred trader llama babies don't inherit the persistent (never-despawn) flag of their parents.
class TraderLlamaDespawnFix(plugin: FixLag) : Listener {

    private val config = plugin.configManager

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityBreed(event: EntityBreedEvent) {
        if (!config.isTraderLlamaDespawnFixEnabled) return

        val child = event.entity as? TraderLlama ?: return
        child.setRemoveWhenFarAway(false)
    }
}
