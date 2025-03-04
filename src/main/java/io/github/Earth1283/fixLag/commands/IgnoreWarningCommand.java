// idk i gave up with the main FixLagCommand.java file and made a helper file?
// I did not intend this lol

package io.github.Earth1283.fixLag.commands;

import io.github.Earth1283.fixLag.tasks.TPSWarningTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import javax.annotation.Nonnull;

public class IgnoreWarningCommand implements CommandExecutor {

    private final TPSWarningTask tpsWarningTask;

    public IgnoreWarningCommand(TPSWarningTask tpsWarningTask) {
        this.tpsWarningTask = tpsWarningTask;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (sender instanceof Player player) {

            // Check if the player is ignoring warnings or not
            if (tpsWarningTask.isIgnoring(player)) {
                tpsWarningTask.unignoreWarning(player);
                player.sendMessage(Component.text("You will now receive TPS warnings.").color(NamedTextColor.GREEN));
            } else {
                tpsWarningTask.ignoreWarning(player);
                player.sendMessage(Component.text("You are now ignoring TPS warnings.").color(NamedTextColor.RED));
            }

            return true;
        }
        return false;
    }
}