package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.utils.EntityScan
import org.bukkit.entity.ExperienceOrb
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor

class ExperienceOrbMerger(private val plugin: FixLag) : BukkitRunnable() {
    private data class Cell(val world: UUID, val x: Int, val y: Int, val z: Int)
    private val scan = EntityScan(plugin)

    override fun run() {
        if (!plugin.configManager.isXpOrbMergerEnabled) { stopScan(); return }
        if (scan.isRunning) return
        val radius = plugin.configManager.xpOrbMergerRadius
        val buckets = mutableMapOf<Cell, ExperienceOrb>()
        val seen = mutableSetOf<UUID>()
        scan.start(visit = { entity ->
            if (plugin.configManager.isXpOrbMergerEnabled && entity is ExperienceOrb && seen.add(entity.uniqueId)) {
                val loc = entity.location
                val cell = Cell(entity.world.uid, floor(loc.x / radius).toInt(), floor(loc.y / radius).toInt(), floor(loc.z / radius).toInt())
                var merged = false
                search@ for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                    val key = cell.copy(x = cell.x + dx, y = cell.y + dy, z = cell.z + dz)
                    val target = buckets[key] ?: continue
                    if (!target.isValid || target.isDead || target.world != entity.world) { buckets.remove(key); continue }
                    val other = target.location
                    if (abs(loc.x - other.x) > radius || abs(loc.y - other.y) > radius || abs(loc.z - other.z) > radius) continue
                    if (mergeOrbs(target, entity)) { merged = true; break@search }
                }
                if (!merged) buckets[cell] = entity
            }
        })
    }

    internal fun mergeOrbs(target: ExperienceOrb, other: ExperienceOrb): Boolean {
        // Paper can already represent multiple equal orbs in one entity. Preserve that XP too.
        val total = target.experience.toLong() * target.count.coerceAtLeast(1) +
            other.experience.toLong() * other.count.coerceAtLeast(1)
        if (total > Int.MAX_VALUE || target === other) return false
        target.experience = total.toInt()
        target.count = 1
        other.remove()
        return true
    }

    fun stopScan() = scan.stop()
}
