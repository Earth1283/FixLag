// This java file is for cleaning up unwaned entities.

package io.github.Earth1283.fixLag.tasks;

import io.github.Earth1283.fixLag.FixLag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityCleanupTask implements Runnable {

    private final FixLag plugin;
    private final int villagerThreshold;
    private final int itemThreshold;
    private final int tntThreshold;
    private final int minecartThreshold;
    private final int snowballThreshold;
    private final int cleanupInterval;
    private final int warningRefreshInterval;
    private final double clusterRadius; // Using radius for cluster check
    private final int clusterThreshold; // Using threshold for cluster check

    private int totalEntitiesCleared = 0;

    public EntityCleanupTask(FixLag plugin) {
        this.plugin = plugin;
        this.cleanupInterval = plugin.getConfig().getInt("cleanup-interval", 300);
        this.warningRefreshInterval = plugin.getConfig().getInt("warning-refresh-interval", 60);
        this.villagerThreshold = plugin.getConfig().getInt("entity-thresholds.villagers", 50);
        this.itemThreshold = plugin.getConfig().getInt("entity-thresholds.items", 100);
        this.tntThreshold = plugin.getConfig().getInt("entity-thresholds.primed-tnt", 75);
        this.minecartThreshold = plugin.getConfig().getInt("entity-thresholds.minecarts", 10);
        this.snowballThreshold = plugin.getConfig().getInt("entity-thresholds.snowballs", 50);
        this.clusterRadius = 4.0; // Radius for entity cluster check, set to 4 as per your code, and making it configurable if needed.
        this.clusterThreshold = minecartThreshold; // Threshold for entity cluster, currently using minecartThreshold as a placeholder, configure if needed.


        // Schedule warning refresh separately
        startWarningRefreshTask();
    }

    @Override
    public void run() {
        notifyPlayers();
        Bukkit.getScheduler().runTaskLater(plugin, this::clearEntities, 10 * 20L);
    }

    private void startWarningRefreshTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkEntityThresholds, 0L, warningRefreshInterval * 20L);
    }

    private void notifyPlayers() {
        int[] warningTimes = {180, 60, 30, 10}; // Warning times in seconds
        for (int time : warningTimes) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> broadcastCleanupMessage(time), (cleanupInterval - time) * 20L);
        }
    }

    private void broadcastCleanupMessage(int time) {
        String message = "&a[FixLag] &bEntity cleanup in " + (time >= 60 ? time / 60 + " minute" + (time >= 120 ? "s" : "") : time + " seconds") + "!";
        Bukkit.getServer().broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }


    private void checkEntityThresholds() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Integer> entityCounts = new HashMap<>();
            List<String> minecartOverloadLocations = new ArrayList<>();
            List<String> snowballOverloadLocations = new ArrayList<>();

            for (World world : Bukkit.getWorlds()) {
                entityCounts.put("villagers", countEntities(world, Villager.class));
                entityCounts.put("items", countEntities(world, Item.class));
                entityCounts.put("tnt", countEntities(world, TNTPrimed.class));

                // Check for clustered minecarts and snowballs
                minecartOverloadLocations.addAll(checkEntityClusters(world, Minecart.class));
                snowballOverloadLocations.addAll(checkEntityClusters(world, Snowball.class));
            }

            Bukkit.getScheduler().runTask(plugin, () -> notifyWarning(entityCounts, minecartOverloadLocations, snowballOverloadLocations));
        });
    }

    private List<String> checkEntityClusters(World world, Class<? extends Entity> entityType) {
        List<String> overloadLocations = new ArrayList<>();

        for (Chunk chunk : world.getLoadedChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (entityType.isInstance(entity)) {
                    if (countNearbyEntities(entity, entityType, clusterRadius) > clusterThreshold) { // Using configured radius and threshold
                        overloadLocations.add(formatLocation(world.getName(), entity.getLocation()));
                    }
                }
            }
        }
        return overloadLocations;
    }

    private <T extends Entity> int countNearbyEntities(Entity center, Class<T> entityType, double radius) {
        int count = 0;
        for (Entity entity : center.getNearbyEntities(radius, radius, radius)) {
            if (entityType.isInstance(entity) && entity != center) { // Exclude the center entity itself
                count++;
            }
        }
        return count;
    }

    private void notifyWarning(Map<String, Integer> entityCounts, List<String> minecartOverloadLocations, List<String> snowballOverloadLocations) {
        StringBuilder warningMessage = new StringBuilder(LegacyComponentSerializer.legacyAmpersand().deserialize("&c[FixLag] &bWarning: Entity load threshold exceeded! ").content()); // Use Adventure Component for base message

        if (entityCounts.get("villagers") > villagerThreshold) warningMessage.append(LegacyComponentSerializer.legacyAmpersand().deserialize("&cMore than ").content()).append(villagerThreshold).append(LegacyComponentSerializer.legacyAmpersand().deserialize(" Villagers loaded! ").content());
        if (entityCounts.get("items") > itemThreshold) warningMessage.append(LegacyComponentSerializer.legacyAmpersand().deserialize("&cMore than ").content()).append(itemThreshold).append(LegacyComponentSerializer.legacyAmpersand().deserialize(" Items loaded! ").content());
        if (entityCounts.get("tnt") > tntThreshold) warningMessage.append(LegacyComponentSerializer.legacyAmpersand().deserialize("&cMore than ").content()).append(tntThreshold).append(LegacyComponentSerializer.legacyAmpersand().deserialize(" TNT primed loaded! ").content());

        if (!minecartOverloadLocations.isEmpty()) {
            warningMessage.append(LegacyComponentSerializer.legacyAmpersand().deserialize("&cMinecart overload detected at: ").content());
            for (String loc : minecartOverloadLocations) {
                warningMessage.append("\n  - ").append(loc);
            }
        }

        if (!snowballOverloadLocations.isEmpty()) {
            warningMessage.append(LegacyComponentSerializer.legacyAmpersand().deserialize("&cSnowball overload detected at: ").content());
            for (String loc : snowballOverloadLocations) {
                warningMessage.append("\n  - ").append(loc);
            }
        }

        Component finalWarningMessage = LegacyComponentSerializer.legacySection().deserialize(warningMessage.toString()); // Deserialize the complete message once

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fixlag.warning")) player.sendMessage(finalWarningMessage); // Use sendMessage(Component)
        }
    }

    private <T extends Entity> int countEntities(World world, Class<T> entityType) {
        int count = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (entityType.isInstance(entity)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void clearEntities() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            totalEntitiesCleared = 0;

            for (World world : Bukkit.getWorlds()) {
                totalEntitiesCleared += removeEntities(world, TNTPrimed.class);
                totalEntitiesCleared += removeEntities(world, Minecart.class);
                totalEntitiesCleared += removeEntities(world, Item.class);
                totalEntitiesCleared += removeEntities(world, Arrow.class);
                totalEntitiesCleared += removeEntities(world, Snowball.class);
            }

            String broadcastMessage = "&a[FixLag] &bCleared " + totalEntitiesCleared + " entities!";
            Bukkit.getServer().broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(broadcastMessage));
        });
    }

    private <T extends Entity> int removeEntities(World world, Class<T> entityType) {
        List<Entity> entitiesToRemove = new ArrayList<>();
        for (Chunk chunk : world.getLoadedChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (entityType.isInstance(entity)) {
                    entitiesToRemove.add(entity);
                }
            }
        }

        int removed = 0;
        for (Entity entity : entitiesToRemove) {
            entity.remove();
            removed++;
        }
        return removed;
    }

    private String formatLocation(String worldName, Location loc) {
        return worldName + " [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";
    }
}