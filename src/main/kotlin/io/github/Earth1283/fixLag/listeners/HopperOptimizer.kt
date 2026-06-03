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

    // Hysteresis: enter throttle mode when TPS drops below threshold, exit only when TPS
    // recovers above recover-tps-threshold. Prevents flickering at borderline TPS values.
    @Volatile private var isThrottling = false

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onHopperMove(event: InventoryMoveItemEvent) {
        if (!plugin.configManager.isHopperOptimizerEnabled) return
        if (event.source.type != InventoryType.HOPPER) return

        val tps = Bukkit.getServer().tps[0]
        val config = plugin.configManager

        if (isThrottling) {
            if (tps >= config.hopperOptimizerRecoverTpsThreshold) {
                isThrottling = false
            }
        } else {
            if (tps < config.hopperOptimizerTpsThreshold) {
                isThrottling = true
            }
        }

        if (isThrottling && ThreadLocalRandom.current().nextDouble() < config.hopperOptimizerCancelChance) {
            event.isCancelled = true
        }
    }
}
