package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.*;

public class RedstoneAnalyzer implements Listener {

    private final FixLag plugin;
    private final MessageManager messageManager;
    private boolean isAnalyzing = false;
    private final Map<String, Integer> chunkActivity = new HashMap<>();
    private CommandSender analyzerSender;

    public RedstoneAnalyzer(FixLag plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    public void startAnalysis(CommandSender sender) {
        if (isAnalyzing) {
            sender.sendMessage("§cRedstone analysis is already in progress.");
            return;
        }

        isAnalyzing = true;
        analyzerSender = sender;
        chunkActivity.clear();
        
        // Register listener dynamically to save resources when not analyzing
        Bukkit.getPluginManager().registerEvents(this, plugin);

        sender.sendMessage("§aStarting Redstone Analysis for 10 seconds...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopAnalysis();
        }, 200L); // 10 seconds
    }

    private void stopAnalysis() {
        isAnalyzing = false;
        HandlerList.unregisterAll(this);

        if (analyzerSender == null) return;

        analyzerSender.sendMessage("§a--- Redstone Analysis Results ---");
        if (chunkActivity.isEmpty()) {
            analyzerSender.sendMessage("§eNo significant redstone activity detected.");
            return;
        }

        List<Map.Entry<String, Integer>> sortedActivity = new ArrayList<>(chunkActivity.entrySet());
        sortedActivity.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedActivity) {
            if (count >= 5) break;
            analyzerSender.sendMessage("§eChunk: §6" + entry.getKey() + " §e- Updates: §c" + entry.getValue());
            count++;
        }
        analyzerSender.sendMessage("§a---------------------------------");
    }

    private void recordActivity(Chunk chunk) {
        if (!isAnalyzing) return;
        String key = chunk.getWorld().getName() + " [" + chunk.getX() + ", " + chunk.getZ() + "]";
        chunkActivity.put(key, chunkActivity.getOrDefault(key, 0) + 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstone(BlockRedstoneEvent event) {
        recordActivity(event.getBlock().getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        recordActivity(event.getBlock().getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        recordActivity(event.getBlock().getChunk());
    }
}
