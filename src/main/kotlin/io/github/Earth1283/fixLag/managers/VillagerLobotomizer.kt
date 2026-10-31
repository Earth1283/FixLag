package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.utils.EntityScan
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.entity.Minecart
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class VillagerLobotomizer(private val plugin: FixLag, private val config: ConfigManager) : BukkitRunnable(), Listener {
    private val scan = EntityScan(plugin)
    private val frozen = mutableSetOf<UUID>()

    override fun run() {
        if (!config.isVillagerLobotomizationEnabled) { restoreAll(); return }
        if (plugin.panicModeManager.isPanicActive) return
        scan.start(visit = { entity ->
            if (entity is Villager && config.isVillagerLobotomizationEnabled && !plugin.panicModeManager.isPanicActive) {
                // Villagers need their brain during work hours to restock trades.
                val freeze = entity.world.time !in 2000L..9000L && isTrapped(entity)
                if (freeze && entity.isAware) {
                    entity.isAware = false
                    frozen.add(entity.uniqueId)
                } else if (!freeze && frozen.remove(entity.uniqueId)) entity.isAware = true
            }
        })
    }

    fun restoreAll() {
        scan.stop()
        frozen.forEach { (Bukkit.getEntity(it) as? Villager)?.isAware = true }
        frozen.clear()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        if (frozen.isEmpty()) return
        for (entity in event.chunk.entities) {
            if (frozen.remove(entity.uniqueId) && entity is Villager) entity.isAware = true
        }
    }

    private fun isTrapped(villager: Villager): Boolean {
        if (villager.vehicle is Minecart) return true
        val foot = villager.location.block
        return arrayOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST).all { face ->
            val x = foot.x + face.modX
            val z = foot.z + face.modZ
            foot.world.isChunkLoaded(x shr 4, z shr 4) && foot.getRelative(face).type.isSolid
        }
    }
}
