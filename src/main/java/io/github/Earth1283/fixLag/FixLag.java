package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class FixLag extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register the command executor
        this.getCommand("fixlag").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("fixlag.clear")) {
                Map<String, Integer> removedEntities = clearEntities();
                sender.sendMessage("All specified entities have been cleared to reduce lag.");
                notifyPlayers(removedEntities); // Notify all players, not just those with fixlag.notify
            } else {
                sender.sendMessage("You do not have permission to execute this command.");
            }
            return true;
        });
    }

    public Map<String, Integer> clearEntities() {
        Map<String, Integer> entityCount = new HashMap<>();

        // Get all entities in the world and remove the specified ones
        for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
            if (entity instanceof Minecart || entity instanceof Arrow || entity instanceof Snowball ||
                    entity instanceof EnderPearl || entity instanceof TNTPrimed || entity instanceof Item) {

                // Increment the count for the removed entity type
                String entityType = entity.getType().toString();
                entityCount.put(entityType, entityCount.getOrDefault(entityType, 0) + 1);

                // Remove the entity
                entity.remove();
            }
        }
        return entityCount;
    }

    public void notifyPlayers(Map<String, Integer> removedEntities) {
        StringBuilder message = new StringBuilder("FixLag removed the following entities:");

        // Build the notification message with the entity counts
        for (Map.Entry<String, Integer> entry : removedEntities.entrySet()) {
            message.append("\n- ").append(entry.getValue()).append(" ").append(entry.getKey());
        }

        // Send the message to all players on the server
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message.toString());
        }
    }
}
