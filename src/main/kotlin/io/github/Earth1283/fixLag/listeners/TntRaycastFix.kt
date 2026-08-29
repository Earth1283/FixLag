package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

// MC-263882: primed TNT runs a fall-damage raycast every tick even though it's immune to fall
// damage; the raycast's cost scales with distance moved, so a fast-moving (e.g. cannoned) TNT
// makes it expensive. The raycast is internal to vanilla movement and can't be skipped from a
// plugin, so this bounds the damage by capping how far primed TNT can move in one tick.
class TntRaycastFix(private val plugin: FixLag) : BukkitRunnable(), Listener {

    private val config = plugin.configManager
    private val tracked = mutableSetOf<UUID>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        if (!config.isTntRaycastFixEnabled) return
        if (event.entity is TNTPrimed) {
            tracked.add(event.entity.uniqueId)
        }
    }

    override fun run() {
        if (!config.isTntRaycastFixEnabled) {
            if (tracked.isNotEmpty()) tracked.clear()
            return
        }
        if (tracked.isEmpty()) return

        val maxSpeed = config.tntRaycastFixMaxVelocityPerTick
        val iterator = tracked.iterator()
        while (iterator.hasNext()) {
            val entity = Bukkit.getEntity(iterator.next()) as? TNTPrimed
            if (entity == null || !entity.isValid) {
                iterator.remove()
                continue
            }
            val velocity = entity.velocity
            if (velocity.lengthSquared() > maxSpeed * maxSpeed) {
                entity.velocity = velocity.normalize().multiply(maxSpeed)
            }
        }
    }
}
