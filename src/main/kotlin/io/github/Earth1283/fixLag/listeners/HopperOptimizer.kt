package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType
import java.util.concurrent.ThreadLocalRandom

class HopperOptimizer(private val plugin: FixLag) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onHopperMove(event: InventoryMoveItemEvent) {
        if (!plugin.configManager.isHopperOptimizerEnabled) return

        if (event.source.type != InventoryType.HOPPER) return

        val tps = Bukkit.getServer().tps[0]
        if (tps < plugin.configManager.hopperOptimizerTpsThreshold) {
            val chance = plugin.configManager.hopperOptimizerCancelChance
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                event.isCancelled = true
            }
        }
    }
}
