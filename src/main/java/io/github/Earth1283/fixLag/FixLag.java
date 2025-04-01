package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;

public class FixLag extends JavaPlugin {

    private static final String UPDATE_URL = "https://github.com/Earth1283/FixLag/blob/main/latest_version.txt";

    private List<String> entitiesToDelete;
    private long deletionIntervalTicks;
    private boolean enableWarning;
    private long warningTimeTicks;
    private OverloadChecker overloadChecker;
    private long overloadCheckIntervalTicks;
    private boolean logMemoryStats;
    private long updateCheckIntervalTicks;

    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        // Load default configuration if it doesn't exist
        saveDefaultConfig();
        loadConfig();
        loadMessages();

        // Initialize Overload Checker
        overloadChecker = new OverloadChecker(this, entitiesToDelete);
        startOverloadCheckTask();

        // Start the entity deletion task
        startDeletionTask();

        // Start the update check task
        startUpdateCheckTask();

        getLogger().log(Level.INFO, "FixLag plugin has been enabled!");
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private String getMessage(String key, String... replacements) {
        String message = messagesConfig.getString(key, "&cError: Message key '" + key + "' not found in messages.yml");
        message = ChatColor.translateAlternateColorCodes('&', message);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return messagesConfig.getString("prefix", "&8[&aFixLag&8] &r") + message;
    }

    private String getLogMessage(String key, String... replacements) {
        String message = messagesConfig.getString(key, "Error: Log message key '" + key + "' not found in messages.yml");
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
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
        warningTimeTicks = config.getLong("warning-time-seconds") * 20L; // Convert seconds to ticks
        overloadCheckIntervalTicks = config.getLong("overload-detection.check-interval-seconds", 30) * 20L; // Default to 30 seconds
        logMemoryStats = config.getBoolean("log-memory-stats", false);
        updateCheckIntervalTicks = config.getLong("update-check-interval-seconds", 60 * 60 * 24) * 20L; // Default to 1 day

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
        if (updateCheckIntervalTicks <= 0) {
            getLogger().log(Level.WARNING, "Update check interval is invalid. Using default value of 1 day.");
            updateCheckIntervalTicks = 60 * 60 * 24 * 20L;
        }
    }

