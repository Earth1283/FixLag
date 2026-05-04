package io.github.Earth1283.fixLag.managers

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class ChunkAnalyzer(private val plugin: JavaPlugin, private val messageManager: MessageManager) {

    fun analyzeChunks(sender: CommandSender) {
        sender.sendMessage(messageManager.getMessage("chunk_analysis_started"))

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val chunkEntityCounts = mutableMapOf<ChunkSnapshotWrapper, Int>()

            Bukkit.getScheduler().runTask(plugin, Runnable {
                for (world in Bukkit.getWorlds()) {
                    for (chunk in world.loadedChunks) {
                        val entityCount = chunk.entities.size
                        if (entityCount > 0) {
                            chunkEntityCounts[ChunkSnapshotWrapper(world.name, chunk.x, chunk.z)] = entityCount
                        }
                    }
                }

                // Now sort and display async
                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    val sortedChunks = chunkEntityCounts.entries
                        .sortedByDescending { it.value }
                        .take(10)

                    sender.sendMessage(messageManager.getMessage("chunk_analysis_header"))
                    if (sortedChunks.isEmpty()) {
                        sender.sendMessage(messageManager.getMessage("chunk_analysis_no_data"))
                    } else {
                        for (entry in sortedChunks) {
                            val chunk = entry.key
                            val count = entry.value
                            sender.sendMessage(messageManager.getMessage("chunk_analysis_entry",
                                "<world>", chunk.worldName,
                                "<x>", chunk.x.toString(),
                                "<z>", chunk.z.toString(),
                                "<count>", count.toString()))
                        }
                    }
                })
            })
        })
    }

    private data class ChunkSnapshotWrapper(val worldName: String, val x: Int, val z: Int)
}
