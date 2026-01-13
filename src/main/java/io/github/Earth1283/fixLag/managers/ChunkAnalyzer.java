package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class ChunkAnalyzer {

    private final JavaPlugin plugin;
    private final MessageManager messageManager;

    public ChunkAnalyzer(JavaPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    public void analyzeChunks(CommandSender sender) {
        sender.sendMessage(messageManager.getMessage("chunk_analysis_started"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<ChunkSnapshotWrapper, Integer> chunkEntityCounts = new HashMap<>();

            // Collect data (Must be done on main thread for Bukkit API safety, but we can iterate worlds)
            // Actually, accessing loaded chunks and entities usually requires main thread.
            // So we will schedule a sync task to get the data, then process async.
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (World world : Bukkit.getWorlds()) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        // We count all entities, including tile entities if needed, but usually entities are the lag cause.
                        // Using getEntities().length might be expensive if many chunks.
                        // But getting loaded chunks is fast.
                        int entityCount = chunk.getEntities().length;
                        if (entityCount > 0) {
                            chunkEntityCounts.put(new ChunkSnapshotWrapper(world.getName(), chunk.getX(), chunk.getZ()), entityCount);
                        }
                    }
                }

                // Now sort and display async
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    List<Map.Entry<ChunkSnapshotWrapper, Integer>> sortedChunks = chunkEntityCounts.entrySet().stream()
                            .sorted(Map.Entry.<ChunkSnapshotWrapper, Integer>comparingByValue().reversed())
                            .limit(10)
                            .collect(Collectors.toList());

                    sender.sendMessage(messageManager.getMessage("chunk_analysis_header"));
                    if (sortedChunks.isEmpty()) {
                        sender.sendMessage(messageManager.getMessage("chunk_analysis_no_data"));
                    } else {
                        for (Map.Entry<ChunkSnapshotWrapper, Integer> entry : sortedChunks) {
                            ChunkSnapshotWrapper chunk = entry.getKey();
                            int count = entry.getValue();
                            sender.sendMessage(messageManager.getMessage("chunk_analysis_entry", 
                                "<world>", chunk.worldName, 
                                "<x>", String.valueOf(chunk.x), 
                                "<z>", String.valueOf(chunk.z), 
                                "<count>", String.valueOf(count)));
                        }
                    }
                });
            });
        });
    }

    // Simple wrapper to hold chunk info without keeping the actual Chunk object (to be safe async)
    private static class ChunkSnapshotWrapper {
        final String worldName;
        final int x;
        final int z;

        ChunkSnapshotWrapper(String worldName, int x, int z) {
            this.worldName = worldName;
            this.x = x;
            this.z = z;
        }
    }
}
