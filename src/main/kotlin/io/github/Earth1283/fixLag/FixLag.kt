package io.github.Earth1283.fixLag

import io.github.Earth1283.fixLag.commands.CommandManager
import io.github.Earth1283.fixLag.listeners.*
import io.github.Earth1283.fixLag.managers.*
import io.github.Earth1283.fixLag.utils.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

open class FixLag : JavaPlugin() {
    private val featureTasks = mutableListOf<BukkitTask>()
    private var metrics: Metrics? = null
    lateinit var overloadChecker: OverloadChecker
        private set

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
    lateinit var dolphinDrowningFix: DolphinDrowningFix
        private set
    lateinit var traderLlamaDespawnFix: TraderLlamaDespawnFix
        private set
    lateinit var pigTeleportExploitFix: PigTeleportExploitFix
        private set
    lateinit var tntRaycastFix: TntRaycastFix
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
        dolphinDrowningFix = DolphinDrowningFix(this)
        traderLlamaDespawnFix = TraderLlamaDespawnFix(this)
        pigTeleportExploitFix = PigTeleportExploitFix(this)
        tntRaycastFix = TntRaycastFix(this)
        val configOptimizer = ServerConfigOptimizer(this, messageManager)
        taskManager = TaskManager(this, configManager, messageManager, performanceMonitor, deletedItemsManager, dynamicDistanceManager, lagNotifier)
        updateChecker = UpdateChecker(this, configManager, messageManager)
        commandManager = CommandManager(this, taskManager, performanceMonitor, messageManager, deletedItemsManager, chunkAnalyzer, configOptimizer, redstoneAnalyzer)
        mobStacker = MobStacker(this, configManager)
        overloadChecker = OverloadChecker(this)

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
        logFeature("Dolphin Drowning Fix (MC-306840)", configManager.isDolphinDrowningFixEnabled)
        logFeature("Trader Llama Despawn Fix (MC-153831)", configManager.isTraderLlamaDespawnFixEnabled)
        logFeature("Pig Teleport Exploit Fix (MC-311414)", configManager.isPigTeleportExploitFixEnabled)
        logFeature("TNT Raycast Fix (MC-263882)", configManager.isTntRaycastFixEnabled)

        server.pluginManager.registerEvents(mobStacker, this)
        server.pluginManager.registerEvents(explosionOptimizer, this)
        server.pluginManager.registerEvents(deletedItemsManager, this)
        server.pluginManager.registerEvents(chunkEntityLimiter, this)
        server.pluginManager.registerEvents(spawnerOptimizer, this)
        server.pluginManager.registerEvents(armorStandOptimizer, this)
        server.pluginManager.registerEvents(hopperOptimizer, this)
        server.pluginManager.registerEvents(panicModeManager, this)
        server.pluginManager.registerEvents(collisionOptimizer, this)
        server.pluginManager.registerEvents(villagerLobotomizer, this)
        server.pluginManager.registerEvents(dolphinDrowningFix, this)
        server.pluginManager.registerEvents(traderLlamaDespawnFix, this)
        server.pluginManager.registerEvents(pigTeleportExploitFix, this)
        server.pluginManager.registerEvents(tntRaycastFix, this)
        messageManager.logInfo("log_startup_events_registered")

        deletedItemsManager.loadItemsFromDisk()
        restartTasks()
        messageManager.logInfo("log_startup_tasks_started")

        val metrics = Metrics(this, 28156)
        this.metrics = metrics

        metrics.addCustomChart(Metrics.AdvancedPie("enabled_features") {
            buildMap {
                if (configManager.isMobStackingEnabled) put("Mob Stacking", 1)
                if (configManager.isChunkEntityLimitsEnabled) put("Chunk Entity Limits", 1)
                if (configManager.isVillagerLobotomizationEnabled) put("Villager Lobotomization", 1)
                if (configManager.isSmartClearEnabled) put("Smart Clear", 1)
                if (configManager.isPanicModeEnabled) put("Panic Mode", 1)
                if (configManager.isSpawnerOptimizerEnabled) put("Spawner Optimizer", 1)
                if (configManager.isArmorStandOptimizerEnabled) put("Armor Stand Optimizer", 1)
                if (configManager.isXpOrbMergerEnabled) put("XP Orb Merger", 1)
                if (configManager.isExplosionOptimizationEnabled) put("Explosion Optimization", 1)
                if (configManager.isDynamicDistanceEnabled) put("Dynamic Distance", 1)
                if (configManager.isHopperOptimizerEnabled) put("Hopper Optimizer", 1)
                if (configManager.isCollisionOptimizerEnabled) put("Collision Optimizer", 1)
                if (configManager.isDeletedItemsPersistenceEnabled) put("Item Persistence", 1)
                if (configManager.isDolphinDrowningFixEnabled) put("Dolphin Drowning Fix", 1)
                if (configManager.isTraderLlamaDespawnFixEnabled) put("Trader Llama Despawn Fix", 1)
                if (configManager.isPigTeleportExploitFixEnabled) put("Pig Teleport Exploit Fix", 1)
                if (configManager.isTntRaycastFixEnabled) put("TNT Raycast Fix", 1)
            }
        })

