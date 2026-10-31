package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.utils.EntityScan
import org.bukkit.Bukkit
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class CollisionOptimizer(private val plugin: FixLag) : BukkitRunnable(), Listener {
    private val disabledCollisions = mutableSetOf<UUID>()
    private val scan = EntityScan(plugin)
    private var optimizing = false

    override fun run() {
        val optimize = plugin.configManager.isCollisionOptimizerEnabled &&
            Bukkit.getServer().tps[0] < plugin.configManager.collisionOptimizerTpsThreshold
        if (optimize != optimizing) scan.stop()
        optimizing = optimize
        if (!optimize && disabledCollisions.isEmpty()) return
        if (!optimize) {
            val ids = disabledCollisions.toList()
            scan.startIds(ids, { entity ->
                if (disabledCollisions.remove(entity.uniqueId) && entity is LivingEntity) entity.isCollidable = true
            }, { disabledCollisions.removeAll(ids.toSet()) })
            return
        }
        scan.start(visit = { entity ->
            if (entity is LivingEntity && entity !is Player) {
                if (optimizing && plugin.configManager.isCollisionOptimizerEnabled &&
                    (entity is Animals || entity is Monster || entity is Ambient)) {
                    if (entity.isCollidable) {
                        entity.isCollidable = false
                        disabledCollisions.add(entity.uniqueId)
                    }
                } else if (disabledCollisions.remove(entity.uniqueId)) entity.isCollidable = true
            }
        }, complete = { if (!optimizing) disabledCollisions.clear() })
    }

    fun restoreAll() {
        scan.stop()
        disabledCollisions.forEach { (Bukkit.getEntity(it) as? LivingEntity)?.isCollidable = true }
        disabledCollisions.clear()
        optimizing = false
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        if (disabledCollisions.isEmpty()) return
        for (entity in event.chunk.entities) {
            if (disabledCollisions.remove(entity.uniqueId) && entity is LivingEntity) entity.isCollidable = true
        }
    }
}
