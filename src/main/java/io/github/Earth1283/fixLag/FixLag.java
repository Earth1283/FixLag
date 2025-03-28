package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;

public class FixLag extends JavaPlugin {

    private List<String> entitiesToDelete;
    private long deletionIntervalTicks;
    private boolean enableWarning;
    private String warningMessage;
    private long warningTimeTicks;
    private OverloadChecker overloadChecker;
    private long overloadCheckIntervalTicks;
    private String cleanupBroadcastMessage;
    private boolean logMemoryStats;

    @Override
    public void onEnable() {
        // Load default configuration if it doesn't exist
        saveDefaultConfig();
        loadConfig();

        // Initialize Overload Checker
        overloadChecker = new OverloadChecker(this, entitiesToDelete);
        startOverloadCheckTask();

        // Start the entity deletion task
        startDeletionTask();

        getLogger().log(Level.INFO, "FixLag plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "FixLag plugin has been disabled!");
    }

    public void loadConfig() {
        FileConfiguration config = getConfig();
        entitiesToDelete = config.getStringList("entities-to-delete");
        deletionIntervalTicks = config.getLong("deletion-interval-seconds") * 20L; // Convert seconds to ticks
        enableWarning = config.getBoolean("enable-warning");
        warningMessage = ChatColor.translateAlternateColorCodes('&', config.getString("warning-message", "&eEntities will be cleared in &6%time% &eseconds."));
        warningTimeTicks = config.getLong("warning-time-seconds") * 20L; // Convert seconds to ticks
        overloadCheckIntervalTicks = config.getLong("overload-detection.check-interval-seconds", 30) * 20L; // Default to 30 seconds
        cleanupBroadcastMessage = ChatColor.translateAlternateColorCodes('&', config.getString("cleanup-broadcast-message", "&aCleaned up &2%count% &aunnecessary entities."));
        logMemoryStats = config.getBoolean("log-memory-stats", false);

        // Basic validation
        if (deletionIntervalTicks <= 0) {
            getLogger().log(Level.WARNING, "Deletion interval is invalid. Using default value of 60 seconds.");
            deletionIntervalTicks = 60 * 20L;
        }
        if (warningTimeTicks < 0) {
            getLogger().log(Level.WARNING, "Warning time is invalid. Using default value of 5 seconds.");
            warningTimeTicks = 5 * 20L;
        }
        if (overloadCheckIntervalTicks <= 0) {
            getLogger().log(Level.WARNING, "Overload check interval is invalid. Using default value of 30 seconds.");
            overloadCheckIntervalTicks = 30 * 20L;
        }
    }

    public void startOverloadCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (overloadChecker != null) {
                    overloadChecker.checkOverloads();
                }
            }
        }.runTaskTimerAsynchronously(this, overloadCheckIntervalTicks, overloadCheckIntervalTicks);
    }

    public void startDeletionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entitiesToDelete == null || entitiesToDelete.isEmpty()) {
                    return; // No entities to delete
                }

                final long warningSchedule = deletionIntervalTicks - warningTimeTicks;
                if (enableWarning && warningSchedule >= 0) {
                    // Schedule the warning message to be sent before deletion
                    Bukkit.getScheduler().runTaskLater(FixLag.this, () -> {
                        long warningTimeSeconds = warningTimeTicks / 20L;
                        String formattedMessage = warningMessage.replace("%time%", String.valueOf(warningTimeSeconds));
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(formattedMessage);
                        }
                    }, warningSchedule);
                }

                // Schedule the actual entity deletion
                Bukkit.getScheduler().runTaskLaterAsynchronously(FixLag.this, () -> {
                    int deletedCount = deleteEntities();
                    if (deletedCount > 0) {
                        String broadcastMessage = cleanupBroadcastMessage.replace("%count%", String.valueOf(deletedCount));
                        Bukkit.broadcastMessage(broadcastMessage);
                        if (logMemoryStats) {
                            FixLag.this.logMemoryUsage();
                        }
                    }
                }, enableWarning ? deletionIntervalTicks : 0); // If warning is enabled, delay by the full interval, otherwise start immediately
            }
        }.runTaskTimer(this, 0L, deletionIntervalTicks); // Initial delay of 0 to start immediately
    }

    public int deleteEntities() {
        int deletedEntities = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entitiesToDelete.contains(entity.getType().name().toUpperCase())) {
                    entity.remove();
                    deletedEntities++;
                }
            }
        }

        if (deletedEntities > 0) {
            getLogger().log(Level.INFO, "Deleted " + deletedEntities + " unnecessary entities.");
        }
        return deletedEntities;
    }

    public String getMemoryAndGCInfo() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        long usedHeapMB = heapMemoryUsage.getUsed() / (1024 * 1024);
        long maxHeapMB = heapMemoryUsage.getMax() / (1024 * 1024);
        long freeHeapMB = maxHeapMB - usedHeapMB;

        long usedNonHeapMB = nonHeapMemoryUsage.getUsed() / (1024 * 1024);
        long maxNonHeapMB = nonHeapMemoryUsage.getMax() / (1024 * 1024);
        long freeNonHeapMB = maxNonHeapMB - usedNonHeapMB;

        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder gcStats = new StringBuilder();
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            gcStats.append(gcBean.getName()).append(": Collections=").append(gcBean.getCollectionCount()).append(", Time=").append(gcBean.getCollectionTime()).append("ms\n");
        }

        return ChatColor.YELLOW + "--- JVM Memory & GC Info ---" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "Heap Memory: " + ChatColor.RESET + "Used=" + ChatColor.GREEN + usedHeapMB + "MB" + ChatColor.RESET + ", Free=" + ChatColor.GREEN + freeHeapMB + "MB" + ChatColor.RESET + ", Max=" + ChatColor.GREEN + maxHeapMB + "MB" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "Non-Heap Memory: " + ChatColor.RESET + "Used=" + ChatColor.GREEN + usedNonHeapMB + "MB" + ChatColor.RESET + ", Free=" + ChatColor.GREEN + freeNonHeapMB + "MB" + ChatColor.RESET + ", Max=" + ChatColor.GREEN + maxNonHeapMB + "MB" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "Garbage Collectors:" + ChatColor.RESET + "\n" + gcStats.toString();
    }

    public void logMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        long usedHeapMB = heapMemoryUsage.getUsed() / (1024 * 1024);
        long maxHeapMB = heapMemoryUsage.getMax() / (1024 * 1024);
        long freeHeapMB = maxHeapMB - usedHeapMB;

        long usedNonHeapMB = nonHeapMemoryUsage.getUsed() / (1024 * 1024);
        long maxNonHeapMB = nonHeapMemoryUsage.getMax() / (1024 * 1024);
        long freeNonHeapMB = maxNonHeapMB - usedNonHeapMB;

        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder gcStats = new StringBuilder();
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            gcStats.append(gcBean.getName()).append(": Collections=").append(gcBean.getCollectionCount()).append(", Time=").append(gcBean.getCollectionTime()).append("ms | ");
        }
        if (gcStats.length() > 2) {
            gcStats.setLength(gcStats.length() - 3); // Remove the trailing " | "
        }

        getLogger().log(Level.INFO, "Memory Stats - Heap: Used=" + usedHeapMB + "MB, Free=" + freeHeapMB + "MB, Max=" + maxHeapMB + "MB | Non-Heap: Used=" + usedNonHeapMB + "MB, Free=" + freeNonHeapMB + "MB, Max=" + maxNonHeapMB + "MB | GC: " + gcStats.toString());
    }

    private String formatDouble(double d) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(d);
    }

    public String getServerInfo() {
        double[] tps = Bukkit.getServer().getTPS();
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        double memoryUsagePercentage = (double) usedMemory / totalMemory * 100;

        // MSPT calculation (approximation)
        long averageMspt = (long) (1000.0 / tps[0]);

        // Getting CPU Usage in Java is platform-dependent and can be complex.
        // This provides a basic indication but might not be perfectly accurate.
        // More robust solutions might involve external libraries or JMX.
        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        String cpuUsage = "N/A";
        if (cpuLoad >= 0) {
            cpuUsage = formatDouble(cpuLoad * 100 / ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()) + "%";
        } else {
            cpuUsage = "Unavailable";
        }

        return ChatColor.YELLOW + "--- Server Information ---" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "TPS (Last 1m, 5m, 15m): " + ChatColor.GREEN + formatDouble(tps[0]) + ", " + formatDouble(tps[1]) + ", " + formatDouble(tps[2]) + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "MSPT (Approx): " + ChatColor.GREEN + averageMspt + " ms" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "RAM Usage: " + ChatColor.GREEN + usedMemory + "MB / " + totalMemory + "MB [" + formatDouble(memoryUsagePercentage) + "%]" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "CPU Usage: " + ChatColor.GREEN + cpuUsage + ChatColor.RESET;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixlag")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.isOp() || player.hasPermission("fixlag.command")) {
                    player.sendMessage(ChatColor.GREEN + "Manually clearing unnecessary entities...");
                    Bukkit.getScheduler().runTaskAsynchronously(this, this::deleteAndAnnounce);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.GREEN + "Manually clearing unnecessary entities...");
                Bukkit.getScheduler().runTaskAsynchronously(this, this::deleteAndAnnounce);
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("gcinfo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.isOp() || player.hasPermission("fixlag.gcinfo")) {
                    player.sendMessage(getMemoryAndGCInfo());
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
            } else {
                // Console can run this command without permission check
                sender.sendMessage(getServerInfo());
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("serverinfo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.isOp() || player.hasPermission("fixlag.serverinfo")) {
                    player.sendMessage(getServerInfo());
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
            } else {
                // Console can run this command without permission check
                sender.sendMessage(getServerInfo());
                return true;
            }
        }
        return false;
    }

    private void deleteAndAnnounce() {
        int deletedCount = deleteEntities();
        if (deletedCount > 0) {
            String broadcastMessage = cleanupBroadcastMessage.replace("%count%", String.valueOf(deletedCount));
            Bukkit.broadcastMessage(broadcastMessage);
            if (logMemoryStats) {
                logMemoryUsage();
            }
        }
    }
}