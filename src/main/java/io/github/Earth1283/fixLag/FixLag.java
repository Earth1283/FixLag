package io.github.Earth1283.fixLag;

import io.github.Earth1283.fixLag.commands.CommandManager;
import io.github.Earth1283.fixLag.listeners.ExplosionOptimizer;
import io.github.Earth1283.fixLag.listeners.MobStacker;
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
        ServerConfigOptimizer configOptimizer = new ServerConfigOptimizer(this, messageManager);
        taskManager = new TaskManager(this, configManager, messageManager, performanceMonitor, deletedItemsManager, dynamicDistanceManager);
        updateChecker = new UpdateChecker(this, configManager, messageManager);
        commandManager = new CommandManager(this, taskManager, performanceMonitor, messageManager, deletedItemsManager, chunkAnalyzer, configOptimizer);
        mobStacker = new MobStacker(this, configManager);

        // Register events
        getServer().getPluginManager().registerEvents(mobStacker, this);
        getServer().getPluginManager().registerEvents(explosionOptimizer, this);
        getServer().getPluginManager().registerEvents(deletedItemsManager, this);

        // Start tasks
        taskManager.startDeletionTask();
        taskManager.startSmartClearTask();
        taskManager.startDynamicDistanceTask();
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
