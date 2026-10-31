package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.utils.EntityScan
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor

class OverloadChecker(private val plugin: FixLag) {
    private data class Cell(val world: UUID, val x: Int, val y: Int, val z: Int)
    private class Target(val id: UUID, val location: Location, var count: Int = 0)
    private val scan = EntityScan(plugin)
    private var checkTask: BukkitTask? = null

    fun startChecking(delayTicks: Long, periodTicks: Long) {
        stopChecking()
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { check() }, delayTicks, periodTicks)
    }

    private fun check() {
        if (scan.isRunning || plugin.configManager.entitiesToDelete.isEmpty()) return
        val radius = plugin.config.getInt("overload-detection.radius", 50).coerceAtLeast(1).toDouble()
        val threshold = plugin.config.getInt("overload-detection.critical-entity-count", 65).coerceAtLeast(1)
        val targets = Bukkit.getOnlinePlayers().filter { !it.hasPermission("fixlag.overload.exempt") }
            .map { Target(it.uniqueId, it.location) }
        if (targets.isEmpty()) return
        val buckets = mutableMapOf<Cell, MutableList<Target>>()
        for (target in targets) {
            val loc = target.location
            val cell = Cell(loc.world!!.uid, floor(loc.x / radius).toInt(), floor(loc.y / radius).toInt(), floor(loc.z / radius).toInt())
            for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                buckets.getOrPut(cell.copy(x = cell.x + dx, y = cell.y + dy, z = cell.z + dz)) { mutableListOf() }.add(target)
            }
        }
        val seen = mutableSetOf<UUID>()
        scan.start(visit = { entity ->
            if (entity.type.name in plugin.configManager.entitiesToDelete && seen.add(entity.uniqueId)) {
                val loc = entity.location
                val cell = Cell(entity.world.uid, floor(loc.x / radius).toInt(), floor(loc.y / radius).toInt(), floor(loc.z / radius).toInt())
                for (target in buckets[cell].orEmpty()) {
                    val p = target.location
                    if (abs(loc.x - p.x) <= radius && abs(loc.y - p.y) <= radius && abs(loc.z - p.z) <= radius) target.count++
                }
            }
        }, complete = {
            for (target in targets.filter { it.count > threshold }) {
                val player = Bukkit.getPlayer(target.id) ?: continue
                if (!player.isOnline || player.hasPermission("fixlag.overload.exempt") || player.world != target.location.world) continue
                if (player.location.distanceSquared(target.location) > radius * radius) continue
                val message = plugin.messageManager.getMessage("overload_warning", "<count>", target.count.toString(), "<player>", player.name)
                Bukkit.getOnlinePlayers().filter { it.hasPermission("fixlag.overload.notify") }.forEach { it.sendMessage(message) }
            }
        })
    }

    fun stopChecking() {
        checkTask?.cancel()
        checkTask = null
        scan.stop()
    }
}
