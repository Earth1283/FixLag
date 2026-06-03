package io.github.Earth1283.fixLag.managers

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ServerConfigOptimizer(private val plugin: JavaPlugin, private val messageManager: MessageManager) {

    private val pendingChanges = ConcurrentHashMap<CommandSender, List<ConfigChange>>()

    fun analyze(sender: CommandSender) {
        sender.sendMessage(messageManager.getMessage("optimize_config_analyzing"))
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val changes = collectChanges()
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (changes.isEmpty()) {
                    sender.sendMessage(messageManager.getComponentMessage("optimize_config_no_changes"))
                    return@Runnable
                }
                pendingChanges[sender] = changes
                sender.sendMessage(messageManager.getComponentMessage("optimize_config_header"))
                for (change in changes) {
                    sender.sendMessage(messageManager.getComponentMessage("optimize_config_entry",
                        "%file%", change.file.name,
                        "%key%", change.key,
                        "%old%", change.currentValue.toString(),
                        "%new%", change.newValue.toString()
                    ))
                }
                sender.sendMessage(messageManager.getComponentMessage("optimize_config_footer"))
            })
        })
    }

    private fun collectChanges(): List<ConfigChange> {
        val changes = mutableListOf<ConfigChange>()

        // Analyze bukkit.yml
        val bukkitFile = File("bukkit.yml")
        if (bukkitFile.exists()) {
            val bukkitConfig = YamlConfiguration.loadConfiguration(bukkitFile)
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.monsters", 70, 50)
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.animals", 10, 8)
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.water-animals", 15, 3)
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.water-ambient", 20, 1)
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.ambient", 15, 1)
            checkSetting(changes, bukkitFile, bukkitConfig, "chunk-gc.period-in-ticks", 600, 400)
            checkSetting(changes, bukkitFile, bukkitConfig, "ticks-per.monster-spawns", 1, 4)
            checkSetting(changes, bukkitFile, bukkitConfig, "ticks-per.animal-spawns", 400, 400)
        }

        // Analyze spigot.yml
        val spigotFile = File("spigot.yml")
        if (spigotFile.exists()) {
            val spigotConfig = YamlConfiguration.loadConfiguration(spigotFile)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.animals", 32, 16)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.monsters", 32, 24)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.raiders", 48, 48)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.misc", 16, 8)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.players", 48, 48)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.animals", 48, 48)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.monsters", 48, 48)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.misc", 32, 32)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.merge-radius.item", 2.5, 4.0)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.merge-radius.exp", 3.0, 6.0)
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.view-distance", "default", 4)
        }

        // Analyze server.properties
        val serverPropsFile = File("server.properties")
        if (serverPropsFile.exists()) {
            val props = Properties()
            try {
                FileInputStream(serverPropsFile).use { props.load(it) }
                checkProperty(changes, serverPropsFile, props, "view-distance", 10, 6)
                checkProperty(changes, serverPropsFile, props, "network-compression-threshold", 256, 256)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // Analyze paper.yml (Old Paper)
        val paperFile = File("paper.yml")
        if (paperFile.exists()) {
            val paperConfig = YamlConfiguration.loadConfiguration(paperFile)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.max-auto-save-chunks-per-tick", 24, 8)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.optimize-explosions", false, true)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.mob-spawner-tick-rate", 1, 2)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.game-mechanics.disable-chest-cat-detection", false, true)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.hopper.disable-move-event", false, true)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.non-player-arrow-despawn-rate", -1, 60)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.creative-arrow-despawn-rate", -1, 60)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.prevent-moving-into-unloaded-chunks", false, true)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.use-faster-eigencraft-redstone", false, true)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.armor-stands-do-collision-entity-lookups", true, false)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.per-player-mob-spawns", false, true)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.alt-item-despawn-rate.enabled", false, true)
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.alt-item-despawn-rate.items.cobblestone", 300, 100)
        }

        // Analyze config/paper-global.yml (New Paper Global)
        val paperGlobalFile = File("config/paper-global.yml")
        if (paperGlobalFile.exists()) {
            val paperGlobalConfig = YamlConfiguration.loadConfiguration(paperGlobalFile)
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "chunk-loading.min-load-radius", 2, 2)
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "misc.client-interaction-updates-per-tick-in-single-player", -1, 1)
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "spam-limiter.tab-spam-increment", 1, 10)
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "spam-limiter.tab-spam-limit", 500, 20)
        }

        // Analyze config/paper-world-defaults.yml (New Paper World Defaults)
        val paperWorldFile = File("config/paper-world-defaults.yml")
        if (paperWorldFile.exists()) {
            val paperWorldConfig = YamlConfiguration.loadConfiguration(paperWorldFile)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "environment.optimize-explosions", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "environment.disable-teleportation-suffocation-check", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "chunks.auto-save-interval", -1, 6000)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "chunks.max-auto-save-chunks-per-tick", 24, 8)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.spawning.per-player-mob-spawns", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.behavior.disable-chest-cat-detection", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.behavior.spawner-nerfed-mobs-should-jump", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.entities-target-with-follow-range", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "collisions.only-players-collide", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "tick-rates.mob-spawner", 1, 2)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "tick-rates.grass-spread", 1, 4)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "tick-rates.container-update", 1, 2)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "hopper.disable-move-event", false, true)
            checkSetting(changes, paperWorldFile, paperWorldConfig, "hopper.ignore-occluding-blocks", false, true)
        }

        return changes
    }

    private fun checkSetting(changes: MutableList<ConfigChange>, file: File, config: YamlConfiguration, key: String, threshold: Any, optimized: Any) {
        if (!config.contains(key)) return

        val current = config.get(key)

        if (current is Boolean && optimized is Boolean) {
            if (current != optimized) {
                changes.add(ConfigChange(file, key, current, optimized))
            }
        } else if (current is Number && optimized is Number) {
            val currVal = current.toDouble()
            val optVal = optimized.toDouble()

            val lowerIsBetter = !key.contains("merge-radius") && !key.contains("ticks-per") && !key.contains("tab-spam-limit")

            if (lowerIsBetter) {
                if (currVal > optVal) {
                    changes.add(ConfigChange(file, key, current, optimized))
                }
            } else {
                if (currVal < optVal) {
                    changes.add(ConfigChange(file, key, current, optimized))
                }
            }
        }
    }

    private fun checkProperty(changes: MutableList<ConfigChange>, file: File, props: Properties, key: String, threshold: Any, optimized: Any) {
        if (!props.containsKey(key)) return

        val currentStr = props.getProperty(key)
        try {
            val currVal = currentStr.toDouble()
            val optVal = optimized.toString().toDouble()

            // For server.properties, usually lower is better (view-distance)
            val lowerIsBetter = key != "network-compression-threshold"

            if (lowerIsBetter) {
                if (currVal > optVal) {
                    changes.add(ConfigChange(file, key, currentStr, optimized))
                }
            }
        } catch (ignored: NumberFormatException) {
        }
    }

    fun applyChanges(sender: CommandSender, overwrite: Boolean) {
        val changes = pendingChanges.remove(sender)
        if (changes == null || changes.isEmpty()) {
            sender.sendMessage(messageManager.getMessage("optimize_config_no_pending"))
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val errors = mutableListOf<String>()
            val byFile = changes.groupBy { it.file }

            for ((file, fileChanges) in byFile) {
                try {
                    if (file.name.endsWith(".yml")) {
                        val config = YamlConfiguration.loadConfiguration(file)
                        for (change in fileChanges) {
                            config.set(change.key, change.newValue)
                        }
                        val saveFile = if (overwrite) file else File(file.parent, file.name + ".changed")
                        config.save(saveFile)
                    } else if (file.name == "server.properties") {
                        val lines = java.nio.file.Files.readAllLines(file.toPath()).toMutableList()
                        for (change in fileChanges) {
                            var found = false
                            for (i in lines.indices) {
                                val line = lines[i].trim()
                                if (line.startsWith("${change.key}=") || line.startsWith("${change.key} =")) {
                                    lines[i] = "${change.key}=${change.newValue}"
                                    found = true
                                    break
                                }
                            }
                            if (!found) {
                                lines.add("${change.key}=${change.newValue}")
                            }
                        }
                        val saveFile = if (overwrite) file else File(file.parent, file.name + ".changed")
                        java.nio.file.Files.write(saveFile.toPath(), lines)
                    }
                } catch (e: IOException) {
                    errors.add(e.message ?: "unknown")
                }
            }

            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (errors.isEmpty()) {
                    sender.sendMessage(messageManager.getMessage("optimize_config_applied"))
                } else {
                    errors.forEach { err ->
                        sender.sendMessage(messageManager.getMessage("optimize_config_error", "%error%", err))
                    }
                }
            })
        })
    }

    fun rejectChanges(sender: CommandSender) {
        if (pendingChanges.remove(sender) != null) {
            sender.sendMessage(messageManager.getMessage("optimize_config_rejected"))
        } else {
            sender.sendMessage(messageManager.getMessage("optimize_config_no_pending"))
        }
    }

    private data class ConfigChange(val file: File, val key: String, val currentValue: Any, val newValue: Any)
}
