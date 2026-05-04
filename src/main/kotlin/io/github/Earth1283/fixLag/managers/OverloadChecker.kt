package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class OverloadChecker(private val plugin: FixLag, private val entitiesToDelete: List<String>) {

    private var overloadRadius: Int = 50
    private var criticalEntityCount: Int = 30
    private var checkTask: BukkitTask? = null

    init {
        loadConfig()
    }

    fun loadConfig() {
        val config = plugin.config
        overloadRadius = config.getInt("overload-detection.radius", 50)
        criticalEntityCount = config.getInt("overload-detection.critical-entity-count", 30)
    }

    fun startChecking(delayTicks: Long, periodTicks: Long) {
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val onlinePlayers = ArrayList(Bukkit.getOnlinePlayers())

            for (player in onlinePlayers) {
                val playerUUID = player.uniqueId
                val playerLocation = player.location
                val world = playerLocation.world

                if (world != null) {
                    val nearbyEntities = world.getNearbyEntities(playerLocation, overloadRadius.toDouble(), overloadRadius.toDouble(), overloadRadius.toDouble())
                        .toList()

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                        val typeFilteredEntities = nearbyEntities.filter { entity ->
                            entitiesToDelete.contains(entity.type.name.uppercase())
                        }

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            val p = Bukkit.getPlayer(playerUUID)
                            if (p != null && p.isOnline) {
                                val finalCount = typeFilteredEntities.count { entity ->
                                    entity.isValid && !entity.hasPermission("fixlag.overload.exempt")
                                }

                                if (finalCount > criticalEntityCount) {
                                    val message = plugin.messageManager.getMessage("overload_warning", "<count>", finalCount.toString(), "<player>", p.name)
                                    for (staff in Bukkit.getOnlinePlayers()) {
                                        if (staff.isOnline && (staff.isOp || staff.hasPermission("fixlag.overload.notify"))) {
                                            staff.sendMessage(message)
                                        }
                                    }
                                }
                            }
                        })
                    })
                }
            }
        }, delayTicks, periodTicks)
    }

    fun stopChecking() {
        checkTask?.let {
            if (!it.isCancelled) {
                it.cancel()
            }
            checkTask = null
        }
    }
}
