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

    // Mob Stacking Config
    private boolean mobStackingEnabled;
    private int mobStackingRadius;
    private int mobStackingMaxStackSize;
    private Set<String> mobStackingAllowedEntities;
    private String mobStackingNameFormat;

    // Smart Clear Config
    private boolean smartClearEnabled;
    private double smartClearTpsThreshold;
    private long smartClearCheckIntervalTicks;
    private long smartClearCooldownTicks;

    private final MessageManager messageManager;

    public ConfigManager(JavaPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        
        // Update config with missing keys
        config.options().copyDefaults(true);
        plugin.saveConfig();
        
        plugin.reloadConfig();

        entitiesToDelete = new HashSet<>(config.getStringList("entities-to-delete"));
        deletionIntervalTicks = config.getLong("deletion-interval-seconds", 60) * 20L;
        enableWarning = config.getBoolean("enable-warning", true);
        warningTimeTicks = config.getLong("warning-time-seconds", 5) * 20L;
        overloadCheckIntervalTicks = config.getLong("overload-detection.check-interval-seconds", 30) * 20L;
        logMemoryStats = config.getBoolean("log-memory-stats", false);
        updateCheckIntervalTicks = config.getLong("update-check-interval-seconds", 60 * 60 * 24) * 20L;
        updateUrl = config.getString("update-url", "https://api.modrinth.com/v2/project/fixlag/version");

        // Load Mob Stacking Config
        mobStackingEnabled = config.getBoolean("mob-stacking.enabled", true);
        mobStackingRadius = config.getInt("mob-stacking.radius", 10);
        mobStackingMaxStackSize = config.getInt("mob-stacking.max-stack-size", 50);
        mobStackingAllowedEntities = new HashSet<>(config.getStringList("mob-stacking.allowed-entities"));
        mobStackingNameFormat = config.getString("mob-stacking.name-format", "&e%type% &6x%count%");

        // Load Smart Clear Config
        smartClearEnabled = config.getBoolean("smart-clear.enabled", true);
        smartClearTpsThreshold = config.getDouble("smart-clear.tps-threshold", 16.0);
        smartClearCheckIntervalTicks = config.getLong("smart-clear.check-interval-seconds", 10) * 20L;
        smartClearCooldownTicks = config.getLong("smart-clear.cooldown-seconds", 300) * 20L;

        validateConfigValues();
    }

    private void validateConfigValues() {
        if (deletionIntervalTicks <= 0) {
            plugin.getLogger().log(Level.WARNING, messageManager.getLogMessage("log_config_invalid_deletion_interval"));
            deletionIntervalTicks = 60 * 20L;
        }
        if (warningTimeTicks < 0) {
            plugin.getLogger().log(Level.WARNING, messageManager.getLogMessage("log_config_invalid_warning_time"));
            warningTimeTicks = 5 * 20L;
        }
        if (overloadCheckIntervalTicks <= 0) {
            plugin.getLogger().log(Level.WARNING, messageManager.getLogMessage("log_config_invalid_overload_interval"));
            overloadCheckIntervalTicks = 30 * 20L;
        }
        if (updateCheckIntervalTicks <= 0) {
            plugin.getLogger().log(Level.WARNING, messageManager.getLogMessage("log_config_invalid_update_interval"));
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

    // Mob Stacking Getters
    public boolean isMobStackingEnabled() {
        return mobStackingEnabled;
    }

    public int getMobStackingRadius() {
        return mobStackingRadius;
    }

    public int getMobStackingMaxStackSize() {
        return mobStackingMaxStackSize;
    }

    public Set<String> getMobStackingAllowedEntities() {
        return mobStackingAllowedEntities;
    }

    public String getMobStackingNameFormat() {
        return mobStackingNameFormat;
    }

    // Smart Clear Getters
    public boolean isSmartClearEnabled() {
        return smartClearEnabled;
    }

    public double getSmartClearTpsThreshold() {
        return smartClearTpsThreshold;
    }

    public long getSmartClearCheckIntervalTicks() {
        return smartClearCheckIntervalTicks;
    }

    public long getSmartClearCooldownTicks() {
        return smartClearCooldownTicks;
    }
}
