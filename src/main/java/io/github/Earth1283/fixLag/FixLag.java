package io.github.Earth1283.fixLag;

import io.github.Earth1283.fixLag.commands.CommandManager;
import io.github.Earth1283.fixLag.listeners.ArmorStandOptimizer;
import io.github.Earth1283.fixLag.listeners.ChunkEntityLimiter;
import io.github.Earth1283.fixLag.listeners.CollisionOptimizer;
import io.github.Earth1283.fixLag.listeners.ExplosionOptimizer;
import io.github.Earth1283.fixLag.listeners.HopperOptimizer;
import io.github.Earth1283.fixLag.listeners.MobStacker;
import io.github.Earth1283.fixLag.listeners.SpawnerOptimizer;
import io.github.Earth1283.fixLag.managers.*;
import io.github.Earth1283.fixLag.utils.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class FixLag extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private PerformanceMonitor performanceMonitor;
    private TaskManager taskManager;
    private UpdateChecker updateChecker;
    private CommandManager commandManager;
    private DeletedItemsManager deletedItemsManager;
    private MobStacker mobStacker;
    private ChunkAnalyzer chunkAnalyzer;
    private ExplosionOptimizer explosionOptimizer;
    private DynamicDistanceManager dynamicDistanceManager;
    private LagNotifier lagNotifier;
    private ChunkEntityLimiter chunkEntityLimiter;
    private VillagerLobotomizer villagerLobotomizer;
    private PanicModeManager panicModeManager;
    private SpawnerOptimizer spawnerOptimizer;
    private ArmorStandOptimizer armorStandOptimizer;
    private RedstoneAnalyzer redstoneAnalyzer;
    private ExperienceOrbMerger experienceOrbMerger;
    private HopperOptimizer hopperOptimizer;
    private CollisionOptimizer collisionOptimizer;

    @Override
    public void onEnable() {
        messageManager = new MessageManager(this);
        messageManager.logInfo("log_startup_version", "<version>", getDescription().getVersion());

        configManager = new ConfigManager(this, messageManager);
        messageManager.logInfo("log_startup_config_loaded");

        performanceMonitor = new PerformanceMonitor(messageManager, getLogger());
        deletedItemsManager = new DeletedItemsManager(this);
        chunkAnalyzer = new ChunkAnalyzer(this, messageManager);
        explosionOptimizer = new ExplosionOptimizer(this);
        dynamicDistanceManager = new DynamicDistanceManager(this);
        lagNotifier = new LagNotifier(this);
        chunkEntityLimiter = new ChunkEntityLimiter(this, configManager);
        villagerLobotomizer = new VillagerLobotomizer(this, configManager);
        panicModeManager = new PanicModeManager(this);
        spawnerOptimizer = new SpawnerOptimizer(this);
        armorStandOptimizer = new ArmorStandOptimizer(this);
        redstoneAnalyzer = new RedstoneAnalyzer(this);
        experienceOrbMerger = new ExperienceOrbMerger(this);
        hopperOptimizer = new HopperOptimizer(this);
        collisionOptimizer = new CollisionOptimizer(this);
        ServerConfigOptimizer configOptimizer = new ServerConfigOptimizer(this, messageManager);
        taskManager = new TaskManager(this, configManager, messageManager, performanceMonitor, deletedItemsManager, dynamicDistanceManager, lagNotifier);
        updateChecker = new UpdateChecker(this, configManager, messageManager);
        commandManager = new CommandManager(this, taskManager, performanceMonitor, messageManager, deletedItemsManager, chunkAnalyzer, configOptimizer, redstoneAnalyzer);
        mobStacker = new MobStacker(this, configManager);

        messageManager.logInfo("log_startup_features_header");
        logFeature("Mob Stacking",           configManager.isMobStackingEnabled());
        logFeature("Smart Clear",            configManager.isSmartClearEnabled());
        logFeature("Panic Mode",             configManager.isPanicModeEnabled());
        logFeature("Spawner Optimizer",      configManager.isSpawnerOptimizerEnabled());
        logFeature("Armor Stand Optimizer",  configManager.isArmorStandOptimizerEnabled());
        logFeature("XP Orb Merger",          configManager.isXpOrbMergerEnabled());
        logFeature("Explosion Optimization", configManager.isExplosionOptimizationEnabled());
        logFeature("Dynamic Distance",       configManager.isDynamicDistanceEnabled());
        logFeature("Lag Notifications",      configManager.isLagNotificationsEnabled());
        logFeature("Hopper Optimizer",       configManager.isHopperOptimizerEnabled());
        logFeature("Collision Optimizer",    configManager.isCollisionOptimizerEnabled());
        logFeature("Chunk Entity Limits",    configManager.isChunkEntityLimitsEnabled());
        logFeature("Villager Lobotomizer",   configManager.isVillagerLobotomizationEnabled());

        getServer().getPluginManager().registerEvents(mobStacker, this);
        getServer().getPluginManager().registerEvents(explosionOptimizer, this);
        getServer().getPluginManager().registerEvents(deletedItemsManager, this);
        getServer().getPluginManager().registerEvents(chunkEntityLimiter, this);
        getServer().getPluginManager().registerEvents(spawnerOptimizer, this);
        getServer().getPluginManager().registerEvents(armorStandOptimizer, this);
        getServer().getPluginManager().registerEvents(hopperOptimizer, this);
        messageManager.logInfo("log_startup_events_registered");

        taskManager.startDeletionTask();
        taskManager.startSmartClearTask();
        taskManager.startDynamicDistanceTask();
        taskManager.startLagNotifierTask();
        villagerLobotomizer.runTaskTimer(this, 20L * 30, 20L * 60);
        collisionOptimizer.runTaskTimer(this, 20L * 20, configManager.getCollisionOptimizerCheckIntervalTicks());
        if (configManager.isPanicModeEnabled()) {
            panicModeManager.runTaskTimer(this, 20L * 15, configManager.getPanicModeCheckIntervalTicks());
        }
        if (configManager.isXpOrbMergerEnabled()) {
            experienceOrbMerger.runTaskTimer(this, 20L * 5, configManager.getXpOrbMergerCheckIntervalTicks());
        }
        updateChecker.startUpdateCheckTask();
        messageManager.logInfo("log_startup_tasks_started");

        new Metrics(this, 28156);

        messageManager.logInfo("log_startup_ready", "<version>", getDescription().getVersion());
    }

    private void logFeature(String name, boolean enabled) {
        String status = enabled ? "<green>enabled" : "<gray>disabled";
        messageManager.logInfo("log_startup_feature_status", "<feature>", name, "<status>", status);
    }

    @Override
    public void onDisable() {
        messageManager.logInfo("log_plugin_disabled");
        messageManager.logInfo("log_plugin_goodbye");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public DeletedItemsManager getDeletedItemsManager() {
        return deletedItemsManager;
    }
}
