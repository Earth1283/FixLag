package io.github.Earth1283.fixLag.managers

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import java.util.logging.Level
import java.util.logging.Logger

class PerformanceMonitor(private val messageManager: MessageManager, private val logger: Logger) {

    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
    private val legacySerializer: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()

    fun getMemoryAndGCInfo(): String {
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        val heapMemoryUsage = memoryMXBean.heapMemoryUsage
        val nonHeapMemoryUsage = memoryMXBean.nonHeapMemoryUsage

        val usedHeapMB = heapMemoryUsage.used / (1024 * 1024)
        val maxHeapMB = heapMemoryUsage.max / (1024 * 1024)
        val freeHeapMB = maxHeapMB - usedHeapMB

        val usedNonHeapMB = nonHeapMemoryUsage.used / (1024 * 1024)
        val maxNonHeapMB = if (nonHeapMemoryUsage.max <= 0) -1 else nonHeapMemoryUsage.max / (1024 * 1024)
        val freeNonHeapMB = if (maxNonHeapMB <= 0) -1 else maxNonHeapMB - usedNonHeapMB

        val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()
        val gcStats = StringBuilder()
        for (gcBean in gcMXBeans) {
            gcStats.append(gcBean.name).append(": Collections=").append(gcBean.collectionCount).append(", Time=").append(gcBean.collectionTime).append("ms\n")
        }

        val gcType = gcMXBeans.joinToString(", ") { it.name }

        val headerRaw = messageManager.getRawMessage("gc_info_header")
        val bodyRaw = messageManager.getRawMessage("gc_info_body")

        val raw = headerRaw + "\n" + bodyRaw
            .replace("<gc_type>", gcType)
            .replace("<used_heap>", usedHeapMB.toString())
            .replace("<free_heap>", freeHeapMB.toString())
            .replace("<max_heap>", maxHeapMB.toString())
            .replace("<used_non_heap>", usedNonHeapMB.toString())
            .replace("<free_non_heap>", if (freeNonHeapMB <= 0) "N/A" else "${freeNonHeapMB}MB")
            .replace("<max_non_heap>", if (maxNonHeapMB <= 0) "N/A" else "${maxNonHeapMB}MB")
            .replace("<gc_stats>", gcStats.toString())

        val comp = miniMessage.deserialize(raw)
        return legacySerializer.serialize(comp)
    }

    fun logMemoryUsage() {
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        val heapMemoryUsage = memoryMXBean.heapMemoryUsage
        val nonHeapMemoryUsage = memoryMXBean.nonHeapMemoryUsage

        val usedHeapMB = heapMemoryUsage.used / (1024 * 1024)
        val maxHeapMB = heapMemoryUsage.max / (1024 * 1024)
        val freeHeapMB = maxHeapMB - usedHeapMB

        val usedNonHeapMB = nonHeapMemoryUsage.used / (1024 * 1024)
        val maxNonHeapMB = if (nonHeapMemoryUsage.max <= 0) -1 else nonHeapMemoryUsage.max / (1024 * 1024)
        val freeNonHeapMB = if (maxNonHeapMB <= 0) -1 else maxNonHeapMB - usedNonHeapMB

        val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()
        val gcStats = gcMXBeans.joinToString(" | ") { gcBean ->
            "${gcBean.name}: Collections=${gcBean.collectionCount}, Time=${gcBean.collectionTime}ms"
        }

        logger.log(Level.INFO, messageManager.getLogMessage("log_memory_stats",
            "<used_heap>", usedHeapMB.toString(),
            "<free_heap>", freeHeapMB.toString(),
            "<max_heap>", maxHeapMB.toString(),
            "<used_non_heap>", usedNonHeapMB.toString(),
            "<free_non_heap>", if (freeNonHeapMB <= 0) "N/A" else "${freeNonHeapMB}MB",
            "<max_non_heap>", if (maxNonHeapMB <= 0) "N/A" else "${maxNonHeapMB}MB",
            "<gc_stats>", gcStats
        ))
    }

    private fun formatDouble(d: Double): String {
        return DecimalFormat("#.##").format(d)
    }

    fun getServerInfo(): String {
        val tps = Bukkit.getServer().tps
        val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)
        val totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024)
        val memoryUsagePercentage = usedMemory.toDouble() / totalMemory * 100

        val jvmVersion = System.getProperty("java.version")
        val jvmName = System.getProperty("java.vm.name")
        val osArch = System.getProperty("os.arch")
        val osName = System.getProperty("os.name")

        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val cpuLoad = osBean.systemLoadAverage
        val cpuUsage = if (cpuLoad >= 0) formatDouble(cpuLoad * 100 / osBean.availableProcessors.toDouble()) + "%" else "Unavailable"

        // Calculate Average Ping
        val onlinePlayers = Bukkit.getOnlinePlayers()
        val playerCount = onlinePlayers.size
        var totalPing = 0
        if (playerCount > 0) {
            for (p in onlinePlayers) {
                totalPing += p.ping
            }
        }
        val avgPing = if (playerCount > 0) totalPing / playerCount else 0

        // Calculate World Stats
        val worldStats = StringBuilder()
        worldStats.append(messageManager.getRawMessage("server_info_world_header")).append("\n")
        for (world in Bukkit.getWorlds()) {
            val worldEntry = messageManager.getRawMessage("server_info_world_entry")
                .replace("%fixlag_world_name%", world.name)
                .replace("%fixlag_world_chunks%", world.loadedChunks.size.toString())
                .replace("%fixlag_world_entities%", world.entityCount.toString())
            worldStats.append(worldEntry).append("\n")
        }

        val headerRaw = messageManager.getRawMessage("server_info_header")
        val tpsLine = messageManager.getRawMessage("server_info_tps")
            .replace("%fixlag_tps_1m%", formatDouble(tps[0]))
            .replace("%fixlag_tps_5m%", formatDouble(tps[1]))
            .replace("%fixlag_tps_15m%", formatDouble(tps[2]))

        val ramLine = messageManager.getRawMessage("server_info_ram")
            .replace("%fixlag_used_ram%", usedMemory.toString())
            .replace("%fixlag_total_ram%", totalMemory.toString())
            .replace("%fixlag_ram_percentage%", formatDouble(memoryUsagePercentage))

        val cpuLine = messageManager.getRawMessage("server_info_cpu")
            .replace("%fixlag_cpu_usage%", cpuUsage)

        val pingLine = messageManager.getRawMessage("server_info_ping")
            .replace("%fixlag_avg_ping%", avgPing.toString())

        val bodyRaw = messageManager.getRawMessage("server_info_body")

        val raw = headerRaw + "\n" + bodyRaw
            .replace("<jvm_version>", jvmVersion)
            .replace("<jvm_name>", jvmName)
            .replace("<os_arch>", osArch)
            .replace("<os_name>", osName)
            .replace("%fixlag_tps_line%", tpsLine)
            .replace("%fixlag_ram_line%", ramLine)
            .replace("%fixlag_cpu_line%", cpuLine) + "\n" +
            pingLine + "\n" +
            worldStats.toString()

        val comp = miniMessage.deserialize(raw)
        return legacySerializer.serialize(comp)
    }
}
