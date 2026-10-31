package io.github.Earth1283.fixLag.managers

import com.google.gson.Gson
import com.google.gson.JsonArray
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

class UpdateChecker(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager,
    private val messageManager: MessageManager
) {
    private val gson = Gson()
    private var task: BukkitTask? = null

    fun startUpdateCheckTask() {
        stop()
        val updateUrl = configManager.updateUrl
        val currentVersion = plugin.description.version
        task = object : BukkitRunnable() {
            override fun run() {
                try {
                    val latestVersion = getLatestVersion(updateUrl)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        if (latestVersion != null) {
                            if (currentVersion != latestVersion) {
                                messageManager.logInfo("log_update_available", "<version>", latestVersion)
                                notifyUpdate(latestVersion)
                            } else {
                                messageManager.logInfo("log_update_uptodate")
                            }
                        } else {
                            messageManager.logWarn("log_update_check_failed")
                        }
                    })
                } catch (e: Exception) {
                    plugin.logger.warning("Update check failed: ${e.message}")
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, configManager.updateCheckIntervalTicks)
    }

    @Throws(IOException::class)
    private fun getLatestVersion(updateUrl: String): String? {
        val connection = URL(updateUrl).openConnection().apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
            val versions = gson.fromJson(reader, JsonArray::class.java)
            if (versions != null && !versions.isJsonNull && versions.size() > 0) {
                val latestVersionElement = versions[0]
                if (latestVersionElement.isJsonObject) {
                    val latestVersionObject = latestVersionElement.asJsonObject
                    if (latestVersionObject.has("version_number")) {
                        return latestVersionObject["version_number"].asString
                    }
                }
            }
            return null
        }
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun notifyUpdate(latestVersion: String) {
        val message = messageManager.getMessage("update_available", "%fixlag_latest_version%", latestVersion) + "\n" +
                messageManager.getMessage("update_current_version", "%fixlag_current_version%", plugin.description.version)
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission("fixlag.notify.update")) {
                player.sendMessage(message)
            }
        }
    }
}
