package io.github.Earth1283.fixLag;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {

    private final JavaPlugin plugin;
    private Set<String> entitiesToDelete;
    private long deletionIntervalTicks;
    private boolean enableWarning;
    private long warningTimeTicks;
    private long overloadCheckIntervalTicks;
    private boolean logMemoryStats;
    private long updateCheckIntervalTicks;
    private String updateUrl;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        plugin.reloadConfig();

        entitiesToDelete = new HashSet<>(config.getStringList("entities-to-delete"));
        deletionIntervalTicks = config.getLong("deletion-interval-seconds", 60) * 20L;
        enableWarning = config.getBoolean("enable-warning", true);
        warningTimeTicks = config.getLong("warning-time-seconds", 5) * 20L;
        overloadCheckIntervalTicks = config.getLong("overload-detection.check-interval-seconds", 30) * 20L;
        logMemoryStats = config.getBoolean("log-memory-stats", false);
        updateCheckIntervalTicks = config.getLong("update-check-interval-seconds", 60 * 60 * 24) * 20L;
        updateUrl = config.getString("update-url", "https://api.modrinth.com/v2/project/fixlag/version");

        validateConfigValues();
    }

    private void validateConfigValues() {
        if (deletionIntervalTicks <= 0) {
            plugin.getLogger().log(Level.WARNING, "Deletion interval is invalid! Using default value of 60 seconds.");
            deletionIntervalTicks = 60 * 20L;
        }
        if (warningTimeTicks < 0) {
            plugin.getLogger().log(Level.WARNING, "Warning time is invalid! Using default value of 5 seconds.");
            warningTimeTicks = 5 * 20L;
        }
        if (overloadCheckIntervalTicks <= 0) {
            plugin.getLogger().log(Level.WARNING, "Overload check interval is invalid! Using default value of 30 seconds.");
            overloadCheckIntervalTicks = 30 * 20L;
        }
        if (updateCheckIntervalTicks <= 0) {
            plugin.getLogger().log(Level.WARNING, "Update check interval is invalid! Using default value of 1 day.");
            updateCheckIntervalTicks = 60 * 60 * 24 * 20L;
        }
    }

    public Set<String> getEntitiesToDelete() {
        return entitiesToDelete;
    }

    public long getDeletionIntervalTicks() {
        return deletionIntervalTicks;
    }

    public boolean isEnableWarning() {
        return enableWarning;
    }

    public long getWarningTimeTicks() {
        return warningTimeTicks;
    }

    public long getOverloadCheckIntervalTicks() {
        return overloadCheckIntervalTicks;
    }

    public boolean isLogMemoryStats() {
        return logMemoryStats;
    }

    public long getUpdateCheckIntervalTicks() {
        return updateCheckIntervalTicks;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }
}
