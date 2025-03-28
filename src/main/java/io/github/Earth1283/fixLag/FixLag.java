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

                if (enableWarning) {
                    // Schedule the warning message to be sent before deletion
                    Bukkit.getScheduler().runTaskLater(FixLag.this, () -> {
                        long warningTimeSeconds = warningTimeTicks / 20L;
                        String formattedMessage = warningMessage.replace("%time%", String.valueOf(warningTimeSeconds));
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(formattedMessage);
                        }
                    }, deletionIntervalTicks - warningTimeTicks);
                }

                // Schedule the actual entity deletion after the warning time
                Bukkit.getScheduler().runTaskLater(FixLag.this, () -> {
                    int deletedCount = deleteEntities();
                    if (deletedCount > 0) {
                        String broadcastMessage = cleanupBroadcastMessage.replace("%count%", String.valueOf(deletedCount));
                        Bukkit.broadcastMessage(broadcastMessage);
                    }
                }, deletionIntervalTicks);
            }
        }.runTaskTimer(this, deletionIntervalTicks, deletionIntervalTicks);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixlag")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.isOp() || player.hasPermission("fixlag.command")) {
                    player.sendMessage(ChatColor.GREEN + "Manually clearing unnecessary entities...");
                    int deletedCount = deleteEntities();
                    if (deletedCount > 0) {
                        String broadcastMessage = cleanupBroadcastMessage.replace("%count%", String.valueOf(deletedCount));
                        Bukkit.broadcastMessage(broadcastMessage);
                    }
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.GREEN + "Manually clearing unnecessary entities...");
                int deletedCount = deleteEntities();
                if (deletedCount > 0) {
                    String broadcastMessage = cleanupBroadcastMessage.replace("%count%", String.valueOf(deletedCount));
                    Bukkit.broadcastMessage(broadcastMessage);
                }
                return true;
            }
        }
        return false;
    }
}