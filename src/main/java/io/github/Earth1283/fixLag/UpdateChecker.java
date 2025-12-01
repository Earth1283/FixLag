package io.github.Earth1283.fixLag;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final Gson gson = new Gson();

    public UpdateChecker(JavaPlugin plugin, ConfigManager configManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    public void startUpdateCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String latestVersion = getLatestVersion();
                    if (latestVersion != null) {
                        if (!plugin.getDescription().getVersion().equals(latestVersion)) {
                            plugin.getLogger().log(Level.INFO, messageManager.getLogMessage("log_update_available", "%fixlag_version%", latestVersion));
                            notifyUpdate(latestVersion);
                        } else {
                            plugin.getLogger().log(Level.INFO, messageManager.getLogMessage("log_update_uptodate"));
                        }
                    } else {
                        plugin.getLogger().log(Level.WARNING, messageManager.getLogMessage("log_update_check_failed"));
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, messageManager.getLogMessage("log_update_check_error", "%fixlag_error%", e.getMessage()));
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, configManager.getUpdateCheckIntervalTicks());
    }

    private String getLatestVersion() throws IOException {
        URL url = new URL(configManager.getUpdateUrl());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            JsonArray versions = gson.fromJson(reader, JsonArray.class);
            if (versions != null && !versions.isJsonNull() && versions.size() > 0) {
                JsonElement latestVersionElement = versions.get(0);
                if (latestVersionElement.isJsonObject()) {
                    JsonObject latestVersionObject = latestVersionElement.getAsJsonObject();
                    if (latestVersionObject.has("version_number")) {
                        return latestVersionObject.get("version_number").getAsString();
                    }
                }
            }
            return null;
        }
    }

    private void notifyUpdate(String latestVersion) {
        String message = messageManager.getMessage("update_available", "%fixlag_latest_version%", latestVersion) +
                messageManager.getMessage("update_current_version", "%fixlag_current_version%", plugin.getDescription().getVersion());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission("fixlag.notify.update")) {
                player.sendMessage(message);
            }
        }
    }
}
