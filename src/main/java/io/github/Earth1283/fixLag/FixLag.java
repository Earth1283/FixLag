package io.github.Earth1283.fixLag;

import io.github.Earth1283.fixLag.commands.FixLagCommand;
import io.github.Earth1283.fixLag.tasks.EntityCleanupTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FixLag extends JavaPlugin {

    private static final String LATEST_VERSION_URL = "https://raw.githubusercontent.com/Earth1283/FixLag/main/latest_version.txt";

    @Override
    public void onEnable() {
        // Initialize the cleanup task
        EntityCleanupTask cleanupTask = new EntityCleanupTask(this);

        // Register the FixLag command with the cleanup task
        CommandExecutor fixLagCommandExecutor = new FixLagCommand(cleanupTask);
        getCommand("fixlag").setExecutor(fixLagCommandExecutor);

        // Load the configuration
        saveDefaultConfig();

        // Run the update checker on startup
        checkForUpdates();

        // Start the entity cleanup task to run at intervals
        startCleanupTask(cleanupTask);
    }

    @Override
    public void onDisable() {
        // No specific cleanup tasks required on disable
    }

    private void startCleanupTask(EntityCleanupTask cleanupTask) {
        int interval = getConfig().getInt("cleanup-interval", 300);
        Bukkit.getScheduler().runTaskTimer(this, cleanupTask, 0L, interval * 20L); // Run periodically
    }

    private void checkForUpdates() {
        Bukkit.getScheduler().runTask(this, () -> {
            String currentVersion = getDescription().getVersion();
            String latestVersion = fetchLatestVersion();

            if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[FixLag] Your plugin version (" + currentVersion + ") is outdated!");
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[FixLag] Please update to the latest version: https://modrinth.com/plugin/fixlag");
            }
        });
    }

    private String fetchLatestVersion() {
        StringBuilder result = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_VERSION_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[FixLag] Unable to check for updates due to an error: " + e.getMessage());
            return null;
        }

        return result.toString().trim();
    }
}
