package io.github.Earth1283.fixLag;

import org.bukkit.plugin.java.JavaPlugin;
import io.github.Earth1283.fixLag.Commands;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class FixLag extends JavaPlugin {

    private static FixLag instance;

    @Override
    public void onEnable() {
        instance = this;

        // Register commands
        getCommand("fixlag").setExecutor(new Commands());
        getCommand("stats").setExecutor(new Commands());

        // Initialize entity cleanup task
        if (getConfig().getBoolean("entity_cleanup.enabled")) {
            new EntityCleaner().startCleanupTask();
        }

        // Version check
        if (getConfig().getBoolean("update_check.enabled")) {
            checkForUpdate();
        }

        // Save default config if not already present
        saveDefaultConfig();
    }

    public static FixLag getInstance() {
        return instance;
    }

    private void checkForUpdate() {
        getServer().getScheduler().runTask(this, () -> {
            try {
                String latestVersion = fetchLatestVersion();
                String currentVersion = getDescription().getVersion();

                if (!currentVersion.equals(latestVersion)) {
                    getLogger().warning("A new version of FixLag is available! Current version: " + currentVersion + ", Latest version: " + latestVersion);
                } else {
                    getLogger().info("FixLag is up-to-date!");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to check for plugin updates.");
            }
        });
    }

    private String fetchLatestVersion() throws Exception {
        URL url = new URL("https://raw.githubusercontent.com/Earth1283/FixLag/main/latest_version.txt");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String latestVersion = reader.readLine();
        reader.close();

        return latestVersion;
    }
}
