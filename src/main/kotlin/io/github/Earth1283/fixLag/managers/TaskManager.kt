package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.utils.EntityScan
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.entity.Entity
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
    private val deletionScan = EntityScan(plugin)

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
            dynamicDistanceTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { dynamicDistanceManager.run() }, 20L * 60, configManager.dynamicDistanceCheckIntervalTicks)
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
        if (deletionScan.isRunning || configManager.entitiesToDelete.isEmpty()) return
        var deletedCount = 0
        val recovered = mutableListOf<ItemStack>()
        deletionScan.start(visit = { entity ->
            if (shouldDeleteEntity(entity)) {
                when (entity) {
                    is Item -> recovered.add(entity.itemStack.clone())
                    is Trident -> recovered.add(entity.itemStack.clone())
                }
                entity.remove()
                deletedCount++
            }
        }, afterBatch = {
            if (recovered.isNotEmpty()) {
                deletedItemsManager.addDeletedItems(recovered)
                recovered.clear()
            }
        }, complete = {
            announceDeletion(deletedCount)
        })
    }

    private fun announceDeletion(deletedCount: Int) {
        lastDeletionTime = System.currentTimeMillis()
        warningSent = false
        if (deletedCount > 0) {

            if ("ACTION_BAR".equals(configManager.notificationType, ignoreCase = true)) {
                val broadcastComponent = messageManager.getComponentMessage("entity_clear_broadcast", "%fixlag_count%", deletedCount.toString())
                for (player in Bukkit.getOnlinePlayers()) {
                    player.sendActionBar(broadcastComponent)
                }
            } else {
                Bukkit.getServer().broadcast(messageManager.getComponentMessage("entity_clear_broadcast", "%fixlag_count%", deletedCount.toString()))
            }

            if (configManager.isLogMemoryStats) {
                performanceMonitor.logMemoryUsage()
            }
            messageManager.logInfo("log_entity_deleted", "<count>", deletedCount.toString())
        }
    }

    internal fun shouldDeleteEntity(entity: Entity): Boolean {
        if (!entity.isValid || entity.type.name !in configManager.entitiesToDelete) return false
        val stack = when (entity) {
            is Item -> entity.itemStack
            is Trident -> entity.itemStack
            else -> null
        }
        if (configManager.isIgnoreCustomNamedItems &&
            (entity.customName != null || stack?.itemMeta?.hasDisplayName() == true)) return false
        return entity !is Trident || stack?.itemMeta?.hasEnchants() != true
    }

    fun stop() {
        deletionTask?.cancel()
        smartClearTask?.cancel()
        dynamicDistanceTask?.cancel()
        lagNotifierTask?.cancel()
        deletionScan.stop()
    }
}
