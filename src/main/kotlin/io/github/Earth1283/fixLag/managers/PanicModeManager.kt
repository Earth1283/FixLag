package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.entity.Mob
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class PanicModeManager(private val plugin: FixLag) : BukkitRunnable(), Listener {

    private val config: ConfigManager = plugin.configManager
    private val messageManager: MessageManager = plugin.messageManager
    private var isPanicModeActive = false
    private val frozenEntities = mutableSetOf<UUID>()

    val isPanicActive: Boolean get() = isPanicModeActive
    val frozenMobCount: Int get() = frozenEntities.size

    override fun run() {
        if (!config.isPanicModeEnabled) return

        val tps = Bukkit.getServer().tps[0]

        if (!isPanicModeActive && tps < config.panicModeTpsThreshold) {
            activatePanicMode()
        } else if (isPanicModeActive && tps >= config.panicModeRecoverTps) {
            deactivatePanicMode()
        }
    }

    private fun activatePanicMode() {
        isPanicModeActive = true
        var frozenCount = 0

        for (world in Bukkit.getWorlds()) {
            for (entity in world.livingEntities) {
                if (entity is Mob) {
                    if (entity.getCustomName() != null) continue
                    if (entity is Tameable && entity.isTamed) continue

                    if (entity.isAware) {
                        entity.isAware = false
                        frozenEntities.add(entity.uniqueId)
                        frozenCount++
                    }
                }
            }
        }

        messageManager.logWarn("log_panic_activated", "<count>", frozenCount.toString())
    }

    private fun deactivatePanicMode() {
        isPanicModeActive = false
        var thawedCount = 0

        for (world in Bukkit.getWorlds()) {
            for (entity in world.livingEntities) {
                if (entity.uniqueId in frozenEntities) {
                    if (entity is Mob) {
                        entity.isAware = true
                        thawedCount++
                    }
                }
            }
        }

        frozenEntities.clear()
        messageManager.logInfo("log_panic_deactivated", "<count>", thawedCount.toString())
    }

    // Restore AI on frozen mobs before the chunk is saved to disk so NoAI doesn't persist.
    @EventHandler(ignoreCancelled = true)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        if (!isPanicModeActive || frozenEntities.isEmpty()) return
        for (entity in event.chunk.entities) {
            if (entity.uniqueId in frozenEntities) {
                if (entity is Mob) entity.isAware = true
                frozenEntities.remove(entity.uniqueId)
            }
        }
    }
}
