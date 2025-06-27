package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

public class OverloadChecker {

    private final FixLag plugin;
    private final List<String> entitiesToDelete;
    private int overloadRadius;
    private int criticalEntityCount;

    public OverloadChecker(FixLag plugin, List<String> entitiesToDelete) {
        this.plugin = plugin;
        this.entitiesToDelete = entitiesToDelete;
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        overloadRadius = config.getInt("overload-detection.radius", 50);
        criticalEntityCount = config.getInt("overload-detection.critical-entity-count", 30);
    }

    public void checkOverloads() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLocation = player.getLocation();
            World world = playerLocation.getWorld();
            if (world != null) { // check all worlds by default
                List<Entity> nearbyEntities = world.getNearbyEntities(playerLocation, overloadRadius, overloadRadius, overloadRadius).stream()
                        .filter(entity -> entitiesToDelete.contains(entity.getType().name().toUpperCase()))
                        .filter(entity -> !entity.hasPermission("fixlag.overload.exempt"))
                        .collect(Collectors.toList());

                if (nearbyEntities.size() > criticalEntityCount) {
                    String message = ChatColor.RED + "Warning! High number of targeted entities (" + nearbyEntities.size() + ") detected near you.";
                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (staff.isOp() || staff.hasPermission("fixlag.overload.notify")) {
                            staff.sendMessage(message);
                        }
                    }
                }
            }
        }
    }
}