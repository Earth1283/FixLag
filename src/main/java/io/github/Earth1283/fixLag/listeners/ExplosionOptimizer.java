package io.github.Earth1283.fixLag.listeners;

import io.github.Earth1283.fixLag.FixLag;
import io.github.Earth1283.fixLag.managers.ConfigManager;
import io.github.Earth1283.fixLag.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public class ExplosionOptimizer implements Listener {

    private final FixLag plugin;
    private final ConfigManager config;
    private final MessageManager messageManager;

    public ExplosionOptimizer(FixLag plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        optimizeExplosion(event.blockList(), event.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        optimizeExplosion(event.blockList(), event.getBlock().getLocation());
    }

    private void optimizeExplosion(List<Block> blockList, org.bukkit.Location location) {
        if (!config.isExplosionOptimizationEnabled()) return;

        double tps = Bukkit.getServer().getTPS()[0];
        if (tps > config.getExplosionOptimizationTpsThreshold()) return;

        int originalSize = blockList.size();
        boolean disabledDrops = false;

        // Limit block destruction
        if (config.getExplosionOptimizationMaxBlocksLimit() > 0 && blockList.size() > config.getExplosionOptimizationMaxBlocksLimit()) {
            blockList.subList(config.getExplosionOptimizationMaxBlocksLimit(), blockList.size()).clear();
        }

        // Disable drops
        if (config.isExplosionOptimizationDisableDrops()) {
            for (Block block : blockList) {
                // By setting yield to 0 or handling it via event. In Bukkit, for EntityExplodeEvent,
                // we can't easily set yield per block without manually breaking them.
                // But we can use event.setYield(0) if it was just EntityExplodeEvent.
                // Since we are modifying the blockList, we might need a different approach for drops.
            }
            // For EntityExplodeEvent and BlockExplodeEvent, we can set yield:
            // But yield is not available on the event directly in all versions or is applied to all.
            // Let's use a trick: if we want NO drops, we can just clear the block list and break them manually without drops,
            // or better, if the API supports it, set yield.
        }

        // Note: EntityExplodeEvent#setYield(float) exists.
        // We will handle yield in the specific event handlers if needed, but for now let's just limit the list.
        
        plugin.getLogger().info(messageManager.getLogMessage("log_explosion_optimized",
                "<world>", location.getWorld().getName(),
                "<x>", String.valueOf(location.getBlockX()),
                "<y>", String.valueOf(location.getBlockY()),
                "<z>", String.valueOf(location.getBlockZ()),
                "<tps>", String.format("%.2f", tps),
                "<count>", String.valueOf(blockList.size()),
                "<drops>", String.valueOf(config.isExplosionOptimizationDisableDrops())
        ));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplodeYield(EntityExplodeEvent event) {
        if (config.isExplosionOptimizationEnabled() && config.isExplosionOptimizationDisableDrops()) {
            double tps = Bukkit.getServer().getTPS()[0];
            if (tps <= config.getExplosionOptimizationTpsThreshold()) {
                event.setYield(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplodeYield(BlockExplodeEvent event) {
        if (config.isExplosionOptimizationEnabled() && config.isExplosionOptimizationDisableDrops()) {
            double tps = Bukkit.getServer().getTPS()[0];
            if (tps <= config.getExplosionOptimizationTpsThreshold()) {
                event.setYield(0);
            }
        }
    }
}
