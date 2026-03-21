package io.github.Earth1283.fixLag.listeners;

import io.github.Earth1283.fixLag.FixLag;
import io.github.Earth1283.fixLag.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public class SpawnerOptimizer implements Listener {

    private final FixLag plugin;
    private final ConfigManager config;

    public SpawnerOptimizer(FixLag plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (!config.isSpawnerOptimizerEnabled()) return;

        double tps = Bukkit.getServer().getTPS()[0];
        if (tps < config.getSpawnerOptimizerTpsThreshold()) {
            event.setCancelled(true);
        }
    }
}
