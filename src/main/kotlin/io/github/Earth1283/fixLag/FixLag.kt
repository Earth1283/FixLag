package io.github.Earth1283.fixLag

import io.github.Earth1283.fixLag.commands.CommandManager
import io.github.Earth1283.fixLag.listeners.*
import io.github.Earth1283.fixLag.managers.*
import io.github.Earth1283.fixLag.utils.Metrics
import org.bukkit.plugin.java.JavaPlugin

class FixLag : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set
    lateinit var messageManager: MessageManager
        private set
    lateinit var performanceMonitor: PerformanceMonitor
        private set
    lateinit var taskManager: TaskManager
        private set
    lateinit var updateChecker: UpdateChecker
        private set
    lateinit var commandManager: CommandManager
        private set
    lateinit var deletedItemsManager: DeletedItemsManager
        private set
    lateinit var mobStacker: MobStacker
        private set
    lateinit var chunkAnalyzer: ChunkAnalyzer
        private set
    lateinit var explosionOptimizer: ExplosionOptimizer
        private set
    lateinit var dynamicDistanceManager: DynamicDistanceManager
        private set
    lateinit var lagNotifier: LagNotifier
        private set
    lateinit var chunkEntityLimiter: ChunkEntityLimiter
        private set
    lateinit var villagerLobotomizer: VillagerLobotomizer
        private set
    lateinit var panicModeManager: PanicModeManager
        private set
    lateinit var spawnerOptimizer: SpawnerOptimizer
        private set
    lateinit var armorStandOptimizer: ArmorStandOptimizer
        private set
    lateinit var redstoneAnalyzer: RedstoneAnalyzer
        private set
    lateinit var experienceOrbMerger: ExperienceOrbMerger
        private set
    lateinit var hopperOptimizer: HopperOptimizer
        private set
    lateinit var collisionOptimizer: CollisionOptimizer
        private set

    companion object {
        lateinit var instance: FixLag
            private set
    }

    override fun onEnable() {
        instance = this
        messageManager = MessageManager(this)
        messageManager.logInfo("log_startup_version", "<version>", description.version)

        configManager = ConfigManager(this, messageManager)
        messageManager.logInfo("log_startup_config_loaded")

        performanceMonitor = PerformanceMonitor(messageManager, logger)
        deletedItemsManager = DeletedItemsManager(this)
        chunkAnalyzer = ChunkAnalyzer(this, messageManager)
        explosionOptimizer = ExplosionOptimizer(this)
        dynamicDistanceManager = DynamicDistanceManager(this)
        lagNotifier = LagNotifier(this)
        chunkEntityLimiter = ChunkEntityLimiter(this, configManager)
        villagerLobotomizer = VillagerLobotomizer(this, configManager)
        panicModeManager = PanicModeManager(this)
        spawnerOptimizer = SpawnerOptimizer(this)
        armorStandOptimizer = ArmorStandOptimizer(this)
        redstoneAnalyzer = RedstoneAnalyzer(this)
        experienceOrbMerger = ExperienceOrbMerger(this)
        hopperOptimizer = HopperOptimizer(this)
        collisionOptimizer = CollisionOptimizer(this)
        val configOptimizer = ServerConfigOptimizer(this, messageManager)
        taskManager = TaskManager(this, configManager, messageManager, performanceMonitor, deletedItemsManager, dynamicDistanceManager, lagNotifier)
        updateChecker = UpdateChecker(this, configManager, messageManager)
        commandManager = CommandManager(this, taskManager, performanceMonitor, messageManager, deletedItemsManager, chunkAnalyzer, configOptimizer, redstoneAnalyzer)
        mobStacker = MobStacker(this, configManager)

        messageManager.logInfo("log_startup_features_header")
        logFeature("Mob Stacking", configManager.isMobStackingEnabled)
        logFeature("Smart Clear", configManager.isSmartClearEnabled)
        logFeature("Panic Mode", configManager.isPanicModeEnabled)
        logFeature("Spawner Optimizer", configManager.isSpawnerOptimizerEnabled)
        logFeature("Armor Stand Optimizer", configManager.isArmorStandOptimizerEnabled)
        logFeature("XP Orb Merger", configManager.isXpOrbMergerEnabled)
        logFeature("Explosion Optimization", configManager.isExplosionOptimizationEnabled)
        logFeature("Dynamic Distance", configManager.isDynamicDistanceEnabled)
        logFeature("Lag Notifications", configManager.isLagNotificationsEnabled)
        logFeature("Hopper Optimizer", configManager.isHopperOptimizerEnabled)
        logFeature("Collision Optimizer", configManager.isCollisionOptimizerEnabled)
        logFeature("Chunk Entity Limits", configManager.isChunkEntityLimitsEnabled)
        logFeature("Villager Lobotomizer", configManager.isVillagerLobotomizationEnabled)

        server.pluginManager.registerEvents(mobStacker, this)
        server.pluginManager.registerEvents(explosionOptimizer, this)
        server.pluginManager.registerEvents(deletedItemsManager, this)
        server.pluginManager.registerEvents(chunkEntityLimiter, this)
        server.pluginManager.registerEvents(spawnerOptimizer, this)
        server.pluginManager.registerEvents(armorStandOptimizer, this)
        server.pluginManager.registerEvents(hopperOptimizer, this)
        messageManager.logInfo("log_startup_events_registered")

        taskManager.startDeletionTask()
        taskManager.startSmartClearTask()
        taskManager.startDynamicDistanceTask()
        taskManager.startLagNotifierTask()
        villagerLobotomizer.runTaskTimer(this, 20L * 30, 20L * 60)
        collisionOptimizer.runTaskTimer(this, 20L * 20, configManager.collisionOptimizerCheckIntervalTicks.toLong())
        if (configManager.isPanicModeEnabled) {
            panicModeManager.runTaskTimer(this, 20L * 15, configManager.panicModeCheckIntervalTicks.toLong())
        }
        if (configManager.isXpOrbMergerEnabled) {
            experienceOrbMerger.runTaskTimer(this, 20L * 5, configManager.xpOrbMergerCheckIntervalTicks.toLong())
        }
        updateChecker.startUpdateCheckTask()
        messageManager.logInfo("log_startup_tasks_started")

        Metrics(this, 28156)

        messageManager.logInfo("log_startup_ready", "<version>", description.version)
    }

    private fun logFeature(name: String, enabled: Boolean) {
        val status = if (enabled) "<green>enabled" else "<gray>disabled"
        messageManager.logInfo("log_startup_feature_status", "<feature>", name, "<status>", status)
    }

    override fun onDisable() {
        messageManager.logInfo("log_plugin_disabled")
        messageManager.logInfo("log_plugin_goodbye")
    }
}
