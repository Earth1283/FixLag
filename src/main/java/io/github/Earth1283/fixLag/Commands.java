package io.github.Earth1283.fixLag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.Bukkit;

import java.util.List;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixlag")) {
            int entitiesCleaned = cleanUpEntities();
            String message = "Entities cleaned up: " + entitiesCleaned;
            sender.sendMessage(message);
            if (sender instanceof org.bukkit.command.ConsoleCommandSender) {
                Bukkit.getConsoleSender().sendMessage(message); // Also log in the console
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("stats")) {
            showServerStats(sender);
            return true;
        }

        return false;
    }

    // Clean up entities and return the number of entities removed
    private int cleanUpEntities() {
        int entitiesCleaned = 0;
        int maxEntitiesPerChunk = FixLag.getInstance().getConfig().getInt("entity_cleanup.max_entities_per_chunk");
        boolean cleanAllEntities = FixLag.getInstance().getConfig().getBoolean("entity_cleanup.clean_all_entities");
        List<String> mobTypesToClean = FixLag.getInstance().getConfig().getStringList("entity_cleanup.mob_cleanup_types");

        // Loop through all worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            // Loop through all loaded chunks
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                // Get all entities in the chunk
                Entity[] entities = chunk.getEntities();

                // Check if the number of entities in the chunk exceeds the threshold
                if (entities.length > maxEntitiesPerChunk) {
                    // Loop through each entity and remove if necessary
                    for (Entity entity : entities) {
                        if (cleanAllEntities) {
                            entity.remove();
                            entitiesCleaned++;
                        } else if (entity instanceof Mob && mobTypesToClean.contains(entity.getType().toString())) {
                            entity.remove();
                            entitiesCleaned++;
                        }
                    }
                }
            }
        }

        return entitiesCleaned; // Return the number of entities cleaned
    }

    // Show server stats
    private void showServerStats(CommandSender sender) {
        boolean showMemory = FixLag.getInstance().getConfig().getBoolean("server_stats.show_memory_usage");
        boolean showEntityCount = FixLag.getInstance().getConfig().getBoolean("server_stats.show_entity_count");
        boolean showTickRate = FixLag.getInstance().getConfig().getBoolean("server_stats.show_tick_rate");

        if (showMemory) {
            displayMemoryUsage(sender);
        }
        if (showEntityCount) {
            displayEntityCount(sender);
        }
        if (showTickRate) {
            displayTickRate(sender);
        }
    }

    private void displayMemoryUsage(CommandSender sender) {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        sender.sendMessage("Memory Usage: " + (totalMemory - freeMemory) / 1024 / 1024 + " MB / " + totalMemory / 1024 / 1024 + " MB");
    }

    private void displayEntityCount(CommandSender sender) {
        int entityCount = 0;
        // Count the number of entities in all worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            entityCount += world.getEntities().size();
        }
        sender.sendMessage("Total Entity Count: " + entityCount);
    }

    private void displayTickRate(CommandSender sender) {
        double tps = Bukkit.getTPS()[0]; // Get the server's TPS
        sender.sendMessage("Server TPS: " + tps);
    }
}
