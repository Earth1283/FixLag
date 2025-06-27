package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask; // Import BukkitTask

import java.util.List;
import java.util.UUID; // Import UUID
import java.util.stream.Collectors;

public class OverloadChecker {

    private final FixLag plugin;
    private final List<String> entitiesToDelete;
    private int overloadRadius;
    private int criticalEntityCount;
    private BukkitTask checkTask; // To hold the scheduled task

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

    /**
     * Starts the periodic overload checking task.
     * This task runs on the main thread to collect necessary data (players, locations, nearby entities)
     * and then submits asynchronous tasks to process the data off the main thread.
     *
     * @param delayTicks The delay in ticks before the first execution.
     * @param periodTicks The period in ticks between subsequent executions.
     */
    public void startChecking(long delayTicks, long periodTicks) {
        // Schedule a repeating task on the main thread to collect data and submit async tasks
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Get a snapshot of online players on the main thread to avoid ConcurrentModificationException
            List<Player> onlinePlayers = List.copyOf(Bukkit.getOnlinePlayers());

            // Process each player
            for (Player player : onlinePlayers) {
                // Get necessary data on the main thread (Location, World, UUID)
                UUID playerUUID = player.getUniqueId();
                Location playerLocation = player.getLocation();
                World world = playerLocation.getWorld();

                if (world != null) {
                    // Get nearby entities on the main thread - this is a synchronous Bukkit API call
                    List<Entity> nearbyEntities = world.getNearbyEntities(playerLocation, overloadRadius, overloadRadius, overloadRadius)
                            .stream().collect(Collectors.toList());

                    // Submit an async task to perform filtering by entity type
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        // Filter entities by type asynchronously
                        List<Entity> typeFilteredEntities = nearbyEntities.stream()
                                .filter(entity -> entitiesToDelete.contains(entity.getType().name().toUpperCase()))
                                .collect(Collectors.toList());

                        // Schedule a synchronous task to perform permission filtering, counting, and notification
                        // This is necessary because permission checks and sending messages must be on the main thread.
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            // This runs on the main thread
                            Player p = Bukkit.getPlayer(playerUUID); // Get player object again on main thread
                            if (p != null && p.isOnline()) { // Check if player is still online

                                // Filter entities by permission on the main thread
                                List<Entity> permissionFilteredEntities = typeFilteredEntities.stream()
                                        .filter(entity -> {
                                            // Check if the entity is still valid/loaded before checking permission
                                            // This helps prevent potential issues if entities are removed between async and sync tasks
                                            return entity.isValid() && !entity.hasPermission("fixlag.overload.exempt");
                                        })
                                        .collect(Collectors.toList());

                                // Count the final list
                                int finalCount = permissionFilteredEntities.size();

                                // If overload detected, notify staff
                                if (finalCount > criticalEntityCount) {
                                    String message = ChatColor.RED + "Warning! High number of targeted entities (" + finalCount + ") detected near " + p.getName() + ".";
                                    // Notify staff on the main thread
                                    for (Player staff : Bukkit.getOnlinePlayers()) {
                                        // Check if staff player is still online and has permission/is op
                                        if (staff.isOnline() && (staff.isOp() || staff.hasPermission("fixlag.overload.notify"))) {
                                            staff.sendMessage(message);
                                        }
                                    }
                                }
                            }
                        });
                    });
                }
            }
        }, delayTicks, periodTicks); // Schedule the main thread collector task
    }

    /**
     * Stops the periodic overload checking task if it is currently running.
     */
    public void stopChecking() {
        if (checkTask != null && !checkTask.isCancelled()) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    // The old checkOverloads method is removed as it's replaced by the scheduled task logic.
    // ...existing code...
    // public void checkOverloads() { ... }
    // ...existing code...
}