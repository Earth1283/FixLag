package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class DynamicDistanceManager extends BukkitRunnable {

    private final FixLag plugin;
    private final ConfigManager config;
    private final MessageManager messageManager;
    private final MiniMessage miniMessage;

    public DynamicDistanceManager(FixLag plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void run() {
        if (!config.isDynamicDistanceEnabled()) return;

        double tps = Bukkit.getServer().getTPS()[0];

        for (World world : Bukkit.getWorlds()) {
            int currentView = world.getViewDistance();
            int currentSim = getSimulationDistance(world);

            int newView = currentView;
            int newSim = currentSim;

            if (tps < config.getDynamicDistanceTpsLowThreshold()) {
                // Decrease distance
                if (currentView > config.getDynamicDistanceMinView()) {
                    newView = Math.max(config.getDynamicDistanceMinView(), currentView - 1);
                }
                if (currentSim > config.getDynamicDistanceMinSim()) {
                    newSim = Math.max(config.getDynamicDistanceMinSim(), currentSim - 1);
                }
            } else if (tps > config.getDynamicDistanceTpsHighThreshold()) {
                // Increase distance
                if (currentView < config.getDynamicDistanceMaxView()) {
                    newView = Math.min(config.getDynamicDistanceMaxView(), currentView + 1);
                }
                if (currentSim < config.getDynamicDistanceMaxSim()) {
                    newSim = Math.min(config.getDynamicDistanceMaxSim(), currentSim + 1);
                }
            }

            if (newView != currentView || newSim != currentSim) {
                world.setViewDistance(newView);
                setSimulationDistance(world, newSim);

                plugin.getLogger().info(messageManager.getLogMessage("log_distance_changed",
                        "<world>", world.getName(),
                        "<tps>", String.format("%.2f", tps),
                        "<view>", String.valueOf(newView),
                        "<sim>", String.valueOf(newSim)
                ));

                if (config.isDynamicDistanceNotifyAdmins()) {
                    String msgKey = (newView < currentView || newSim < currentSim) ? "distance_decreased" : "distance_increased";
                    String rawMsg = messageManager.getRawMessage(msgKey)
                            .replace("<view>", String.valueOf(newView))
                            .replace("<sim>", String.valueOf(newSim));
                    
                    Component message = miniMessage.deserialize(messageManager.getRawMessage("prefix") + rawMsg);
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission("fixlag.notify.distance")) {
                            player.sendMessage(message);
                        }
                    }
                }
            }
        }
    }

    private int getSimulationDistance(World world) {
        try {
            // World#getSimulationDistance() was added in 1.18
            return (int) world.getClass().getMethod("getSimulationDistance").invoke(world);
        } catch (Exception e) {
            // Fallback for older versions
            return world.getViewDistance();
        }
    }

    private void setSimulationDistance(World world, int distance) {
        try {
            // World#setSimulationDistance(int) was added in 1.18
            world.getClass().getMethod("setSimulationDistance", int.class).invoke(world, distance);
        } catch (Exception ignored) {
            // Fallback for older versions - do nothing
        }
    }
}