    public void startOverloadCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Run the OverloadChecker on the main thread
                Bukkit.getScheduler().runTask(FixLag.this, () -> {
                    if (overloadChecker != null) {
                        overloadChecker.checkOverloads();
                    }
                });
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
                        String formattedMessage = getMessage("entity_clear_warning", "%fixlag_time%", String.valueOf(warningTimeSeconds / 20L));
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(formattedMessage);
                        }
                    }, warningSchedule);
                }

                // Schedule the actual entity deletion
                Bukkit.getScheduler().runTaskLaterAsynchronously(FixLag.this, () -> {
                    Bukkit.getScheduler().runTask(FixLag.this, () -> { // Run deleteEntities on the main thread
                        int deletedCount = deleteEntities();
                        if (deletedCount > 0) {
                            String broadcastMessage = getMessage("entity_clear_broadcast", "%fixlag_count%", String.valueOf(deletedCount));
                            Bukkit.getServer().broadcastMessage(broadcastMessage); // Using the recommended method
                            if (logMemoryStats) {
                                FixLag.this.logMemoryUsage();
                            }
                            getLogger().log(Level.INFO, getLogMessage("log_entity_deleted", "%fixlag_count%", String.valueOf(deletedCount)));
                        }
                    });
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

        StringBuilder gcType = new StringBuilder();
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            if (gcType.length() > 0) {
                gcType.append(", ");
            }
            gcType.append(gcBean.getName());
        }

        return getMessage("gc_info_header") + "\n" +
                ChatColor.AQUA + "Garbage Collector: " + ChatColor.GREEN + gcType.toString() + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "Heap Memory: " + ChatColor.RESET + "Used=" + ChatColor.GREEN + usedHeapMB + "MB" + ChatColor.RESET + ", Free=" + ChatColor.GREEN + freeHeapMB + "MB" + ChatColor.RESET + ", Max=" + ChatColor.GREEN + maxHeapMB + "MB" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "Non-Heap Memory: " + ChatColor.RESET + "Used=" + ChatColor.GREEN + usedNonHeapMB + "MB" + ChatColor.RESET + ", Free=" + ChatColor.GREEN + freeNonHeapMB + "MB" + ChatColor.RESET + ", Max=" + ChatColor.GREEN + maxNonHeapMB + "MB" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "GC Stats:" + ChatColor.RESET + "\n" + gcStats.toString();
    }

    public void logMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

        long usedHeapMB = heapMemoryUsage.getUsed() / (1024 * 1024);
        long maxHeapMB = heapMemoryUsage.getMax() / (1024 * 1024);
        long freeHeapMB = maxHeapMB - usedHeapMB;

        long usedNonHeapMB = nonHeapMemoryUsage.getUsed() / (1024 * 1024);
        long maxNonHeapMB = nonHeapMemoryUsage.getMax() / (1024 * 1024);
        long freeNonHeapMB = maxNonHeapMB - usedNonHeapMB;

        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder gcStats = new StringBuilder();
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            gcStats.append(gcBean.getName()).append(": Collections=").append(gcBean.getCollectionCount()).append(", Time=" + gcBean.getCollectionTime()).append("ms | ");
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
        long averageMspt1 = (long) Math.round(1000.0 / tps[0]);
        long averageMspt5 = (long) Math.round(1000.0 / tps[1]);
        long averageMspt15 = (long) Math.round(1000.0 / tps[2]);

        String jvmVersion = System.getProperty("java.version");
        String jvmName = System.getProperty("java.vm.name");

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

        return getMessage("server_info_header") + "\n" +
                ChatColor.AQUA + "JVM Version: " + ChatColor.GREEN + jvmVersion + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "JVM Name: " + ChatColor.GREEN + jvmName + ChatColor.RESET + "\n" +
                getMessage("server_info_tps", "%fixlag_tps_1m%", formatDouble(tps[0]), "%fixlag_tps_5m%", formatDouble(tps[1]), "%fixlag_tps_15m%", formatDouble(tps[2])) + "\n" +
                ChatColor.AQUA + "MSPT (Last 1m): " + ChatColor.GREEN + averageMspt1 + " ms" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "MSPT (Last 5m): " + ChatColor.GREEN + averageMspt5 + " ms" + ChatColor.RESET + "\n" +
                ChatColor.AQUA + "MSPT (Last 15m): " + ChatColor.GREEN + averageMspt15 + " ms" + ChatColor.RESET + "\n" +
                getMessage("server_info_ram", "%fixlag_used_ram%", String.valueOf(usedMemory), "%fixlag_total_ram%", String.valueOf(totalMemory), "%fixlag_ram_percentage%", formatDouble(memoryUsagePercentage)) + "\n" +
                getMessage("server_info_cpu", "%fixlag_cpu_usage%", cpuUsage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixlag")) {
            if (sender instanceof Player player) {
                if (player.isOp() || player.hasPermission("fixlag.command")) {
                    player.sendMessage(getMessage("entity_clear_manual"));
                    Bukkit.getScheduler().runTask(this, this::deleteAndAnnounce); // Run synchronously
                    return true;
                } else {
                    player.sendMessage(getMessage("permission_denied"));
                    return true;
                }
            } else {
                sender.sendMessage(getMessage("entity_clear_manual"));
                Bukkit.getScheduler().runTask(this, this::deleteAndAnnounce); // Run synchronously
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("gcinfo")) {
            if (sender instanceof Player player) {
                if (player.isOp() || player.hasPermission("fixlag.gcinfo")) {
                    player.sendMessage(getMemoryAndGCInfo());
                    return true;
                } else {
                    player.sendMessage(getMessage("permission_denied"));
                    return true;
                }
            } else {
                // Console can run this command without permission check
                sender.sendMessage(getMemoryAndGCInfo());
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("serverinfo")) {
            if (sender instanceof Player player) {
                if (player.isOp() || player.hasPermission("fixlag.serverinfo")) {
                    player.sendMessage(getServerInfo());
                    return true;
                } else {
                    player.sendMessage(getMessage("permission_denied"));
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
            String broadcastMessage = getMessage("entity_clear_broadcast", "%fixlag_count%", String.valueOf(deletedCount));
            Bukkit.getServer().broadcastMessage(broadcastMessage); // Using the recommended method
            if (logMemoryStats) {
                logMemoryUsage();
            }
            getLogger().log(Level.INFO, getLogMessage("log_entity_deleted", "%fixlag_count%", String.valueOf(deletedCount)));
        }
    }

    public void startUpdateCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String latestVersion = getLatestVersion();
                    if (latestVersion != null) {
                        if (!getDescription().getVersion().equals(latestVersion)) {
                            getLogger().log(Level.INFO, getLogMessage("log_update_available", "%fixlag_version%", latestVersion));
                            notifyUpdate(latestVersion);
                        } else {
                            getLogger().log(Level.INFO, getLogMessage("log_update_uptodate"));
                        }
                    } else {
                        getLogger().log(Level.WARNING, getLogMessage("log_update_check_failed"));
                    }
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, getLogMessage("log_update_check_error", "%fixlag_error%", e.getMessage()));
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, updateCheckIntervalTicks);
    }

    private String getLatestVersion() throws IOException {
        URL url = new URL(UPDATE_URL);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        return reader.readLine();
    }

    private void notifyUpdate(String latestVersion) {
        String message = getMessage("update_available", "%fixlag_latest_version%", latestVersion) +
                getMessage("update_current_version", "%fixlag_current_version%", getDescription().getVersion());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission("fixlag.notify.update")) {
                player.sendMessage(message);
            }
        }
    }
}