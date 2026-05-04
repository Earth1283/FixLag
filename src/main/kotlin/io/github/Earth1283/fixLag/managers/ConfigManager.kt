package io.github.Earth1283.fixLag.managers

import org.bukkit.plugin.java.JavaPlugin

class ConfigManager(private val plugin: JavaPlugin, private val messageManager: MessageManager) {

    var entitiesToDelete: Set<String> = emptySet()
        private set
    var isIgnoreCustomNamedItems: Boolean = true
        private set
    var deletionIntervalTicks: Long = 60 * 20L
        private set
    var isEnableWarning: Boolean = true
        private set
    var warningTimeTicks: Long = 5 * 20L
        private set
    var notificationType: String = "CHAT"
        private set
    var overloadCheckIntervalTicks: Long = 30 * 20L
        private set
    var isLogMemoryStats: Boolean = false
        private set
    var updateCheckIntervalTicks: Long = 60 * 60 * 24 * 20L
        private set
    var updateUrl: String = "https://api.modrinth.com/v2/project/fixlag/version"
        private set

    // Chunk Entity Limits Config
    var isChunkEntityLimitsEnabled: Boolean = false
        private set
    var chunkEntityLimits: Map<String, Int> = emptyMap()
        private set
    var isChunkEntityLimitsSkipNamed: Boolean = true
        private set
    var isChunkEntityLimitsSkipTamed: Boolean = true
        private set

    // Villager Lobotomization Config
    var isVillagerLobotomizationEnabled: Boolean = false
        private set

    // Mob Stacking Config
    var isMobStackingEnabled: Boolean = true
        private set
    var mobStackingRadius: Int = 10
        private set
    var mobStackingMaxStackSize: Int = 50
        private set
    var mobStackingAllowedEntities: Set<String> = emptySet()
        private set
    var mobStackingNameFormat: String = "&e%type% &6x%count%"
        private set

    // Smart Clear Config
    var isSmartClearEnabled: Boolean = true
        private set
    var smartClearTpsThreshold: Double = 16.0
        private set
    var smartClearCheckIntervalTicks: Long = 10 * 20L
        private set
    var smartClearCooldownTicks: Long = 300 * 20L
        private set

    // Panic Mode Config
    var isPanicModeEnabled: Boolean = true
        private set
    var panicModeTpsThreshold: Double = 14.0
        private set
    var panicModeRecoverTps: Double = 18.0
        private set
    var panicModeCheckIntervalTicks: Long = 15 * 20L
        private set

    // Spawner Optimizer Config
    var isSpawnerOptimizerEnabled: Boolean = true
        private set
    var spawnerOptimizerTpsThreshold: Double = 16.0
        private set

    // Armor Stand Optimizer Config
    var isArmorStandOptimizerEnabled: Boolean = true
        private set
    var isArmorStandDisableGravity: Boolean = true
        private set
    var isArmorStandDisableCollisions: Boolean = true
        private set

    // XP Orb Merger Config
    var isXpOrbMergerEnabled: Boolean = true
        private set
    var xpOrbMergerRadius: Double = 4.0
        private set
    var xpOrbMergerCheckIntervalTicks: Long = 20 * 20L
        private set

    // Explosion Optimization Config
    var isExplosionOptimizationEnabled: Boolean = true
        private set
    var explosionOptimizationTpsThreshold: Double = 16.0
        private set
    var isExplosionOptimizationDisableDrops: Boolean = true
        private set
    var explosionOptimizationMaxBlocksLimit: Int = 100
        private set

    // Dynamic Distance Config
    var isDynamicDistanceEnabled: Boolean = false
        private set
    var dynamicDistanceTpsLowThreshold: Double = 17.0
        private set
    var dynamicDistanceTpsHighThreshold: Double = 19.0
        private set
    var dynamicDistanceMinView: Int = 4
        private set
    var dynamicDistanceMaxView: Int = 10
        private set
    var dynamicDistanceMinSim: Int = 4
        private set
    var dynamicDistanceMaxSim: Int = 10
        private set
    var dynamicDistanceCheckIntervalTicks: Long = 60 * 20L
        private set
    var isDynamicDistanceNotifyAdmins: Boolean = true
        private set

