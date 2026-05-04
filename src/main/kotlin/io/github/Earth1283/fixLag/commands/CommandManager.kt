package io.github.Earth1283.fixLag.commands

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.managers.*
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
