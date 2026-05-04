package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable

class DynamicDistanceManager(private val plugin: FixLag) : BukkitRunnable() {

    private val config: ConfigManager = plugin.configManager
    private val messageManager: MessageManager = plugin.messageManager
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()

    override fun run() {
        if (!config.isDynamicDistanceEnabled) return

        val tps = Bukkit.getServer().tps[0]

        for (world in Bukkit.getWorlds()) {
            val currentView = world.viewDistance
            val currentSim = getSimulationDistance(world)

            var newView = currentView
            var newSim = currentSim

            if (tps < config.dynamicDistanceTpsLowThreshold) {
                // Decrease distance
                if (currentView > config.dynamicDistanceMinView) {
                    newView = (currentView - 1).coerceAtLeast(config.dynamicDistanceMinView)
                }
                if (currentSim > config.dynamicDistanceMinSim) {
                    newSim = (currentSim - 1).coerceAtLeast(config.dynamicDistanceMinSim)
                }
            } else if (tps > config.dynamicDistanceTpsHighThreshold) {
                // Increase distance
                if (currentView < config.dynamicDistanceMaxView) {
                    newView = (currentView + 1).coerceAtMost(config.dynamicDistanceMaxView)
                }
                if (currentSim < config.dynamicDistanceMaxSim) {
                    newSim = (currentSim + 1).coerceAtMost(config.dynamicDistanceMaxSim)
                }
            }

            if (newView != currentView || newSim != currentSim) {
                world.viewDistance = newView
                setSimulationDistance(world, newSim)

                messageManager.logInfo("log_distance_changed",
                    "<world>", world.name,
                    "<tps>", String.format("%.2f", tps),
                    "<view>", newView.toString(),
                    "<sim>", newSim.toString())

                if (config.isDynamicDistanceNotifyAdmins) {
                    val msgKey = if (newView < currentView || newSim < currentSim) "distance_decreased" else "distance_increased"
                    val rawMsg = messageManager.getRawMessage(msgKey)
                        .replace("<view>", newView.toString())
                        .replace("<sim>", newSim.toString())

                    val message = miniMessage.deserialize(messageManager.getRawMessage("prefix") + rawMsg)

                    for (player in Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission("fixlag.notify.distance")) {
                            player.sendMessage(message)
                        }
                    }
                }
            }
        }
    }

    private fun getSimulationDistance(world: World): Int {
        return try {
            val method = world.javaClass.getMethod("getSimulationDistance")
            method.invoke(world) as Int
        } catch (e: Exception) {
            world.viewDistance
        }
    }

    private fun setSimulationDistance(world: World, distance: Int) {
        try {
            val method = world.javaClass.getMethod("setSimulationDistance", Int::class.javaPrimitiveType)
            method.invoke(world, distance)
        } catch (ignored: Exception) {
        }
    }
}
