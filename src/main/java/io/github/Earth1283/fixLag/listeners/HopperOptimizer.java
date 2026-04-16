package io.github.Earth1283.fixLag.listeners;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.concurrent.ThreadLocalRandom;

public class HopperOptimizer implements Listener {

    private final FixLag plugin;

    public HopperOptimizer(FixLag plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (!plugin.getConfigManager().isHopperOptimizerEnabled()) {
            return;
        }

        if (event.getSource().getType() != InventoryType.HOPPER) {
            return;
        }

        double tps = Bukkit.getServer().getTPS()[0];
        if (tps < plugin.getConfigManager().getHopperOptimizerTpsThreshold()) {
            double chance = plugin.getConfigManager().getHopperOptimizerCancelChance();
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                event.setCancelled(true);
            }
        }
    }
}
