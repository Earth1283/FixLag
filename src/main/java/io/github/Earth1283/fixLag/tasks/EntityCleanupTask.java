// This java file is for cleaning up unwaned entities.

package io.github.Earth1283.fixLag.tasks;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.Location;

import java.util.*;

public class EntityCleanupTask implements Runnable {

    private final FixLag plugin;
    private final int villagerThreshold;
    private final int itemThreshold;
    private final int tntThreshold;
    private final int minecartThreshold;
    private final int snowballThreshold;
    private final int cleanupInterval;
    private final int warningRefreshInterval;

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
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 3 minutes!")), (cleanupInterval - 180) * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 1 minute!")), (cleanupInterval - 60) * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 30 seconds!")), (cleanupInterval - 30) * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 10 seconds!")), (cleanupInterval - 10) * 20L);
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
                minecartOverloadLocations.addAll(checkEntityClusters(world, Minecart.class, minecartThreshold, 4));
                snowballOverloadLocations.addAll(checkEntityClusters(world, Snowball.class, snowballThreshold, 4));
            }

            Bukkit.getScheduler().runTask(plugin, () -> notifyWarning(entityCounts, minecartOverloadLocations, snowballOverloadLocations));
        });
    }

    private List<String> checkEntityClusters(World world, Class<? extends Entity> entityType, int threshold, double radius) {
        List<String> overloadLocations = new ArrayList<>();

        for (Chunk chunk : world.getLoadedChunks()) {
            Entity[] entitiesArray = chunk.getEntities();  // Get an array of entities in the chunk

            // Convert the array to a List<Entity> (which is a Collection<Entity>)
            Collection<Entity> entities = Arrays.asList(entitiesArray);

            for (Entity entity : entities) {
                if (entityType.isInstance(entity)) {
                    Location loc = entity.getLocation();  // Get the Location of the entity
                    int blockX = loc.getBlockX();         // Get the X coordinate
                    int blockY = loc.getBlockY();         // Get the Y coordinate
                    int blockZ = loc.getBlockZ();         // Get the Z coordinate

                    overloadLocations.add(world.getName() + " [" + blockX + ", " + blockY + ", " + blockZ + "]");  // Add the location to the list
                }
            }
        }
        return overloadLocations;
    }

    private <T extends Entity> int countNearbyEntities(Entity center, Class<T> entityType, double radius) {
        int count = 0;
        for (Entity entity : center.getNearbyEntities(radius, radius, radius)) {
            if (entityType.isInstance(entity)) {
                count++;
            }
        }
        return count;
    }

    private void notifyWarning(Map<String, Integer> entityCounts, List<String> minecartOverloadLocations, List<String> snowballOverloadLocations) {
        StringBuilder warningMessage = new StringBuilder(ChatColor.translateAlternateColorCodes('&', "&c[FixLag] &bWarning: Entity load threshold exceeded! "));

        if (entityCounts.get("villagers") > villagerThreshold) warningMessage.append("&cMore than ").append(villagerThreshold).append(" Villagers loaded! ");
        if (entityCounts.get("items") > itemThreshold) warningMessage.append("&cMore than ").append(itemThreshold).append(" Items loaded! ");
        if (entityCounts.get("tnt") > tntThreshold) warningMessage.append("&cMore than ").append(tntThreshold).append(" TNT primed loaded! ");

        if (!minecartOverloadLocations.isEmpty()) {
            warningMessage.append("&cMinecart overload detected at: ");
            for (String loc : minecartOverloadLocations) {
                warningMessage.append("\n  - ").append(loc);
            }
        }

        if (!snowballOverloadLocations.isEmpty()) {
            warningMessage.append("&cSnowball overload detected at: ");
            for (String loc : snowballOverloadLocations) {
                warningMessage.append("\n  - ").append(loc);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fixlag.warning")) player.sendMessage(warningMessage.toString());
        }
    }

    private <T extends Entity> int countEntities(World world, Class<T> entityType) {
        Collection<T> entities = new ArrayList<>();
        for (Chunk chunk : world.getLoadedChunks()) {
            // Get all entities in the chunk
            for (Entity entity : chunk.getEntities()) {
                if (entityType.isInstance(entity)) {
                    entities.add(entityType.cast(entity));  // Safely cast to the desired type
                }
            }
        }
        return entities.size();
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

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bCleared " + totalEntitiesCleared + " entities (including arrows & snowballs)!"));
        });
    }

    private <T extends Entity> int removeEntities(World world, Class<T> entityType) {
        Collection<T> entities = new ArrayList<>();
        for (Chunk chunk : world.getLoadedChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (entityType.isInstance(entity)) {
                    entities.add(entityType.cast(entity)); // Add to the collection
                }
            }
        }

        int removed = 0;
        for (T entity : entities) {
            if (entity instanceof Entity) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }
}
