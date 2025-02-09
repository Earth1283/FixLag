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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            // Only allow OPs to run this command
            if (player.isOp()) {
                cleanupTask.clearEntities();  // Run the cleanup manually
                player.sendMessage("§a[FixLag] §bManually triggered entity cleanup.");
                return true;
            } else {
                player.sendMessage("§cYou don't have permission to use this command.");
                return false;
            }
        } else {
            cleanupTask.clearEntities();
            Bukkit.getConsoleSender().sendMessage("§a[FixLag] §bManually triggered entity cleanup.");
            return true;
        }
    }
}
