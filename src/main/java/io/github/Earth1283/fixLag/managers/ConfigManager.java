package io.github.Earth1283.fixLag.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private final JavaPlugin plugin;
    private Set<String> entitiesToDelete;
    private boolean ignoreCustomNamedItems;
    private long deletionIntervalTicks;
    private boolean enableWarning;
    private long warningTimeTicks;
    private String notificationType;
    private long overloadCheckIntervalTicks;
    private boolean logMemoryStats;
    private long updateCheckIntervalTicks;
    private String updateUrl;

    // Chunk Entity Limits Config
    private boolean chunkEntityLimitsEnabled;
    private Map<String, Integer> chunkEntityLimits;
    private boolean chunkEntityLimitsSkipNamed;
    private boolean chunkEntityLimitsSkipTamed;

    // Villager Lobotomization Config
    private boolean villagerLobotomizationEnabled;

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

    // Panic Mode Config
    private boolean panicModeEnabled;
    private double panicModeTpsThreshold;
    private double panicModeRecoverTps;
    private long panicModeCheckIntervalTicks;

    // Spawner Optimizer Config
    private boolean spawnerOptimizerEnabled;
    private double spawnerOptimizerTpsThreshold;

    // Armor Stand Optimizer Config
    private boolean armorStandOptimizerEnabled;
    private boolean armorStandDisableGravity;
    private boolean armorStandDisableCollisions;

    // XP Orb Merger Config
    private boolean xpOrbMergerEnabled;
    private double xpOrbMergerRadius;
    private long xpOrbMergerCheckIntervalTicks;

    // Explosion Optimization Config
    private boolean explosionOptimizationEnabled;
    private double explosionOptimizationTpsThreshold;
    private boolean explosionOptimizationDisableDrops;
    private int explosionOptimizationMaxBlocksLimit;

    // Dynamic Distance Config
    private boolean dynamicDistanceEnabled;
    private double dynamicDistanceTpsLowThreshold;
    private double dynamicDistanceTpsHighThreshold;
    private int dynamicDistanceMinView;
    private int dynamicDistanceMaxView;
    private int dynamicDistanceMinSim;
    private int dynamicDistanceMaxSim;
    private long dynamicDistanceCheckIntervalTicks;
    private boolean dynamicDistanceNotifyAdmins;

    // Lag Notifications Config
    private boolean lagNotificationsEnabled;
    private long lagNotificationsCheckIntervalTicks;
    private long lagNotificationsCooldownTicks;
    private boolean lagNotificationsTpsEnabled;
    private double lagNotificationsTpsThreshold;
    private boolean lagNotificationsRamEnabled;
    private double lagNotificationsRamThreshold;
    private boolean lagNotificationsPingEnabled;
    private int lagNotificationsPingThreshold;

    // Hopper Optimizer Config
    private boolean hopperOptimizerEnabled;
    private double hopperOptimizerTpsThreshold;
    private double hopperOptimizerCancelChance;

    // Collision Optimizer Config
    private boolean collisionOptimizerEnabled;
    private double collisionOptimizerTpsThreshold;
    private long collisionOptimizerCheckIntervalTicks;

    private final MessageManager messageManager;

    public ConfigManager(JavaPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        entitiesToDelete = new HashSet<>(config.getStringList("entities-to-delete"));
        ignoreCustomNamedItems = config.getBoolean("ignore-custom-named-items", true);
        deletionIntervalTicks = config.getLong("deletion-interval-seconds", 60) * 20L;
        enableWarning = config.getBoolean("enable-warning", true);
        warningTimeTicks = config.getLong("warning-time-seconds", 5) * 20L;
        notificationType = config.getString("notification-type", "CHAT").toUpperCase();
        overloadCheckIntervalTicks = config.getLong("overload-detection.check-interval-seconds", 30) * 20L;
        logMemoryStats = config.getBoolean("log-memory-stats", false);
        updateCheckIntervalTicks = config.getLong("update-check-interval-seconds", 60 * 60 * 24) * 20L;
        updateUrl = config.getString("update-url", "https://api.modrinth.com/v2/project/fixlag/version");

        // Load Chunk Entity Limits Config
        chunkEntityLimitsEnabled = config.getBoolean("chunk-entity-limits.enabled", false);
        chunkEntityLimits = new HashMap<>();
        org.bukkit.configuration.ConfigurationSection limitsSection =
            config.getConfigurationSection("chunk-entity-limits.limits");
        if (limitsSection != null) {
            for (String key : limitsSection.getKeys(false)) {
                chunkEntityLimits.put(key.toUpperCase(), limitsSection.getInt(key));
            }
        }
        chunkEntityLimitsSkipNamed = config.getBoolean("chunk-entity-limits.skip-custom-named", true);
        chunkEntityLimitsSkipTamed = config.getBoolean("chunk-entity-limits.skip-tamed", true);

        // Load Villager Lobotomization Config
        villagerLobotomizationEnabled = config.getBoolean("villager-lobotomization.enabled", false);

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

        // Load Panic Mode Config
        panicModeEnabled = config.getBoolean("panic-mode.enabled", true);
        panicModeTpsThreshold = config.getDouble("panic-mode.tps-threshold", 14.0);
        panicModeRecoverTps = config.getDouble("panic-mode.recover-tps", 18.0);
        panicModeCheckIntervalTicks = config.getLong("panic-mode.check-interval-seconds", 15) * 20L;

        // Load Spawner Optimizer Config
        spawnerOptimizerEnabled = config.getBoolean("spawner-optimizer.enabled", true);
        spawnerOptimizerTpsThreshold = config.getDouble("spawner-optimizer.tps-threshold", 16.0);

        // Load Armor Stand Optimizer Config
        armorStandOptimizerEnabled = config.getBoolean("armor-stand-optimizer.enabled", true);
        armorStandDisableGravity = config.getBoolean("armor-stand-optimizer.disable-gravity", true);
        armorStandDisableCollisions = config.getBoolean("armor-stand-optimizer.disable-collisions", true);

        // Load XP Orb Merger Config
        xpOrbMergerEnabled = config.getBoolean("xp-orb-merger.enabled", true);
        xpOrbMergerRadius = config.getDouble("xp-orb-merger.merge-radius", 4.0);
        xpOrbMergerCheckIntervalTicks = config.getLong("xp-orb-merger.check-interval-seconds", 20) * 20L;

        // Load Explosion Optimization Config
        explosionOptimizationEnabled = config.getBoolean("explosion-optimization.enabled", true);
        explosionOptimizationTpsThreshold = config.getDouble("explosion-optimization.tps-threshold", 16.0);
        explosionOptimizationDisableDrops = config.getBoolean("explosion-optimization.disable-block-drops", true);
        explosionOptimizationMaxBlocksLimit = config.getInt("explosion-optimization.max-blocks-limit", 100);

        // Load Dynamic Distance Config
        dynamicDistanceEnabled = config.getBoolean("dynamic-distance.enabled", false);
        dynamicDistanceTpsLowThreshold = config.getDouble("dynamic-distance.tps-low-threshold", 17.0);
        dynamicDistanceTpsHighThreshold = config.getDouble("dynamic-distance.tps-high-threshold", 19.0);
        dynamicDistanceMinView = config.getInt("dynamic-distance.min-view-distance", 4);
        dynamicDistanceMaxView = config.getInt("dynamic-distance.max-view-distance", 10);
        dynamicDistanceMinSim = config.getInt("dynamic-distance.min-simulation-distance", 4);
        dynamicDistanceMaxSim = config.getInt("dynamic-distance.max-simulation-distance", 10);
        dynamicDistanceCheckIntervalTicks = config.getLong("dynamic-distance.check-interval-seconds", 60) * 20L;
        dynamicDistanceNotifyAdmins = config.getBoolean("dynamic-distance.notify-admins", true);

        // Load Lag Notifications Config
        lagNotificationsEnabled = config.getBoolean("lag-notifications.enabled", true);
        lagNotificationsCheckIntervalTicks = config.getLong("lag-notifications.check-interval-seconds", 15) * 20L;
        lagNotificationsCooldownTicks = config.getLong("lag-notifications.cooldown-seconds", 300) * 20L;
        lagNotificationsTpsEnabled = config.getBoolean("lag-notifications.tps.enabled", true);
        lagNotificationsTpsThreshold = config.getDouble("lag-notifications.tps.threshold", 15.0);
        lagNotificationsRamEnabled = config.getBoolean("lag-notifications.ram.enabled", true);
        lagNotificationsRamThreshold = config.getDouble("lag-notifications.ram.usage-percent-threshold", 90.0);
        lagNotificationsPingEnabled = config.getBoolean("lag-notifications.ping.enabled", true);
        lagNotificationsPingThreshold = config.getInt("lag-notifications.ping.average-threshold", 150);

        // Load Hopper Optimizer Config
        hopperOptimizerEnabled = config.getBoolean("hopper-optimizer.enabled", true);
        hopperOptimizerTpsThreshold = config.getDouble("hopper-optimizer.tps-threshold", 16.0);
        hopperOptimizerCancelChance = config.getDouble("hopper-optimizer.cancel-chance", 0.5);

        // Load Collision Optimizer Config
        collisionOptimizerEnabled = config.getBoolean("collision-optimizer.enabled", true);
        collisionOptimizerTpsThreshold = config.getDouble("collision-optimizer.tps-threshold", 16.0);
        collisionOptimizerCheckIntervalTicks = config.getLong("collision-optimizer.check-interval-seconds", 30) * 20L;

        validateConfigValues();
    }

    private void validateConfigValues() {
        if (deletionIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_deletion_interval");
            deletionIntervalTicks = 60 * 20L;
        }
        if (warningTimeTicks < 0) {
            messageManager.logWarn("log_config_invalid_warning_time");
            warningTimeTicks = 5 * 20L;
        }
        if (overloadCheckIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_overload_interval");
            overloadCheckIntervalTicks = 30 * 20L;
        }
        if (updateCheckIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_update_interval");
            updateCheckIntervalTicks = 60 * 60 * 24 * 20L;
        }

        // TPS threshold range checks (must be > 0 and <= 20)
        if (smartClearTpsThreshold <= 0 || smartClearTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "smart-clear");
            smartClearTpsThreshold = 16.0;
        }
        if (panicModeTpsThreshold <= 0 || panicModeTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "panic-mode");
            panicModeTpsThreshold = 14.0;
        }
        if (panicModeRecoverTps <= 0 || panicModeRecoverTps > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "panic-mode recover");
            panicModeRecoverTps = 18.0;
        }
        if (panicModeRecoverTps <= panicModeTpsThreshold) {
            messageManager.logWarn("log_config_panic_recover_lte_threshold");
            panicModeRecoverTps = panicModeTpsThreshold + 2.0;
        }
        if (spawnerOptimizerTpsThreshold <= 0 || spawnerOptimizerTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "spawner-optimizer");
            spawnerOptimizerTpsThreshold = 16.0;
        }
        if (explosionOptimizationTpsThreshold <= 0 || explosionOptimizationTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "explosion-optimization");
            explosionOptimizationTpsThreshold = 16.0;
        }
        if (hopperOptimizerTpsThreshold <= 0 || hopperOptimizerTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "hopper-optimizer");
            hopperOptimizerTpsThreshold = 16.0;
        }
        if (collisionOptimizerTpsThreshold <= 0 || collisionOptimizerTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "collision-optimizer");
            collisionOptimizerTpsThreshold = 16.0;
        }
        if (lagNotificationsTpsThreshold <= 0 || lagNotificationsTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "lag-notifications");
            lagNotificationsTpsThreshold = 15.0;
        }

        // Dynamic distance min < max invariants
        if (dynamicDistanceMinView >= dynamicDistanceMaxView) {
            messageManager.logWarn("log_config_invalid_distance_range", "<type>", "view");
            dynamicDistanceMinView = 4;
            dynamicDistanceMaxView = 10;
        }
        if (dynamicDistanceMinSim >= dynamicDistanceMaxSim) {
            messageManager.logWarn("log_config_invalid_distance_range", "<type>", "simulation");
            dynamicDistanceMinSim = 4;
            dynamicDistanceMaxSim = 10;
        }
        if (dynamicDistanceTpsLowThreshold >= dynamicDistanceTpsHighThreshold) {
            messageManager.logWarn("log_config_invalid_dynamic_distance_tps");
            dynamicDistanceTpsLowThreshold = 17.0;
            dynamicDistanceTpsHighThreshold = 19.0;
        }

        // Interval > 0 checks
        if (smartClearCheckIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_interval", "<feature>", "smart-clear check");
            smartClearCheckIntervalTicks = 10 * 20L;
        }

        // Misc range checks
        if (hopperOptimizerCancelChance < 0 || hopperOptimizerCancelChance > 1.0) {
            messageManager.logWarn("log_config_invalid_cancel_chance");
            hopperOptimizerCancelChance = 0.5;
        }
        if (lagNotificationsRamThreshold <= 0 || lagNotificationsRamThreshold > 100) {
            messageManager.logWarn("log_config_invalid_ram_threshold");
            lagNotificationsRamThreshold = 90.0;
        }
        if (lagNotificationsPingThreshold <= 0) {
            messageManager.logWarn("log_config_invalid_ping_threshold");
            lagNotificationsPingThreshold = 150;
        }
        if (xpOrbMergerRadius <= 0) {
            messageManager.logWarn("log_config_invalid_xporb_radius");
            xpOrbMergerRadius = 4.0;
        }
        if (mobStackingRadius <= 0) {
            messageManager.logWarn("log_config_invalid_mob_stacking_radius");
            mobStackingRadius = 10;
        }
        if (mobStackingMaxStackSize <= 0) {
            messageManager.logWarn("log_config_invalid_mob_stacking_max");
            mobStackingMaxStackSize = 50;
        }
    }

    public Set<String> getEntitiesToDelete() {
        return entitiesToDelete;
    }

    public boolean isIgnoreCustomNamedItems() {
        return ignoreCustomNamedItems;
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

    public String getNotificationType() {
        return notificationType;
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

    // Chunk Entity Limits Getters
    public boolean isChunkEntityLimitsEnabled() {
        return chunkEntityLimitsEnabled;
    }

    public Map<String, Integer> getChunkEntityLimits() {
        return chunkEntityLimits;
    }

    public boolean isChunkEntityLimitsSkipNamed() {
        return chunkEntityLimitsSkipNamed;
    }

    public boolean isChunkEntityLimitsSkipTamed() {
        return chunkEntityLimitsSkipTamed;
    }

    // Villager Lobotomization Getters
    public boolean isVillagerLobotomizationEnabled() {
        return villagerLobotomizationEnabled;
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

    // Panic Mode Getters
    public boolean isPanicModeEnabled() {
        return panicModeEnabled;
    }

    public double getPanicModeTpsThreshold() {
        return panicModeTpsThreshold;
    }

    public double getPanicModeRecoverTps() {
        return panicModeRecoverTps;
    }

    public long getPanicModeCheckIntervalTicks() {
        return panicModeCheckIntervalTicks;
    }

    // Spawner Optimizer Getters
    public boolean isSpawnerOptimizerEnabled() {
        return spawnerOptimizerEnabled;
    }

    public double getSpawnerOptimizerTpsThreshold() {
        return spawnerOptimizerTpsThreshold;
    }

    // Armor Stand Optimizer Getters
    public boolean isArmorStandOptimizerEnabled() {
        return armorStandOptimizerEnabled;
    }

    public boolean isArmorStandDisableGravity() {
        return armorStandDisableGravity;
    }

    public boolean isArmorStandDisableCollisions() {
        return armorStandDisableCollisions;
    }

    // XP Orb Merger Getters
    public boolean isXpOrbMergerEnabled() {
        return xpOrbMergerEnabled;
    }

    public double getXpOrbMergerRadius() {
        return xpOrbMergerRadius;
    }

    public long getXpOrbMergerCheckIntervalTicks() {
        return xpOrbMergerCheckIntervalTicks;
    }

    // Explosion Optimization Getters
    public boolean isExplosionOptimizationEnabled() {
        return explosionOptimizationEnabled;
    }

    public double getExplosionOptimizationTpsThreshold() {
        return explosionOptimizationTpsThreshold;
    }

    public boolean isExplosionOptimizationDisableDrops() {
        return explosionOptimizationDisableDrops;
    }

    public int getExplosionOptimizationMaxBlocksLimit() {
        return explosionOptimizationMaxBlocksLimit;
    }

    // Dynamic Distance Getters
    public boolean isDynamicDistanceEnabled() {
        return dynamicDistanceEnabled;
    }

    public double getDynamicDistanceTpsLowThreshold() {
        return dynamicDistanceTpsLowThreshold;
    }

    public double getDynamicDistanceTpsHighThreshold() {
        return dynamicDistanceTpsHighThreshold;
    }

    public int getDynamicDistanceMinView() {
        return dynamicDistanceMinView;
    }

    public int getDynamicDistanceMaxView() {
        return dynamicDistanceMaxView;
    }

    public int getDynamicDistanceMinSim() {
        return dynamicDistanceMinSim;
    }

    public int getDynamicDistanceMaxSim() {
        return dynamicDistanceMaxSim;
    }

    public long getDynamicDistanceCheckIntervalTicks() {
        return dynamicDistanceCheckIntervalTicks;
    }

    public boolean isDynamicDistanceNotifyAdmins() {
        return dynamicDistanceNotifyAdmins;
    }

    // Lag Notifications Getters
    public boolean isLagNotificationsEnabled() {
        return lagNotificationsEnabled;
    }

    public long getLagNotificationsCheckIntervalTicks() {
        return lagNotificationsCheckIntervalTicks;
    }

    public long getLagNotificationsCooldownTicks() {
        return lagNotificationsCooldownTicks;
    }

    public boolean isLagNotificationsTpsEnabled() {
        return lagNotificationsTpsEnabled;
    }

    public double getLagNotificationsTpsThreshold() {
        return lagNotificationsTpsThreshold;
    }

    public boolean isLagNotificationsRamEnabled() {
        return lagNotificationsRamEnabled;
    }

    public double getLagNotificationsRamThreshold() {
        return lagNotificationsRamThreshold;
    }

    public boolean isLagNotificationsPingEnabled() {
        return lagNotificationsPingEnabled;
    }

    public int getLagNotificationsPingThreshold() {
        return lagNotificationsPingThreshold;
    }

    // Hopper Optimizer Getters
    public boolean isHopperOptimizerEnabled() {
        return hopperOptimizerEnabled;
    }

    public double getHopperOptimizerTpsThreshold() {
        return hopperOptimizerTpsThreshold;
    }

    public double getHopperOptimizerCancelChance() {
        return hopperOptimizerCancelChance;
    }

    // Collision Optimizer Getters
    public boolean isCollisionOptimizerEnabled() {
        return collisionOptimizerEnabled;
    }

    public double getCollisionOptimizerTpsThreshold() {
        return collisionOptimizerTpsThreshold;
    }

    public long getCollisionOptimizerCheckIntervalTicks() {
        return collisionOptimizerCheckIntervalTicks;
    }
}