        metrics.addCustomChart(Metrics.SimplePie("entity_clear_interval") {
            val seconds = configManager.deletionIntervalTicks / 20
            when {
                seconds <= 30 -> "≤30s"
                seconds <= 60 -> "31–60s"
                seconds <= 120 -> "61–120s"
                else -> ">120s"
            }
        })

        metrics.addCustomChart(Metrics.SimplePie("panic_mode_tps_threshold") {
            val tps = configManager.panicModeTpsThreshold
            when {
                !configManager.isPanicModeEnabled -> "Disabled"
                tps <= 10.0 -> "≤10"
                tps <= 14.0 -> "11–14"
                tps <= 16.0 -> "15–16"
                else -> ">16"
            }
        })

        messageManager.logInfo("log_startup_ready", "<version>", description.version)
    }

    private fun logFeature(name: String, enabled: Boolean) {
        val status = if (enabled) "<green>enabled" else "<gray>disabled"
        messageManager.logInfo("log_startup_feature_status", "<feature>", name, "<status>", status)
    }

    fun restartTasks() {
        deletedItemsManager.onConfigReload()
        featureTasks.forEach { it.cancel() }
        featureTasks.clear()
        taskManager.startDeletionTask()
        taskManager.startSmartClearTask()
        taskManager.startDynamicDistanceTask()
        taskManager.startLagNotifierTask()
        fun repeat(delay: Long, period: Long, work: Runnable) {
            // Schedule a wrapper: BukkitRunnable instances themselves cannot be rescheduled.
            featureTasks.add(server.scheduler.runTaskTimer(this, Runnable { work.run() }, delay, period))
        }
        if (configManager.isVillagerLobotomizationEnabled) repeat(600L, 1200L, villagerLobotomizer)
        else villagerLobotomizer.restoreAll()
        if (configManager.isCollisionOptimizerEnabled) repeat(400L, configManager.collisionOptimizerCheckIntervalTicks, collisionOptimizer)
        else collisionOptimizer.restoreAll()
        if (configManager.isPanicModeEnabled) repeat(300L, configManager.panicModeCheckIntervalTicks, panicModeManager)
        else panicModeManager.restoreAll()
        experienceOrbMerger.stopScan()
        if (configManager.isXpOrbMergerEnabled) repeat(100L, configManager.xpOrbMergerCheckIntervalTicks, experienceOrbMerger)
        if (configManager.isTntRaycastFixEnabled) repeat(1L, 1L, tntRaycastFix)
        else tntRaycastFix.run()
        overloadChecker.startChecking(600L, configManager.overloadCheckIntervalTicks)
        updateChecker.startUpdateCheckTask()
    }

    override fun onDisable() {
        featureTasks.forEach { it.cancel() }
        featureTasks.clear()
        if (::taskManager.isInitialized) taskManager.stop()
        if (::overloadChecker.isInitialized) overloadChecker.stopChecking()
        if (::experienceOrbMerger.isInitialized) experienceOrbMerger.stopScan()
        if (::panicModeManager.isInitialized) panicModeManager.restoreAll()
        if (::collisionOptimizer.isInitialized) collisionOptimizer.restoreAll()
        if (::villagerLobotomizer.isInitialized) villagerLobotomizer.restoreAll()
        if (::deletedItemsManager.isInitialized) deletedItemsManager.shutdown()
        if (::updateChecker.isInitialized) updateChecker.stop()
        metrics?.shutdown()
        if (::messageManager.isInitialized) {
            messageManager.logInfo("log_plugin_disabled")
            messageManager.logInfo("log_plugin_goodbye")
        }
    }
}
