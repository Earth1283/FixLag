package io.github.Earth1283.fixLag;

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
        String raw = headerRaw + "\n" +
                "<aqua>Garbage Collector: <green>" + gcType.toString() + "</green></aqua>\n" +
                "<aqua>Heap Memory:</aqua> Used=<green>" + usedHeapMB + "MB</green>, Free=<green>" + freeHeapMB + "MB</green>, Max=<green>" + maxHeapMB + "MB</green>\n" +
                "<aqua>Non-Heap Memory:</aqua> Used=<green>" + usedNonHeapMB + "MB</green>, Free=<green>" + (freeNonHeapMB <= 0 ? "N/A" : freeNonHeapMB + "MB") + "</green>, Max=<green>" + (maxNonHeapMB <= 0 ? "N/A" : maxNonHeapMB + "MB") + "</green>\n" +
                "<aqua>GC Stats:</aqua>\n" + gcStats.toString();

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

        logger.log(Level.INFO, "Memory Stats - Heap: Used=" + usedHeapMB + "MB, Free=" + freeHeapMB + "MB, Max=" + maxHeapMB + "MB | Non-Heap: Used=" + usedNonHeapMB + "MB, Free=" + (freeNonHeapMB <= 0 ? "N/A" : freeNonHeapMB + "MB") + ", Max=" + (maxNonHeapMB <= 0 ? "N/A" : maxNonHeapMB + "MB") + " | GC: " + gcStats.toString());
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

        String headerRaw = messageManager.getRawMessage("server_info_header");
        String raw = headerRaw + "\n" +
                "<aqua>JVM Version: <green>" + jvmVersion + "</green></aqua>\n" +
                "<aqua>JVM Name: <green>" + jvmName + "</green></aqua>\n" +
                "<aqua>OS Architecture: <green>" + osArch + "</green></aqua>\n" +
                "<aqua>OS Name: <green>" + osName + "</green></aqua>\n" +
                messageManager.getRawMessage("server_info_tps")
                        .replace("%fixlag_tps_1m%", formatDouble(tps[0]))
                        .replace("%fixlag_tps_5m%", formatDouble(tps[1]))
                        .replace("%fixlag_tps_15m%", formatDouble(tps[2])) + "\n" +
                messageManager.getRawMessage("server_info_ram")
                        .replace("%fixlag_used_ram%", String.valueOf(usedMemory))
                        .replace("%fixlag_total_ram%", String.valueOf(totalMemory))
                        .replace("%fixlag_ram_percentage%", formatDouble(memoryUsagePercentage)) + "\n" +
                messageManager.getRawMessage("server_info_cpu")
                        .replace("%fixlag_cpu_usage%", cpuUsage);

        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }
}
