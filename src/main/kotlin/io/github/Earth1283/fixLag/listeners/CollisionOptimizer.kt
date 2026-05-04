package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.entity.*
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class CollisionOptimizer(private val plugin: FixLag) : BukkitRunnable() {

    private val disabledCollisions = mutableSetOf<UUID>()

    override fun run() {
        if (!plugin.configManager.isCollisionOptimizerEnabled) {
            if (disabledCollisions.isNotEmpty()) {
                restoreAll()
            }
            return
        }

        val tps = Bukkit.getServer().tps[0]
        if (tps < plugin.configManager.collisionOptimizerTpsThreshold) {
            optimize()
        } else {
            if (disabledCollisions.isNotEmpty()) {
                restoreAll()
            }
        }
    }

    private fun optimize() {
        for (world in Bukkit.getWorlds()) {
            for (entity in world.entities) {
                if (shouldDisableCollision(entity)) {
                    val living = entity as LivingEntity
                    if (living.isCollidable) {
                        living.isCollidable = false
                        disabledCollisions.add(entity.uniqueId)
                    }
                }
            }
        }
    }

    private fun shouldDisableCollision(entity: Entity): Boolean {
        if (entity !is LivingEntity) return false
        if (entity is Player) return false
        
        return entity is Animals || entity is Monster || entity is Ambient
    }

    private fun restoreAll() {
        val toRemove = mutableSetOf<UUID>()
        for (uuid in disabledCollisions) {
            val entity = Bukkit.getEntity(uuid)
            if (entity is LivingEntity) {
                entity.isCollidable = true
            }
            toRemove.add(uuid)
        }
        disabledCollisions.removeAll(toRemove)
    }
}
