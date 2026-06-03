package io.github.Earth1283.fixLag.managers

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class ChunkAnalyzer(private val plugin: JavaPlugin, private val messageManager: MessageManager) {

    fun analyzeChunks(sender: CommandSender) {
        sender.sendMessage(messageManager.getMessage("chunk_analysis_started"))

        // Entity access (chunk.entities) must happen on the main thread.
        // Defer one tick so the "started" message renders first, then collect and display.
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val results = mutableListOf<Pair<ChunkSnapshotWrapper, Int>>()
            for (world in Bukkit.getWorlds()) {
                for (chunk in world.loadedChunks) {
                    val count = chunk.entities.size
                    if (count > 0) {
                        results.add(Pair(ChunkSnapshotWrapper(world.name, chunk.x, chunk.z), count))
                    }
                }
            }

            val sortedChunks = results.sortedByDescending { it.second }.take(10)

            sender.sendMessage(messageManager.getMessage("chunk_analysis_header"))
            if (sortedChunks.isEmpty()) {
                sender.sendMessage(messageManager.getMessage("chunk_analysis_no_data"))
            } else {
                for ((chunk, count) in sortedChunks) {
                    sender.sendMessage(messageManager.getMessage("chunk_analysis_entry",
                        "<world>", chunk.worldName,
                        "<x>", chunk.x.toString(),
                        "<z>", chunk.z.toString(),
                        "<count>", count.toString()))
                }
            }
        })
    }

    private data class ChunkSnapshotWrapper(val worldName: String, val x: Int, val z: Int)
}
