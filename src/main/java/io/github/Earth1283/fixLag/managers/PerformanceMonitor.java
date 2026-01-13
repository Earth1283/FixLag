package io.github.Earth1283.fixLag.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PerformanceMonitor {

    private final MessageManager messageManager;
    private final Logger logger;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public PerformanceMonitor(MessageManager messageManager, Logger logger) {
        this.messageManager = messageManager;
        this.logger = logger;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacySection();
    }

    public String getMemoryAndGCInfo() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        long usedHeapMB = heapMemoryUsage.getUsed() / (1024 * 1024);
        long maxHeapMB = heapMemoryUsage.getMax() / (1024 * 1024);
        long freeHeapMB = maxHeapMB - usedHeapMB;

        long usedNonHeapMB = nonHeapMemoryUsage.getUsed() / (1024 * 1024);
        long maxNonHeapMB = nonHeapMemoryUsage.getMax() <= 0 ? -1 : nonHeapMemoryUsage.getMax() / (1024 * 1024);
        long freeNonHeapMB = maxNonHeapMB <= 0 ? -1 : maxNonHeapMB - usedNonHeapMB;

        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder gcStats = new StringBuilder();
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            gcStats.append(gcBean.getName()).append(": Collections=").append(gcBean.getCollectionCount()).append(", Time=").append(gcBean.getCollectionTime()).append("ms\n");
        }

        StringBuilder gcType = new StringBuilder();
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            if (gcType.length() > 0) {
                gcType.append(", ");
            }
            gcType.append(gcBean.getName());
        }

        String headerRaw = messageManager.getRawMessage("gc_info_header");
        String bodyRaw = messageManager.getRawMessage("gc_info_body");
        
        String raw = headerRaw + "\n" + bodyRaw
                .replace("<gc_type>", gcType.toString())
                .replace("<used_heap>", String.valueOf(usedHeapMB))
                .replace("<free_heap>", String.valueOf(freeHeapMB))
                .replace("<max_heap>", String.valueOf(maxHeapMB))
                .replace("<used_non_heap>", String.valueOf(usedNonHeapMB))
                .replace("<free_non_heap>", freeNonHeapMB <= 0 ? "N/A" : freeNonHeapMB + "MB")
                .replace("<max_non_heap>", maxNonHeapMB <= 0 ? "N/A" : maxNonHeapMB + "MB")
                .replace("<gc_stats>", gcStats.toString());

        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }

    public void logMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        long usedHeapMB = heapMemoryUsage.getUsed() / (1024 * 1024);
        long maxHeapMB = heapMemoryUsage.getMax() / (1024 * 1024);
        long freeHeapMB = maxHeapMB - usedHeapMB;

        long usedNonHeapMB = nonHeapMemoryUsage.getUsed() / (1024 * 1024);
        long maxNonHeapMB = nonHeapMemoryUsage.getMax() <= 0 ? -1 : nonHeapMemoryUsage.getMax() / (1024 * 1024);
        long freeNonHeapMB = maxNonHeapMB <= 0 ? -1 : maxNonHeapMB - usedNonHeapMB;

        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder gcStats = new StringBuilder();
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            gcStats.append(gcBean.getName()).append(": Collections=").append(gcBean.getCollectionCount()).append(", Time=").append(gcBean.getCollectionTime()).append("ms | ");
        }
        if (gcStats.length() > 2) {
            gcStats.setLength(gcStats.length() - 3);
        }

        logger.log(Level.INFO, messageManager.getLogMessage("log_memory_stats",
                "<used_heap>", String.valueOf(usedHeapMB),
                "<free_heap>", String.valueOf(freeHeapMB),
                "<max_heap>", String.valueOf(maxHeapMB),
                "<used_non_heap>", String.valueOf(usedNonHeapMB),
                "<free_non_heap>", freeNonHeapMB <= 0 ? "N/A" : freeNonHeapMB + "MB",
                "<max_non_heap>", maxNonHeapMB <= 0 ? "N/A" : maxNonHeapMB + "MB",
                "<gc_stats>", gcStats.toString()
        ));
    }

    private String formatDouble(double d) {
        return new DecimalFormat("#.##").format(d);
    }

    public String getServerInfo() {
        double[] tps = Bukkit.getServer().getTPS();
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        double memoryUsagePercentage = (double) usedMemory / totalMemory * 100;

        String jvmVersion = System.getProperty("java.version");
        String jvmName = System.getProperty("java.vm.name");
        String osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name");

        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        String cpuUsage = (cpuLoad >= 0) ? formatDouble(cpuLoad * 100 / ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()) + "%" : "Unavailable";

        // Calculate Average Ping
        int totalPing = 0;
        int playerCount = Bukkit.getOnlinePlayers().size();
        if (playerCount > 0) {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                totalPing += p.getPing();
            }
        }
        int avgPing = playerCount > 0 ? totalPing / playerCount : 0;

        // Calculate World Stats
        StringBuilder worldStats = new StringBuilder();
        worldStats.append(messageManager.getRawMessage("server_info_world_header")).append("\n");
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            String worldEntry = messageManager.getRawMessage("server_info_world_entry")
                    .replace("%fixlag_world_name%", world.getName())
                    .replace("%fixlag_world_chunks%", String.valueOf(world.getLoadedChunks().length))
                    .replace("%fixlag_world_entities%", String.valueOf(world.getEntityCount()));
            worldStats.append(worldEntry).append("\n");
        }

        String headerRaw = messageManager.getRawMessage("server_info_header");
        String tpsLine = messageManager.getRawMessage("server_info_tps")
                .replace("%fixlag_tps_1m%", formatDouble(tps[0]))
                .replace("%fixlag_tps_5m%", formatDouble(tps[1]))
                .replace("%fixlag_tps_15m%", formatDouble(tps[2]));

        String ramLine = messageManager.getRawMessage("server_info_ram")
                .replace("%fixlag_used_ram%", String.valueOf(usedMemory))
                .replace("%fixlag_total_ram%", String.valueOf(totalMemory))
                .replace("%fixlag_ram_percentage%", formatDouble(memoryUsagePercentage));

        String cpuLine = messageManager.getRawMessage("server_info_cpu")
                .replace("%fixlag_cpu_usage%", cpuUsage);

        String pingLine = messageManager.getRawMessage("server_info_ping")
                .replace("%fixlag_avg_ping%", String.valueOf(avgPing));

        String bodyRaw = messageManager.getRawMessage("server_info_body");

        String raw = headerRaw + "\n" + bodyRaw
                .replace("<jvm_version>", jvmVersion)
                .replace("<jvm_name>", jvmName)
                .replace("<os_arch>", osArch)
                .replace("<os_name>", osName)
                .replace("%fixlag_tps_line%", tpsLine)
                .replace("%fixlag_ram_line%", ramLine)
                .replace("%fixlag_cpu_line%", cpuLine) + "\n" +
                pingLine + "\n" +
                worldStats.toString();

        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }
}