    // Lag Notifications Config
    var isLagNotificationsEnabled: Boolean = true
        private set
    var lagNotificationsCheckIntervalTicks: Long = 15 * 20L
        private set
    var lagNotificationsCooldownTicks: Long = 300 * 20L
        private set
    var isLagNotificationsTpsEnabled: Boolean = true
        private set
    var lagNotificationsTpsThreshold: Double = 15.0
        private set
    var isLagNotificationsRamEnabled: Boolean = true
        private set
    var lagNotificationsRamThreshold: Double = 90.0
        private set
    var isLagNotificationsPingEnabled: Boolean = true
        private set
    var lagNotificationsPingThreshold: Int = 150
        private set

    // Hopper Optimizer Config
    var isHopperOptimizerEnabled: Boolean = true
        private set
    var hopperOptimizerTpsThreshold: Double = 16.0
        private set
    var hopperOptimizerCancelChance: Double = 0.5
        private set

    // Collision Optimizer Config
    var isCollisionOptimizerEnabled: Boolean = true
        private set
    var collisionOptimizerTpsThreshold: Double = 16.0
        private set
    var collisionOptimizerCheckIntervalTicks: Long = 30 * 20L
        private set

    init {
        loadConfig()
    }

    fun loadConfig() {
        plugin.saveDefaultConfig()
        var config = plugin.config
        config.options().copyDefaults(true)
        plugin.saveConfig()
        plugin.reloadConfig()
        config = plugin.config

        entitiesToDelete = config.getStringList("entities-to-delete").toSet()
        isIgnoreCustomNamedItems = config.getBoolean("ignore-custom-named-items", true)
        deletionIntervalTicks = config.getLong("deletion-interval-seconds", 60) * 20L
        isEnableWarning = config.getBoolean("enable-warning", true)
        warningTimeTicks = config.getLong("warning-time-seconds", 5) * 20L
        notificationType = config.getString("notification-type", "CHAT")?.uppercase() ?: "CHAT"
        overloadCheckIntervalTicks = config.getLong("overload-detection.check-interval-seconds", 30) * 20L
        isLogMemoryStats = config.getBoolean("log-memory-stats", false)
        updateCheckIntervalTicks = config.getLong("update-check-interval-seconds", 60 * 60 * 24) * 20L
        updateUrl = config.getString("update-url", "https://api.modrinth.com/v2/project/fixlag/version") ?: "https://api.modrinth.com/v2/project/fixlag/version"

        // Load Chunk Entity Limits Config
        isChunkEntityLimitsEnabled = config.getBoolean("chunk-entity-limits.enabled", false)
        val limitsMap = mutableMapOf<String, Int>()
        config.getConfigurationSection("chunk-entity-limits.limits")?.let { section ->
            for (key in section.getKeys(false)) {
                limitsMap[key.uppercase()] = section.getInt(key)
            }
        }
        chunkEntityLimits = limitsMap
        isChunkEntityLimitsSkipNamed = config.getBoolean("chunk-entity-limits.skip-custom-named", true)
        isChunkEntityLimitsSkipTamed = config.getBoolean("chunk-entity-limits.skip-tamed", true)

        // Load Villager Lobotomization Config
        isVillagerLobotomizationEnabled = config.getBoolean("villager-lobotomization.enabled", false)

        // Load Mob Stacking Config
        isMobStackingEnabled = config.getBoolean("mob-stacking.enabled", true)
        mobStackingRadius = config.getInt("mob-stacking.radius", 10)
        mobStackingMaxStackSize = config.getInt("mob-stacking.max-stack-size", 50)
        mobStackingAllowedEntities = config.getStringList("mob-stacking.allowed-entities").toSet()
        mobStackingNameFormat = config.getString("mob-stacking.name-format", "&e%type% &6x%count%") ?: "&e%type% &6x%count%"

        // Load Smart Clear Config
        isSmartClearEnabled = config.getBoolean("smart-clear.enabled", true)
        smartClearTpsThreshold = config.getSafeDouble("smart-clear.tps-threshold", 16.0)
        smartClearCheckIntervalTicks = config.getLong("smart-clear.check-interval-seconds", 10) * 20L
        smartClearCooldownTicks = config.getLong("smart-clear.cooldown-seconds", 300) * 20L

        // Load Panic Mode Config
        isPanicModeEnabled = config.getBoolean("panic-mode.enabled", true)
        panicModeTpsThreshold = config.getSafeDouble("panic-mode.tps-threshold", 14.0)
        panicModeRecoverTps = config.getSafeDouble("panic-mode.recover-tps", 18.0)
        panicModeCheckIntervalTicks = config.getLong("panic-mode.check-interval-seconds", 15) * 20L

        // Load Spawner Optimizer Config
        isSpawnerOptimizerEnabled = config.getBoolean("spawner-optimizer.enabled", true)
        spawnerOptimizerTpsThreshold = config.getSafeDouble("spawner-optimizer.tps-threshold", 16.0)

        // Load Armor Stand Optimizer Config
        isArmorStandOptimizerEnabled = config.getBoolean("armor-stand-optimizer.enabled", true)
        isArmorStandDisableGravity = config.getBoolean("armor-stand-optimizer.disable-gravity", true)
        isArmorStandDisableCollisions = config.getBoolean("armor-stand-optimizer.disable-collisions", true)

        // Load XP Orb Merger Config
        isXpOrbMergerEnabled = config.getBoolean("xp-orb-merger.enabled", true)
        xpOrbMergerRadius = config.getSafeDouble("xp-orb-merger.merge-radius", 4.0)
        xpOrbMergerCheckIntervalTicks = config.getLong("xp-orb-merger.check-interval-seconds", 20) * 20L

        // Load Explosion Optimization Config
        isExplosionOptimizationEnabled = config.getBoolean("explosion-optimization.enabled", true)
        explosionOptimizationTpsThreshold = config.getSafeDouble("explosion-optimization.tps-threshold", 16.0)
        isExplosionOptimizationDisableDrops = config.getBoolean("explosion-optimization.disable-block-drops", true)
        explosionOptimizationMaxBlocksLimit = config.getInt("explosion-optimization.max-blocks-limit", 100)

        // Load Dynamic Distance Config
        isDynamicDistanceEnabled = config.getBoolean("dynamic-distance.enabled", false)
        dynamicDistanceTpsLowThreshold = config.getSafeDouble("dynamic-distance.tps-low-threshold", 17.0)
        dynamicDistanceTpsHighThreshold = config.getSafeDouble("dynamic-distance.tps-high-threshold", 19.0)
        dynamicDistanceMinView = config.getInt("dynamic-distance.min-view-distance", 4)
        dynamicDistanceMaxView = config.getInt("dynamic-distance.max-view-distance", 10)
        dynamicDistanceMinSim = config.getInt("dynamic-distance.min-simulation-distance", 4)
        dynamicDistanceMaxSim = config.getInt("dynamic-distance.max-simulation-distance", 10)
        dynamicDistanceCheckIntervalTicks = config.getLong("dynamic-distance.check-interval-seconds", 60) * 20L
        isDynamicDistanceNotifyAdmins = config.getBoolean("dynamic-distance.notify-admins", true)

        // Load Lag Notifications Config
        isLagNotificationsEnabled = config.getBoolean("lag-notifications.enabled", true)
        lagNotificationsCheckIntervalTicks = config.getLong("lag-notifications.check-interval-seconds", 15) * 20L
        lagNotificationsCooldownTicks = config.getLong("lag-notifications.cooldown-seconds", 300) * 20L
        isLagNotificationsTpsEnabled = config.getBoolean("lag-notifications.tps.enabled", true)
        lagNotificationsTpsThreshold = config.getSafeDouble("lag-notifications.tps.threshold", 15.0)
        isLagNotificationsRamEnabled = config.getBoolean("lag-notifications.ram.enabled", true)
        lagNotificationsRamThreshold = config.getSafeDouble("lag-notifications.ram.usage-percent-threshold", 90.0)
        isLagNotificationsPingEnabled = config.getBoolean("lag-notifications.ping.enabled", true)
        lagNotificationsPingThreshold = config.getInt("lag-notifications.ping.average-threshold", 150)

        // Load Hopper Optimizer Config
        isHopperOptimizerEnabled = config.getBoolean("hopper-optimizer.enabled", true)
        hopperOptimizerTpsThreshold = config.getSafeDouble("hopper-optimizer.tps-threshold", 16.0)
        hopperOptimizerCancelChance = config.getSafeDouble("hopper-optimizer.cancel-chance", 0.5)

        // Load Collision Optimizer Config
        isCollisionOptimizerEnabled = config.getBoolean("collision-optimizer.enabled", true)
        collisionOptimizerTpsThreshold = config.getSafeDouble("collision-optimizer.tps-threshold", 16.0)
        collisionOptimizerCheckIntervalTicks = config.getLong("collision-optimizer.check-interval-seconds", 30) * 20L

        validateConfigValues()
    }

