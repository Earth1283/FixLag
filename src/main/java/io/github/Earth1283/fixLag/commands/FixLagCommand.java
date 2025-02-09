package io.github.Earth1283.fixLag.commands;

import io.github.Earth1283.fixLag.tasks.EntityCleanupTask;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FixLagCommand implements CommandExecutor {

    private final EntityCleanupTask cleanupTask;

    public FixLagCommand(EntityCleanupTask cleanupTask) {
        this.cleanupTask = cleanupTask;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If the sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Check if the player has the 'fixlag.command' permission node
            if (player.hasPermission("fixlag.command")) {
                cleanupTask.clearEntities();  // Run the cleanup manually
                player.sendMessage("§a[FixLag] §bManually triggered entity cleanup.");
                return true;
            } else {
                player.sendMessage("§cYou don't have permission to use this command.");
                return false;
            }
        } else {
            // Console can always run the command
            cleanupTask.clearEntities();
            Bukkit.getConsoleSender().sendMessage("§a[FixLag] §bManually triggered entity cleanup.");
            return true;
        }
    }
}
