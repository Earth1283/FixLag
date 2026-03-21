package io.github.Earth1283.fixLag.listeners;

import io.github.Earth1283.fixLag.FixLag;
import io.github.Earth1283.fixLag.managers.ConfigManager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class ArmorStandOptimizer implements Listener {

    private final FixLag plugin;
    private final ConfigManager config;

    public ArmorStandOptimizer(FixLag plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!config.isArmorStandOptimizerEnabled()) return;

        if (event.getEntity() instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) event.getEntity();
            
            if (config.isArmorStandDisableGravity()) {
                stand.setGravity(false);
            }
            if (config.isArmorStandDisableCollisions()) {
                stand.setCollidable(false);
            }
        }
    }
}
