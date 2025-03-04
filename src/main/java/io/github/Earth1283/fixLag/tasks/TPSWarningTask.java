// Warn admins when the server is on potato mode

package io.github.Earth1283.fixLag.tasks;

import io.github.Earth1283.fixLag.FixLag;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class TPSWarningTask extends BukkitRunnable {

    private final FixLag plugin;
    private boolean isWarningRed = true;

    // A set to track ignored players
    private Set<String> ignoredPlayers = new HashSet<>();

    public TPSWarningTask(FixLag plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Get TPS
        double tps = Bukkit.getServer().getTPS()[0]; // Get the 1-minute TPS value

        // If TPS is below 18, send a warning message
        if (tps < 18) {
            String warningMessage = ChatColor.RED + "Warning: TPS " + String.format("%.2f", tps) + "!";

            // Toggle between red and yellow color
            if (!isWarningRed) {
                warningMessage = ChatColor.YELLOW + "Warning: TPS " + String.format("%.2f", tps) + "!";
            }

            // Toggle the color for the next warning
            isWarningRed = !isWarningRed;

            // Send the warning to players with the permission "fixlag.tpswarn"
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("fixlag.tpswarn") && !ignoredPlayers.contains(player.getName())) {
                    TextComponent textComponent = new TextComponent(warningMessage);
                    textComponent.setBold(true);  // Make the warning bold
                    player.spigot().sendMessage(textComponent); // Send to the action bar
                }
            }
        }
    }

    // Method to add a player to the ignored list
    public void ignoreWarning(Player player) {
        ignoredPlayers.add(player.getName());
    }

    // Method to remove a player from the ignored list
    public void unignoreWarning(Player player) {
        ignoredPlayers.remove(player.getName());
    }

    // Check if a player is ignoring the warning
    public boolean isIgnoring(Player player) {
        return ignoredPlayers.contains(player.getName());
    }
}
