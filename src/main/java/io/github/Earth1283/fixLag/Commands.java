package io.github.Earth1283.fixLag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixlag")) {
            // Add logic for cleaning up entities here
            sender.sendMessage("Entities cleaned up.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("stats")) {
            // Add logic for showing stats here
            sender.sendMessage("Displaying server stats.");
            return true;
        }
        return false;
    }
}
