package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

class LagNotifier(private val plugin: FixLag) {

    private val configManager: ConfigManager = plugin.configManager
    private val messageManager: MessageManager = plugin.messageManager
    private val lastAlertTime = mutableMapOf<String, Long>()
    private val df = DecimalFormat("#.##")

    fun checkLag() {
        if (!configManager.isLagNotificationsEnabled) return

        val now = System.currentTimeMillis()
        val cooldownMillis = configManager.lagNotificationsCooldownTicks * 50 // Convert ticks to ms

        // TPS Check
        if (configManager.isLagNotificationsTpsEnabled) {
            val tps = Bukkit.getServer().tps[0]
            if (tps < configManager.lagNotificationsTpsThreshold) {
                triggerAlert("tps", now, cooldownMillis, "lag_alert_tps", "%fixlag_tps%", df.format(tps))
            }
        }

        // RAM Check
        if (configManager.isLagNotificationsRamEnabled) {
            val memoryMXBean = ManagementFactory.getMemoryMXBean()
            val heapMemoryUsage = memoryMXBean.heapMemoryUsage
            val usedPercent = heapMemoryUsage.used.toDouble() / heapMemoryUsage.max * 100

            if (usedPercent > configManager.lagNotificationsRamThreshold) {
                triggerAlert("ram", now, cooldownMillis, "lag_alert_ram", "%fixlag_ram_percent%", df.format(usedPercent))
            }
        }

        // Ping Check
        if (configManager.isLagNotificationsPingEnabled) {
            var totalPing = 0
            val onlinePlayers = Bukkit.getOnlinePlayers()
            val playerCount = onlinePlayers.size
            if (playerCount > 0) {
                for (p in onlinePlayers) {
                    totalPing += p.ping
                }
                val avgPing = totalPing / playerCount

                if (avgPing > configManager.lagNotificationsPingThreshold) {
                    triggerAlert("ping", now, cooldownMillis, "lag_alert_ping", "%fixlag_avg_ping%", avgPing.toString())
                }
            }
        }
    }

    private fun triggerAlert(type: String, now: Long, cooldownMillis: Long, messageKey: String, vararg replacements: String) {
        val lastAlert = lastAlertTime.getOrDefault(type, 0L)
        if (now - lastAlert < cooldownMillis) return

        val message = messageManager.getMessage(messageKey, true, *replacements)

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fixlag.notify.lag") || player.isOp) {
                player.sendMessage(message)
            }
        }

        messageManager.logWarn(messageKey, *replacements)
        lastAlertTime[type] = now
    }
}
