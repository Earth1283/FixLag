package io.github.Earth1283.fixLag;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.ChatColor;

public class PlayerJoinListener implements Listener {

    private final FixLag plugin;

    public PlayerJoinListener(FixLag plugin) {
        this.plugin = plugin;
    }
// Nag them to update
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.isUpdateAvailable()) {
            event.getPlayer().sendMessage(ChatColor.RED + "[FixLag] A new version of FixLag is available!");
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Download the latest version here: https://modrinth.com/plugin/fixlag");
        }
    }
}
