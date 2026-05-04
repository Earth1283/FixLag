package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.entity.Minecart
import org.bukkit.entity.Villager
import org.bukkit.scheduler.BukkitRunnable

class VillagerLobotomizer(private val plugin: FixLag, private val config: ConfigManager) : BukkitRunnable() {

    private val messageManager: MessageManager = plugin.messageManager

    override fun run() {
        if (!config.isVillagerLobotomizationEnabled) return

        var lobotomizedCount = 0
        var restoredCount = 0

        for (world in Bukkit.getWorlds()) {
            for (villager in world.getEntitiesByClass(Villager::class.java)) {
                val trapped = isTrapped(villager)

                if (trapped && villager.isAware) {
                    villager.isAware = false
                    lobotomizedCount++
                } else if (!trapped && !villager.isAware) {
                    villager.isAware = true
                    restoredCount++
                }
            }
        }

        if (lobotomizedCount > 0) {
            messageManager.logInfo("log_villager_lobotomized", "<count>", lobotomizedCount.toString())
        }
        if (restoredCount > 0) {
            messageManager.logInfo("log_villager_restored", "<count>", restoredCount.toString())
        }
    }

    private fun isTrapped(villager: Villager): Boolean {
        // Check if trapped in a minecart
        if (villager.vehicle is Minecart) {
            return true
        }

        // Check if trapped in a 1x1 space (solid blocks on all 4 sides at foot level)
        val footBlock = villager.location.block
        var solidCount = 0

        val sides = arrayOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
        for (face in sides) {
            if (footBlock.getRelative(face).type.isSolid) {
                solidCount++
            }
        }

        return solidCount >= 4
    }
}
