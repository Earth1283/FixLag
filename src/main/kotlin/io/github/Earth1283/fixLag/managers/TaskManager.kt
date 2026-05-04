package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class TaskManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val messageManager: MessageManager,
    private val performanceMonitor: PerformanceMonitor,
    private val deletedItemsManager: DeletedItemsManager,
    private val dynamicDistanceManager: DynamicDistanceManager,
    private val lagNotifier: LagNotifier
) {

    private var deletionTask: BukkitTask? = null
    private var smartClearTask: BukkitTask? = null
    private var dynamicDistanceTask: BukkitTask? = null
    private var lagNotifierTask: BukkitTask? = null

    private var lastDeletionTime = System.currentTimeMillis()
    private var warningSent = false

    fun startDeletionTask() {
        deletionTask?.cancel()

        deletionTask = object : BukkitRunnable() {
            override fun run() {
                if (configManager.entitiesToDelete.isEmpty()) {
                    return
                }

                val intervalMs = configManager.deletionIntervalTicks * 50L
                val warningMs = configManager.warningTimeTicks * 50L
                val now = System.currentTimeMillis()
                val elapsed = now - lastDeletionTime

                if (configManager.isEnableWarning && !warningSent && elapsed >= (intervalMs - warningMs)) {
                    sendWarning()
                    warningSent = true
                }

                if (elapsed >= intervalMs) {
                    deleteAndAnnounce()
                    lastDeletionTime = System.currentTimeMillis()
                    warningSent = false
                }
            }
        }.runTaskTimer(plugin, 20L, 20L) // Check every second
    }

    fun startSmartClearTask() {
        smartClearTask?.cancel()
        if (configManager.isSmartClearEnabled) {
            smartClearTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { checkSmartClear() }, 20L * 30, configManager.smartClearCheckIntervalTicks)
        }
    }

    fun startDynamicDistanceTask() {
        dynamicDistanceTask?.cancel()
        if (configManager.isDynamicDistanceEnabled) {
            dynamicDistanceTask = dynamicDistanceManager.runTaskTimer(plugin, 20L * 60, configManager.dynamicDistanceCheckIntervalTicks)
        }
    }

    fun startLagNotifierTask() {
        lagNotifierTask?.cancel()
        if (configManager.isLagNotificationsEnabled) {
            lagNotifierTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { lagNotifier.checkLag() }, 20L * 30, configManager.lagNotificationsCheckIntervalTicks)
        }
    }

    private var lastSmartClearTime = 0L

    private fun checkSmartClear() {
        if (!configManager.isSmartClearEnabled) return

        val tps = Bukkit.getServer().tps[0]
        if (tps < configManager.smartClearTpsThreshold) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSmartClearTime >= configManager.smartClearCooldownTicks * 50L) {
                lastSmartClearTime = currentTime
                messageManager.logInfo("log_smart_clear_triggered", "<tps>", String.format("%.2f", tps))
                deleteAndAnnounce()
            }
        }
    }

    private fun sendWarning() {
        val warningTimeSeconds = configManager.warningTimeTicks / 20L

        if ("ACTION_BAR".equals(configManager.notificationType, ignoreCase = true)) {
            val warningComponent = messageManager.getComponentMessage("entity_clear_warning", "%fixlag_time%", warningTimeSeconds.toString())
            for (player in Bukkit.getOnlinePlayers()) {
                player.sendActionBar(warningComponent)
            }
        } else {
            val formattedMessage = messageManager.getMessage("entity_clear_warning", "%fixlag_time%", warningTimeSeconds.toString())
            for (player in Bukkit.getOnlinePlayers()) {
                player.sendMessage(formattedMessage)
            }
        }
    }

    fun deleteAndAnnounce() {
        val deletedItems = deleteEntities()
        val deletedCount = deletedItems.size
        if (deletedCount > 0) {
            deletedItemsManager.addDeletedItems(deletedItems)

            if ("ACTION_BAR".equals(configManager.notificationType, ignoreCase = true)) {
                val broadcastComponent = messageManager.getComponentMessage("entity_clear_broadcast", "%fixlag_count%", deletedCount.toString())
                for (player in Bukkit.getOnlinePlayers()) {
                    player.sendActionBar(broadcastComponent)
                }
            } else {
                val broadcastMessage = messageManager.getMessage("entity_clear_broadcast", "%fixlag_count%", deletedCount.toString())
                Bukkit.getServer().broadcast(Component.text(broadcastMessage))
            }

            if (configManager.isLogMemoryStats) {
                performanceMonitor.logMemoryUsage()
            }
            messageManager.logInfo("log_entity_deleted", "<count>", deletedCount.toString())
        }
    }

    private fun deleteEntities(): List<ItemStack> {
        val deletedItems = mutableListOf<ItemStack>()
        val entitiesToDelete = configManager.entitiesToDelete
        for (world in Bukkit.getWorlds()) {
            for (entity in world.entities) {
                if (!entity.isValid) continue

                val shouldDelete = entitiesToDelete.contains(entity.type.name)
                val isProjectile = entity is org.bukkit.entity.AbstractArrow

                if (shouldDelete || isProjectile) {
                    if (configManager.isIgnoreCustomNamedItems && entity.customName != null) {
                        continue
                    }

                    if (entity is Trident) {
                        if (entity.itemStack.enchantments.isEmpty()) {
                            deletedItems.add(entity.itemStack)
                        } else {
                            continue
                        }
                    }

                    if (entity is Item) {
                        deletedItems.add(entity.itemStack)
                    }
                    entity.remove()
                }
            }
        }
        return deletedItems
    }
}
