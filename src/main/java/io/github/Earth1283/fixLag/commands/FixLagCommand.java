// Command permissions management & default responces

package io.github.Earth1283.fixLag.commands;

import io.github.Earth1283.fixLag.tasks.EntityCleanupTask;
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
        // Only allow players with the right permissions to execute this command
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("fixlag.cleanup")) {
                player.sendMessage("You don't have permission to use this command.");
                return false;
            }
        }

        // If no arguments, clear entities immediately
        if (args.length == 0) {
            cleanupTask.clearEntities();
            sender.sendMessage("Entity cleanup started.");
            return true;
        }

        // Handle other arguments (e.g., force cleanup, show info, etc.)
        if (args.length == 1 && args[0].equalsIgnoreCase("force")) {
            cleanupTask.clearEntities();
            sender.sendMessage("Force cleanup initiated.");
            return true;
        }

        // Invalid command usage
        return false;
    }
}
