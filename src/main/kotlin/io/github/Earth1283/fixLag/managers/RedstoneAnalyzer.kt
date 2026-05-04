package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockRedstoneEvent

class RedstoneAnalyzer(private val plugin: FixLag) : Listener {

    private val messageManager: MessageManager = plugin.messageManager
    private var isAnalyzing = false
    private val chunkActivity = mutableMapOf<String, Int>()
    private var analyzerSender: CommandSender? = null

    fun startAnalysis(sender: CommandSender) {
        if (isAnalyzing) {
            sender.sendMessage("§cRedstone analysis is already in progress.")
            return
        }

        isAnalyzing = true
        analyzerSender = sender
        chunkActivity.clear()

        // Register listener dynamically to save resources when not analyzing
        Bukkit.getPluginManager().registerEvents(this, plugin)

        sender.sendMessage("§aStarting Redstone Analysis for 10 seconds...")

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            stopAnalysis()
        }, 200L) // 10 seconds
    }

    private fun stopAnalysis() {
        isAnalyzing = false
        HandlerList.unregisterAll(this)

        val sender = analyzerSender ?: return

        sender.sendMessage("§a--- Redstone Analysis Results ---")
        if (chunkActivity.isEmpty()) {
            sender.sendMessage("§eNo significant redstone activity detected.")
            return
        }

        val sortedActivity = chunkActivity.entries.sortedByDescending { it.value }

        sortedActivity.take(5).forEach { entry ->
            sender.sendMessage("§eChunk: §6${entry.key} §e- Updates: §c${entry.value}")
        }
        sender.sendMessage("§a---------------------------------")
    }

    private fun recordActivity(chunk: Chunk) {
        if (!isAnalyzing) return
        val key = "${chunk.world.name} [${chunk.x}, ${chunk.z}]"
        chunkActivity[key] = chunkActivity.getOrDefault(key, 0) + 1
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRedstone(event: BlockRedstoneEvent) {
        recordActivity(event.block.chunk)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        recordActivity(event.block.chunk)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        recordActivity(event.block.chunk)
    }
}
