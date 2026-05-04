package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent

class ExplosionOptimizer(plugin: FixLag) : Listener {

    private val config = plugin.configManager
    private val messageManager = plugin.messageManager

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        optimizeExplosion(event.blockList(), event.location)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        optimizeExplosion(event.blockList(), event.block.location)
    }

    private fun optimizeExplosion(blockList: MutableList<Block>, location: Location) {
        if (!config.isExplosionOptimizationEnabled) return

        val tps = Bukkit.getServer().tps[0]
        if (tps > config.explosionOptimizationTpsThreshold) return

        val maxBlocks = config.explosionOptimizationMaxBlocksLimit
        if (maxBlocks > 0 && blockList.size > maxBlocks) {
            blockList.subList(maxBlocks, blockList.size).clear()
        }

        messageManager.logInfo("log_explosion_optimized",
                "<world>", location.world?.name ?: "unknown",
                "<x>", location.blockX.toString(),
                "<y>", location.blockY.toString(),
                "<z>", location.blockZ.toString(),
                "<tps>", "%.2f".format(tps),
                "<count>", blockList.size.toString(),
                "<drops>", config.isExplosionOptimizationDisableDrops.toString())
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityExplodeYield(event: EntityExplodeEvent) {
        if (config.isExplosionOptimizationEnabled && config.isExplosionOptimizationDisableDrops) {
            val tps = Bukkit.getServer().tps[0]
            if (tps <= config.explosionOptimizationTpsThreshold) {
                event.yield = 0f
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockExplodeYield(event: BlockExplodeEvent) {
        if (config.isExplosionOptimizationEnabled && config.isExplosionOptimizationDisableDrops) {
            val tps = Bukkit.getServer().tps[0]
            if (tps <= config.explosionOptimizationTpsThreshold) {
                event.yield = 0f
            }
        }
    }
}
