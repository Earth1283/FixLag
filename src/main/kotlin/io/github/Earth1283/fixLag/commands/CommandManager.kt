package io.github.Earth1283.fixLag.commands

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.managers.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class CommandManager(
    private val plugin: JavaPlugin,
    private val taskManager: TaskManager,
    private val performanceMonitor: PerformanceMonitor,
    private val messageManager: MessageManager,
    private val deletedItemsManager: DeletedItemsManager,
    private val chunkAnalyzer: ChunkAnalyzer,
    private val configOptimizer: ServerConfigOptimizer,
    private val redstoneAnalyzer: RedstoneAnalyzer
) : TabExecutor {

    init {
        registerCommands()
    }

    private fun registerCommands() {
        plugin.getCommand("fixlag")?.let {
            it.setExecutor(this)
            it.tabCompleter = this
        }
        plugin.getCommand("gcinfo")?.setExecutor(this)
        plugin.getCommand("serverinfo")?.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return when (command.name.lowercase()) {
            "fixlag" -> handleFixLagCommand(sender, args)
            "gcinfo" -> handleGcInfoCommand(sender)
            "serverinfo" -> handleServerInfoCommand(sender)
            else -> false
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (command.name.equals("fixlag", ignoreCase = true)) {
            if (args.size == 1) {
                val subcommands = mutableListOf<String>()
                if (sender.hasPermission("fixlag.retrieve")) subcommands.add("retrieve")
                if (sender.hasPermission("fixlag.reload")) subcommands.add("reload")
                if (sender.hasPermission("fixlag.checkchunks")) subcommands.add("checkchunks")
                if (sender.hasPermission("fixlag.optimizeconfig")) subcommands.add("optimizeconfig")
                if (sender.hasPermission("fixlag.checkredstone")) subcommands.add("checkredstone")
                if (sender.hasPermission("fixlag.status")) subcommands.add("status")

                return subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            if (args.size == 2 && args[0].equals("optimizeconfig", ignoreCase = true) && sender.hasPermission("fixlag.optimizeconfig")) {
                val subcommands = listOf("accept", "save-new", "reject")
                return subcommands.filter { it.startsWith(args[1], ignoreCase = true) }
            }
        }
        return null
    }

    private fun handleFixLagCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            when (args[0].lowercase()) {
                "reload" -> {
                    if (!sender.hasPermission("fixlag.reload")) {
                        sender.sendMessage(messageManager.getMessage("permission_denied"))
                        return true
                    }
                    if (plugin is FixLag) {
                        plugin.configManager.loadConfig()
                        plugin.messageManager.loadMessages()
                        plugin.taskManager.startDeletionTask()
                        plugin.taskManager.startSmartClearTask()
                        plugin.taskManager.startDynamicDistanceTask()
                        plugin.taskManager.startLagNotifierTask()
                        plugin.updateChecker.startUpdateCheckTask()
                    }
                    sender.sendMessage(messageManager.getMessage("command_reload_success"))
                    return true
                }
                "checkchunks" -> {
                    if (!sender.hasPermission("fixlag.checkchunks")) {
                        sender.sendMessage(messageManager.getMessage("permission_denied"))
                        return true
                    }
                    chunkAnalyzer.analyzeChunks(sender)
                    return true
                }
                "checkredstone" -> {
                    if (!sender.hasPermission("fixlag.checkredstone")) {
                        sender.sendMessage(messageManager.getMessage("permission_denied"))
                        return true
                    }
                    redstoneAnalyzer.startAnalysis(sender)
                    return true
                }
                "optimizeconfig" -> {
                    if (!sender.hasPermission("fixlag.optimizeconfig")) {
                        sender.sendMessage(messageManager.getMessage("permission_denied"))
                        return true
                    }
                    if (args.size == 1) {
                        configOptimizer.analyze(sender)
                        return true
                    }
                    when (args[1].lowercase()) {
                        "accept" -> configOptimizer.applyChanges(sender, true)
                        "save-new" -> configOptimizer.applyChanges(sender, false)
                        "reject" -> configOptimizer.rejectChanges(sender)
                        else -> sender.sendMessage(messageManager.getComponentMessage("optimize_config_footer"))
                    }
                    return true
                }
                "retrieve" -> {
                    if (!sender.hasPermission("fixlag.retrieve")) {
                        sender.sendMessage(messageManager.getMessage("permission_denied"))
                        return true
                    }
                    if (sender is Player) {
                        deletedItemsManager.openChestGUI(sender)
                    } else {
                        sender.sendMessage(messageManager.getMessage("command_player_only"))
                    }
                    return true
                }
                "status" -> {
                    if (!sender.hasPermission("fixlag.status")) {
                        sender.sendMessage(messageManager.getMessage("permission_denied"))
                        return true
                    }
                    handleStatusCommand(sender)
                    return true
                }
            }
        }

        if (!sender.hasPermission("fixlag.command")) {
            sender.sendMessage(messageManager.getMessage("permission_denied"))
            return true
        }

        sender.sendMessage(messageManager.getMessage("entity_clear_manual"))
        taskManager.deleteAndAnnounce()
        return true
    }

    private fun handleStatusCommand(sender: CommandSender) {
        val fixLag = plugin as FixLag
        val config = fixLag.configManager
        val tps = Bukkit.getServer().tps
        val tps1 = tps[0].coerceAtMost(20.0)
        val tps5 = tps[1].coerceAtMost(20.0)
        val tps15 = tps[2].coerceAtMost(20.0)
        val isPanic = fixLag.panicModeManager.isPanicActive
        val frozenMobs = fixLag.panicModeManager.frozenMobCount
        val queuedItems = deletedItemsManager.deletedItemCount

        sender.sendMessage(messageManager.getComponentMessage("status_header", false))
        sender.sendMessage(messageManager.getComponentMessage("status_tps",
            "<tps1>", String.format("%.2f", tps1),
            "<tps5>", String.format("%.2f", tps5),
            "<tps15>", String.format("%.2f", tps15)))
        sender.sendMessage(messageManager.getComponentMessage("status_panic",
            "<state>", if (isPanic) "<red>ACTIVE" else "<green>Inactive",
            "<frozen>", frozenMobs.toString()))
        sender.sendMessage(messageManager.getComponentMessage("status_smart_clear",
            "<enabled>", if (config.isSmartClearEnabled) "<green>✓ Enabled" else "<gray>✗ Disabled",
            "<threshold>", config.smartClearTpsThreshold.toString()))
        sender.sendMessage(messageManager.getComponentMessage("status_recovery_queue",
            "<count>", queuedItems.toString()))
        sender.sendMessage(messageManager.getComponentMessage("status_features_header", false))

        val features = listOf(
            "Panic Mode" to config.isPanicModeEnabled,
            "Smart Clear" to config.isSmartClearEnabled,
            "Mob Stacking" to config.isMobStackingEnabled,
            "Spawner Optimizer" to config.isSpawnerOptimizerEnabled,
            "Hopper Optimizer" to config.isHopperOptimizerEnabled,
            "Collision Optimizer" to config.isCollisionOptimizerEnabled,
            "Armor Stand Optimizer" to config.isArmorStandOptimizerEnabled,
            "XP Orb Merger" to config.isXpOrbMergerEnabled,
            "Explosion Optimizer" to config.isExplosionOptimizationEnabled,
            "Dynamic Distance" to config.isDynamicDistanceEnabled,
            "Chunk Entity Limits" to config.isChunkEntityLimitsEnabled,
            "Villager Lobotomizer" to config.isVillagerLobotomizationEnabled,
            "Lag Notifications" to config.isLagNotificationsEnabled
        )
        for ((name, enabled) in features) {
            sender.sendMessage(messageManager.getComponentMessage("status_feature_entry", false,
                "<feature>", name,
                "<state>", if (enabled) "<green>✓" else "<gray>✗"))
        }
    }

    private fun handleGcInfoCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("fixlag.gcinfo")) {
            sender.sendMessage(messageManager.getMessage("permission_denied"))
            return true
        }
        sender.sendMessage(performanceMonitor.getMemoryAndGCInfo())
        return true
    }

    private fun handleServerInfoCommand(sender: CommandSender): Boolean {
        if (!sender.hasPermission("fixlag.serverinfo")) {
            sender.sendMessage(messageManager.getMessage("permission_denied"))
            return true
        }
        sender.sendMessage(performanceMonitor.getServerInfo())
        return true
    }
}
