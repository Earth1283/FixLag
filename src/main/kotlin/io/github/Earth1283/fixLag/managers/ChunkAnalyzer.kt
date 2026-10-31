package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.utils.EntityScan
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.util.PriorityQueue

class ChunkAnalyzer(private val plugin: JavaPlugin, private val messageManager: MessageManager) {
    private data class Key(val world: String, val x: Int, val z: Int)
    private val scan = EntityScan(plugin)

    fun analyzeChunks(sender: CommandSender) {
        if (scan.isRunning) {
            sender.sendMessage("§eChunk analysis is already in progress.")
            return
        }
        sender.sendMessage(messageManager.getMessage("chunk_analysis_started"))
        val counts = mutableMapOf<Key, Int>()
        scan.start(visit = { entity ->
            val loc = entity.location
            val key = Key(entity.world.name, loc.blockX shr 4, loc.blockZ shr 4)
            counts[key] = counts.getOrDefault(key, 0) + 1
        }, complete = {
            val top = PriorityQueue<Map.Entry<Key, Int>>(compareBy { it.value })
            for (entry in counts.entries) {
                top.offer(entry)
                if (top.size > 10) top.poll()
            }
            sender.sendMessage(messageManager.getMessage("chunk_analysis_header"))
            if (top.isEmpty()) sender.sendMessage(messageManager.getMessage("chunk_analysis_no_data"))
            else for ((chunk, count) in top.sortedByDescending { it.value }) {
                sender.sendMessage(messageManager.getMessage("chunk_analysis_entry",
                    "<world>", chunk.world, "<x>", chunk.x.toString(), "<z>", chunk.z.toString(), "<count>", count.toString()))
            }
        })
    }
}
