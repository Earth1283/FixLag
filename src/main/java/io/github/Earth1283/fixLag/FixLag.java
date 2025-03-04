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

    // Get latest version
    private static final String LATEST_VERSION_URL = "https://raw.githubusercontent.com/Earth1283/FixLag/main/latest_version.txt";
    // someone help me i walys forget this XD
    @Override
    public void onEnable() {
        // Initialize the cleanup task
        EntityCleanupTask cleanupTask = new EntityCleanupTask(this);

        // Register the FixLag command with the cleanup task
        CommandExecutor fixLagCommandExecutor = new FixLagCommand(cleanupTask);
        getCommand("fixlag").setExecutor(fixLagCommandExecutor);

        // Load the configuration
        saveDefaultConfig();

        // Run the update checker asynchronously
        checkForUpdates();

        // Start the entity cleanup task
        startCleanupTask(cleanupTask);

        // Notify OPs of updates when they join
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    @Override
    public void onDisable() {
        // No specific cleanup tasks required on disable
    }

    private void startCleanupTask(EntityCleanupTask cleanupTask) {
        int interval = getConfig().getInt("cleanup-interval", 300);
        // Correct scheduling method for periodic execution
        Bukkit.getScheduler().runTaskTimer(this, cleanupTask, 0L, interval * 20L);
    }

    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String currentVersion = getDescription().getVersion();
            String latestVersion = fetchLatestVersion();

            if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[FixLag] A new version (" + latestVersion + ") is available! You're running version " + currentVersion + ".");
                Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[FixLag] Download the latest version here: https://modrinth.com/plugin/fixlag");
            }
        });
    }

    private String fetchLatestVersion() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_VERSION_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String latestVersion = reader.readLine();
            reader.close();

            return latestVersion != null ? latestVersion.trim() : null;
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[FixLag] Unable to check for updates: " + e.getMessage());
            return null;
        }
    }

    public boolean isUpdateAvailable() {
        String currentVersion = getDescription().getVersion();
        String latestVersion = fetchLatestVersion();
        return latestVersion != null && !currentVersion.equals(latestVersion);
    }
}
