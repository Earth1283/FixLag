package io.github.Earth1283.fixLag;

import io.github.Earth1283.fixLag.commands.CommandManager;
import io.github.Earth1283.fixLag.listeners.ArmorStandOptimizer;
import io.github.Earth1283.fixLag.listeners.ChunkEntityLimiter;
import io.github.Earth1283.fixLag.listeners.ExplosionOptimizer;
import io.github.Earth1283.fixLag.listeners.MobStacker;
import io.github.Earth1283.fixLag.listeners.SpawnerOptimizer;
import io.github.Earth1283.fixLag.managers.*;
import io.github.Earth1283.fixLag.utils.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

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

    @Override
    public void onEnable() {
        // Initialize managers
        messageManager = new MessageManager(this);
        configManager = new ConfigManager(this, messageManager);
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
        ServerConfigOptimizer configOptimizer = new ServerConfigOptimizer(this, messageManager);
        taskManager = new TaskManager(this, configManager, messageManager, performanceMonitor, deletedItemsManager, dynamicDistanceManager, lagNotifier);
        updateChecker = new UpdateChecker(this, configManager, messageManager);
        commandManager = new CommandManager(this, taskManager, performanceMonitor, messageManager, deletedItemsManager, chunkAnalyzer, configOptimizer, redstoneAnalyzer);
        mobStacker = new MobStacker(this, configManager);

        // Register events
        getServer().getPluginManager().registerEvents(mobStacker, this);
        getServer().getPluginManager().registerEvents(explosionOptimizer, this);
        getServer().getPluginManager().registerEvents(deletedItemsManager, this);
        getServer().getPluginManager().registerEvents(chunkEntityLimiter, this);
        getServer().getPluginManager().registerEvents(spawnerOptimizer, this);
        getServer().getPluginManager().registerEvents(armorStandOptimizer, this);

        // Start tasks
        taskManager.startDeletionTask();
        taskManager.startSmartClearTask();
        taskManager.startDynamicDistanceTask();
        taskManager.startLagNotifierTask();
        villagerLobotomizer.runTaskTimer(this, 20L * 30, 20L * 60);
        
        if (configManager.isPanicModeEnabled()) {
            panicModeManager.runTaskTimer(this, 20L * 15, configManager.getPanicModeCheckIntervalTicks());
        }
        if (configManager.isXpOrbMergerEnabled()) {
            experienceOrbMerger.runTaskTimer(this, 20L * 5, configManager.getXpOrbMergerCheckIntervalTicks());
        }
        
        updateChecker.startUpdateCheckTask();

        // Enable bStats
        new Metrics(this, 28156);

        getLogger().log(Level.INFO, messageManager.getLogMessage("log_plugin_enabled"));
    }

    @Override
    public void onDisable() {
        // Bukkit automatically cancels tasks on disable
        getLogger().log(Level.INFO, messageManager.getLogMessage("log_plugin_disabled"));
        getLogger().log(Level.INFO, messageManager.getLogMessage("log_plugin_goodbye"));
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