    private fun org.bukkit.configuration.file.FileConfiguration.getSafeDouble(path: String, default: Double): Double {
        return if (this.contains(path)) this.getDouble(path) else default
    }

    private fun validateConfigValues() {
        if (deletionIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_deletion_interval")
            deletionIntervalTicks = 60 * 20L
        }
        if (warningTimeTicks < 0) {
            messageManager.logWarn("log_config_invalid_warning_time")
            warningTimeTicks = 5 * 20L
        }
        if (overloadCheckIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_overload_interval")
            overloadCheckIntervalTicks = 30 * 20L
        }
        if (updateCheckIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_update_interval")
            updateCheckIntervalTicks = 60 * 60 * 24 * 20L
        }

        // TPS threshold range checks (must be > 0 and <= 20)
        if (smartClearTpsThreshold <= 0 || smartClearTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "smart-clear")
            smartClearTpsThreshold = 16.0
        }
        if (panicModeTpsThreshold <= 0 || panicModeTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "panic-mode")
            panicModeTpsThreshold = 14.0
        }
        if (panicModeRecoverTps <= 0 || panicModeRecoverTps > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "panic-mode recover")
            panicModeRecoverTps = 18.0
        }
        if (panicModeRecoverTps <= panicModeTpsThreshold) {
            messageManager.logWarn("log_config_panic_recover_lte_threshold")
            panicModeRecoverTps = panicModeTpsThreshold + 2.0
        }
        if (spawnerOptimizerTpsThreshold <= 0 || spawnerOptimizerTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "spawner-optimizer")
            spawnerOptimizerTpsThreshold = 16.0
        }
        if (explosionOptimizationTpsThreshold <= 0 || explosionOptimizationTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "explosion-optimization")
            explosionOptimizationTpsThreshold = 16.0
        }
        if (hopperOptimizerTpsThreshold <= 0 || hopperOptimizerTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "hopper-optimizer")
            hopperOptimizerTpsThreshold = 16.0
        }
        if (collisionOptimizerTpsThreshold <= 0 || collisionOptimizerTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "collision-optimizer")
            collisionOptimizerTpsThreshold = 16.0
        }
        if (lagNotificationsTpsThreshold <= 0 || lagNotificationsTpsThreshold > 20) {
            messageManager.logWarn("log_config_invalid_tps_threshold", "<feature>", "lag-notifications")
            lagNotificationsTpsThreshold = 15.0
        }

        // Dynamic distance min < max invariants
        if (dynamicDistanceMinView >= dynamicDistanceMaxView) {
            messageManager.logWarn("log_config_invalid_distance_range", "<type>", "view")
            dynamicDistanceMinView = 4
            dynamicDistanceMaxView = 10
        }
        if (dynamicDistanceMinSim >= dynamicDistanceMaxSim) {
            messageManager.logWarn("log_config_invalid_distance_range", "<type>", "simulation")
            dynamicDistanceMinSim = 4
            dynamicDistanceMaxSim = 10
        }
        if (dynamicDistanceTpsLowThreshold >= dynamicDistanceTpsHighThreshold) {
            messageManager.logWarn("log_config_invalid_dynamic_distance_tps")
            dynamicDistanceTpsLowThreshold = 17.0
            dynamicDistanceTpsHighThreshold = 19.0
        }

        // Interval > 0 checks
        if (smartClearCheckIntervalTicks <= 0) {
            messageManager.logWarn("log_config_invalid_interval", "<feature>", "smart-clear check")
            smartClearCheckIntervalTicks = 10 * 20L
        }

        // Misc range checks
        if (hopperOptimizerCancelChance < 0 || hopperOptimizerCancelChance > 1.0) {
            messageManager.logWarn("log_config_invalid_cancel_chance")
            hopperOptimizerCancelChance = 0.5
        }
        if (lagNotificationsRamThreshold <= 0 || lagNotificationsRamThreshold > 100) {
            messageManager.logWarn("log_config_invalid_ram_threshold")
            lagNotificationsRamThreshold = 90.0
        }
        if (lagNotificationsPingThreshold <= 0) {
            messageManager.logWarn("log_config_invalid_ping_threshold")
            lagNotificationsPingThreshold = 150
        }
        if (xpOrbMergerRadius <= 0) {
            messageManager.logWarn("log_config_invalid_xporb_radius")
            xpOrbMergerRadius = 4.0
        }
        if (mobStackingRadius <= 0) {
            messageManager.logWarn("log_config_invalid_mob_stacking_radius")
            mobStackingRadius = 10
        }
        if (mobStackingMaxStackSize <= 0) {
            messageManager.logWarn("log_config_invalid_mob_stacking_max")
            mobStackingMaxStackSize = 50
        }
    }
}
