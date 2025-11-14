package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class FixLag extends JavaPlugin {

    private static final String UPDATE_URL = "https://github.com/Earth1283/FixLag/blob/main/latest_version.txt";

    private List<String> entitiesToDelete;
    private long deletionIntervalTicks;
    private boolean enableWarning;
    private long warningTimeTicks;
    private OverloadChecker overloadChecker; // Reference to the separate OverloadChecker class
    private long overloadCheckIntervalTicks;
    private boolean logMemoryStats;
    private long updateCheckIntervalTicks;

    private FileConfiguration messagesConfig;
    private MiniMessage miniMessage;
    private LegacyComponentSerializer legacySerializer;

    @Override
    public void onEnable() {
        // Load default configuration if it doesn't exist
        saveDefaultConfig();
        loadConfig();
        loadMessages();

        // Initialize MiniMessage and serializer
        miniMessage = MiniMessage.miniMessage();
        legacySerializer = LegacyComponentSerializer.legacySection();

        // Initialize Overload Checker (using the separate class)
        overloadChecker = new OverloadChecker(this, entitiesToDelete);
        // Start the overload checking task using the method from the OverloadChecker class
        // Use overloadCheckIntervalTicks for both delay and period
        overloadChecker.startChecking(overloadCheckIntervalTicks, overloadCheckIntervalTicks);


        // Start the entity deletion task
        startDeletionTask();
        // This log message might be misleading as entities are cleared periodically, not just on enable
        // getLogger().log(Level.INFO, "Cleared entities!");


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

    // Render the message using MiniMessage and return a legacy-formatted string (with color codes) ready for Bukkit sendMessage.
    private String getMessage(String key, boolean includePrefix, String... replacements) {
        String raw = messagesConfig.getString(key, "Error: Message key '" + key + "' not found in messages.yml");
        // Apply replacements on raw text before parsing
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                raw = raw.replace(replacements[i], replacements[i + 1]);
            }
        }
        if (includePrefix) {
            String prefixRaw = messagesConfig.getString("prefix", "<gray>[<green>FixLag<gray>] <reset>");
            raw = prefixRaw + raw;
        }
        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }

    // Overload for existing calls that assume prefix
    private String getMessage(String key, String... replacements) {
        return getMessage(key, true, replacements);
    }

    // Return a plain (no color/formatting) string suitable for logs using PlainTextComponentSerializer
    private String getLogMessage(String key, String... replacements) {
        String raw = messagesConfig.getString(key, "Error: Log message key '" + key + "' not found in messages.yml");
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                raw = raw.replace(replacements[i], replacements[i + 1]);
            }
        }
        Component comp = miniMessage.deserialize(raw);
        return PlainTextComponentSerializer.plainText().serialize(comp);
    }

    @Override
    public void onDisable() {
        // Stop the overload checking task
        if (overloadChecker != null) {
            overloadChecker.stopChecking();
        }
        // Stop the entity deletion task (if you have a BukkitTask for it)
        // You might need to store the BukkitTask returned by runTaskTimer
        // if you want to explicitly cancel it on disable.
        // For now, the tasks will naturally stop when the plugin is disabled,
        // but explicit cancellation is cleaner.

        getLogger().log(Level.INFO, "FixLag plugin has been disabled!");
        getLogger().log(Level.INFO, "Goodbye!");
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
                        // Corrected placeholder usage based on messages.yml structure
                        String formattedMessage = getMessage("entity_clear_warning", "%time%", String.valueOf(warningTimeSeconds));
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(formattedMessage);
                        }
                    }, warningSchedule);
                }

                // Schedule the actual entity deletion
                // Deletion must run on the main thread
                Bukkit.getScheduler().runTaskLater(FixLag.this, () -> {
                    int deletedCount = deleteEntities();
                    if (deletedCount > 0) {
                        // Corrected placeholder usage based on messages.yml structure
                        String broadcastMessage = getMessage("entity_clear_broadcast", "%count%", String.valueOf(deletedCount));
                        Bukkit.getServer().broadcastMessage(broadcastMessage); // Using the recommended method
                        if (logMemoryStats) {
                            FixLag.this.logMemoryUsage();
                        }
                        // Corrected placeholder usage based on messages.yml structure
                        getLogger().log(Level.INFO, getLogMessage("log_entity_deleted", "%count%", String.valueOf(deletedCount)));
                    }
                }, enableWarning ? deletionIntervalTicks : 0); // If warning is enabled, delay by the full interval, otherwise start immediately
            }
        }.runTaskTimer(this, 0L, deletionIntervalTicks); // Initial delay of 0 to start immediately
    }

    public int deleteEntities() {
        int deletedEntities = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                // Check if the entity is valid before attempting to remove it
                if (entity.isValid() && entitiesToDelete.contains(entity.getType().name().toUpperCase())) {
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

        // Build a MiniMessage raw string for the full GC info
        String headerRaw = messagesConfig.getString("gc_info_header", "<bold>GC Info</bold>");
        String raw = headerRaw + "\n" +
                "<aqua>Garbage Collector: <green>" + gcType.toString() + "</green></aqua>\n" +
                "<aqua>Heap Memory:</aqua> Used=<green>" + usedHeapMB + "MB</green>, Free=<green>" + freeHeapMB + "MB</green>, Max=<green>" + maxHeapMB + "MB</green>\n" +
                "<aqua>Non-Heap Memory:</aqua> Used=<green>" + usedNonHeapMB + "MB</green>, Free=<green>" + freeNonHeapMB + "MB</green>, Max=<green>" + maxNonHeapMB + "MB</green>\n" +
                "<aqua>GC Stats:</aqua>\n" + gcStats.toString();

        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }

    public void logMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getHeapMemoryUsage(); // This should probably be getNonHeapMemoryUsage()

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
        String osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name");

        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        String cpuUsage = "N/A";
        if (cpuLoad >= 0) {
            cpuUsage = formatDouble(cpuLoad * 100 / ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()) + "%";
        } else {
            cpuUsage = "Unavailable";
        }

        String headerRaw = messagesConfig.getString("server_info_header", "<bold>Server Info</bold>");
        String raw = headerRaw + "\n" +
                "<aqua>JVM Version: <green>" + jvmVersion + "</green></aqua>\n" +
                "<aqua>JVM Name: <green>" + jvmName + "</green></aqua>\n" +
                "<aqua>OS Architecture: <green>" + osArch + "</green></aqua>\n" +
                "<aqua>OS Name: <green>" + osName + "</green></aqua>\n" +
                // TPS line is taken from messages config to allow localized formatting
                messagesConfig.getString("server_info_tps", "<aqua>TPS:</aqua> 1m=<green>%tps_1m%</green>, 5m=<green>%tps_5m%</green>, 15m=<green>%tps_15m%</green>")
                        .replace("%tps_1m%", formatDouble(tps[0]))
                        .replace("%tps_5m%", formatDouble(tps[1]))
                        .replace("%tps_15m%", formatDouble(tps[2])) + "\n" +
                "<aqua>MSPT (Last 1m): <green>" + averageMspt1 + " ms</green></aqua>\n" +
                "<aqua>MSPT (Last 5m): <green>" + averageMspt5 + " ms</green></aqua>\n" +
                "<aqua>MSPT (Last 15m): <green>" + averageMspt15 + " ms</green></aqua>\n" +
                messagesConfig.getString("server_info_ram", "<aqua>RAM:</aqua> Used=<green>%used_ram%MB</green>, Total=<green>%total_ram%MB</green>, Percent=<green>%ram_percentage%</green>")
                        .replace("%used_ram%", String.valueOf(usedMemory))
                        .replace("%total_ram%", String.valueOf(totalMemory))
                        .replace("%ram_percentage%", formatDouble(memoryUsagePercentage)) + "\n" +
                messagesConfig.getString("server_info_cpu", "<aqua>CPU Usage: <green>%cpu_usage%</green></aqua>")
                        .replace("%cpu_usage%", cpuUsage);

        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixlag")) {
            if (sender instanceof Player player) {
                if (player.isOp() || player.hasPermission("fixlag.command")) {
                    player.sendMessage(getMessage("entity_clear_manual", false)); // No prefix
                    Bukkit.getScheduler().runTask(this, this::deleteAndAnnounce); // Run synchronously
                    return true;
                } else {
                    player.sendMessage(getMessage("permission_denied", false)); // No prefix
                    return true;
                }
            } else {
                sender.sendMessage(getMessage("entity_clear_manual", false)); // No prefix
                Bukkit.getScheduler().runTask(this, this::deleteAndAnnounce); // Run synchronously
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("gcinfo")) {
            if (sender instanceof Player player) {
                if (player.isOp() || player.hasPermission("fixlag.gcinfo")) {
                    player.sendMessage(getMemoryAndGCInfo()); // Already formatted
                    return true;
                } else {
                    player.sendMessage(getMessage("permission_denied", false)); // No prefix
                    return true;
                }
            } else {
                // Console can run this command without permission check
                sender.sendMessage(getMemoryAndGCInfo()); // Already formatted
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("serverinfo")) {
            if (sender instanceof Player player) {
                if (player.isOp() || player.hasPermission("fixlag.serverinfo")) {
                    player.sendMessage(getServerInfo()); // Already formatted
                    return true;
                } else {
                    player.sendMessage(getMessage("permission_denied", false)); // No prefix
                    return true;
                }
            } else {
                // Console can run this command without permission check
                sender.sendMessage(getServerInfo()); // Already formatted
                return true;
            }
        }
        return false;
    }

    private void deleteAndAnnounce() {
        int deletedCount = deleteEntities();
        if (deletedCount > 0) {
            // Corrected placeholder usage based on messages.yml structure
            String broadcastMessage = getMessage("entity_clear_broadcast", "%count%", String.valueOf(deletedCount));
            Bukkit.getServer().broadcastMessage(broadcastMessage); // Using the recommended method
            if (logMemoryStats) {
                logMemoryUsage();
            }
            // Corrected placeholder usage based on messages.yml structure
            getLogger().log(Level.INFO, getLogMessage("log_entity_deleted", "%count%", String.valueOf(deletedCount)));
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
                            // Corrected placeholder usage based on messages.yml structure
                            getLogger().log(Level.INFO, getLogMessage("log_update_available", "%version%", latestVersion));
                            notifyUpdate(latestVersion);
                        } else {
                            getLogger().log(Level.INFO, getLogMessage("log_update_uptodate"));
                        }
                    } else {
                        getLogger().log(Level.WARNING, getLogMessage("log_update_check_failed"));
                    }
                } catch (IOException e) {
                    // Corrected placeholder usage based on messages.yml structure
                    getLogger().log(Level.WARNING, getLogMessage("log_update_check_error", "%error%", e.getMessage()));
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
        // Corrected placeholder usage based on messages.yml structure
        String message = getMessage("update_available", "%latest_version%", latestVersion) +
                getMessage("update_current_version", "%current_version%", getDescription().getVersion());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission("fixlag.notify.update")) {
                player.sendMessage(message);
            }
        }
    }
}
