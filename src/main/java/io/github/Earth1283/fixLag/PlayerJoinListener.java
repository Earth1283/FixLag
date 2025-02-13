package io.github.Earth1283.fixLag;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {

    private final FixLag plugin;

    public PlayerJoinListener(FixLag plugin) {
        this.plugin = plugin;
    }
// Nag admins to update
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && plugin.isUpdateAvailable()) {
            player.sendMessage(ChatColor.RED + "[FixLag] A new update is available! Get the latest version at: https://modrinth.com/plugin/fixlag");
        }
    }
}
