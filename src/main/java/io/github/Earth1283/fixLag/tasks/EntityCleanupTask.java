package io.github.Earth1283.fixLag.tasks;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.*;

import java.util.Collection;

public class EntityCleanupTask implements Runnable {

    private final FixLag plugin;
    private final int villagerThreshold;
    private final int itemThreshold;
    private final int tntThreshold;
    private final int minecartThreshold;
    private final int cleanupInterval;

    private int totalEntitiesCleared = 0;

    public EntityCleanupTask(FixLag plugin) {
        this.plugin = plugin;
        this.cleanupInterval = plugin.getConfig().getInt("cleanup-interval", 300);
        this.villagerThreshold = plugin.getConfig().getInt("entity-thresholds.villagers", 50);
        this.itemThreshold = plugin.getConfig().getInt("entity-thresholds.items", 100);
        this.tntThreshold = plugin.getConfig().getInt("entity-thresholds.primed-tnt", 75);
        this.minecartThreshold = plugin.getConfig().getInt("entity-thresholds.minecarts", 10);
    }

    @Override
    public void run() {
        notifyPlayers();
        checkEntityThresholds();
        Bukkit.getScheduler().runTaskLater(plugin, this::clearEntities, 10 * 20L);
    }

    private void notifyPlayers() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 3 minutes!")), (cleanupInterval - 180) * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 1 minute!")), (cleanupInterval - 60) * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 30 seconds!")), (cleanupInterval - 30) * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bEntity cleanup in 10 seconds!")), (cleanupInterval - 10) * 20L);
    }

    private void checkEntityThresholds() {
        int villagerCount = 0, itemCount = 0, tntCount = 0, minecartCount = 0;

        for (World world : Bukkit.getWorlds()) {
            villagerCount += countEntities(world, Villager.class);
            itemCount += countEntities(world, Item.class);
            tntCount += countEntities(world, TNTPrimed.class);
            minecartCount += countEntities(world, Minecart.class);
        }

        if (villagerCount > villagerThreshold || itemCount > itemThreshold || tntCount > tntThreshold || minecartCount > minecartThreshold) {
            notifyWarning(villagerCount, itemCount, tntCount, minecartCount);
        }
    }

    private void notifyWarning(int villagerCount, int itemCount, int tntCount, int minecartCount) {
        StringBuilder warningMessage = new StringBuilder(ChatColor.translateAlternateColorCodes('&', "&c[FixLag] &bWarning: Entity load threshold exceeded! "));

        if (villagerCount > villagerThreshold) warningMessage.append("&cMore than ").append(villagerThreshold).append(" Villagers loaded! ");
        if (itemCount > itemThreshold) warningMessage.append("&cMore than ").append(itemThreshold).append(" Items loaded! ");
        if (tntCount > tntThreshold) warningMessage.append("&cMore than ").append(tntThreshold).append(" TNT primed loaded! ");
        if (minecartCount > minecartThreshold) warningMessage.append("&cMore than ").append(minecartThreshold).append(" Minecarts loaded in one block! ");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fixlag.warning")) player.sendMessage(warningMessage.toString());
        }
    }

    private <T extends Entity> int countEntities(World world, Class<T> entityType) {
        return world.getEntitiesByClass(entityType).size();
    }

    public void clearEntities() {
        totalEntitiesCleared = 0;

        for (World world : Bukkit.getWorlds()) {
            totalEntitiesCleared += removeEntities(world, TNTPrimed.class);
            totalEntitiesCleared += removeEntities(world, Minecart.class);
            totalEntitiesCleared += removeEntities(world, Item.class);
        }

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&a[FixLag] &bCleared " + totalEntitiesCleared + " entities!"));
    }

    private <T extends Entity> int removeEntities(World world, Class<T> entityType) {
        Collection<T> entities = world.getEntitiesByClass(entityType);
        int removedEntities = 0;

        for (T entity : entities) {
            entity.remove();
            removedEntities++;
        }
        return removedEntities;
    }
}
