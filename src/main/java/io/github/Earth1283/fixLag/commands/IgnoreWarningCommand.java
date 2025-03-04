// idk i gave up with the main FixLagCommand.java file and made a helper file?
// I did not intend this lol

package io.github.Earth1283.fixLag.commands;

import io.github.Earth1283.fixLag.tasks.TPSWarningTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class IgnoreWarningCommand implements CommandExecutor {

    private final TPSWarningTask tpsWarningTask;

    public IgnoreWarningCommand(TPSWarningTask tpsWarningTask) {
        this.tpsWarningTask = tpsWarningTask;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Check if the player is ignoring warnings or not
            if (tpsWarningTask.isIgnoring(player)) {
                tpsWarningTask.unignoreWarning(player);
                player.sendMessage(ChatColor.GREEN + "You will now receive TPS warnings.");
            } else {
                tpsWarningTask.ignoreWarning(player);
                player.sendMessage(ChatColor.RED + "You are now ignoring TPS warnings.");
            }

            return true;
        }
        return false;
    }
}
