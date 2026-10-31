package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.utils.EntityScan
import org.bukkit.Bukkit
import org.bukkit.entity.Mob
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class PanicModeManager(private val plugin: FixLag) : BukkitRunnable(), Listener {
    private val config = plugin.configManager
    private val scan = EntityScan(plugin)
    private val frozenEntities = mutableSetOf<UUID>()
    var isPanicActive = false
        private set
    val frozenMobCount: Int get() = frozenEntities.size

    override fun run() {
        if (!config.isPanicModeEnabled) {
            restoreAll()
            return
        }
        val tps = Bukkit.getServer().tps[0]
        val active = if (isPanicActive) tps < config.panicModeRecoverTps else tps < config.panicModeTpsThreshold
        val changed = active != isPanicActive
        if (changed) {
            scan.stop()
            isPanicActive = active
        }
        if (!active && frozenEntities.isEmpty()) return
        var changedCount = 0
        if (!active) {
            val ids = frozenEntities.toList()
            scan.startIds(ids, { entity ->
                if (frozenEntities.remove(entity.uniqueId) && entity is Mob) {
                    entity.isAware = true
                    changedCount++
                }
            }, {
                frozenEntities.removeAll(ids.toSet())
                if (changed) plugin.messageManager.logInfo("log_panic_deactivated", "<count>", changedCount.toString())
            })
            return
        }
        scan.start(visit = { entity ->
            if (entity is Mob) {
                if (isPanicActive && config.isPanicModeEnabled) {
                    if (entity.customName == null && !(entity is Tameable && entity.isTamed) && entity.isAware) {
                        entity.isAware = false
                        frozenEntities.add(entity.uniqueId)
                        changedCount++
                    }
                } else if (frozenEntities.remove(entity.uniqueId)) {
                    entity.isAware = true
                    changedCount++
                }
            }
        }, complete = {
            if (!isPanicActive) frozenEntities.clear()
            if (changed) {
                if (active) plugin.messageManager.logWarn("log_panic_activated", "<count>", changedCount.toString())
                else plugin.messageManager.logInfo("log_panic_deactivated", "<count>", changedCount.toString())
            }
        })
    }

    /** Also called on disable, before entities can be saved with awareness disabled. */
    fun restoreAll() {
        scan.stop()
        frozenEntities.forEach { (Bukkit.getEntity(it) as? Mob)?.isAware = true }
        frozenEntities.clear()
        isPanicActive = false
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        if (frozenEntities.isEmpty()) return
        for (entity in event.chunk.entities) {
            if (frozenEntities.remove(entity.uniqueId) && entity is Mob) entity.isAware = true
        }
    }
}
