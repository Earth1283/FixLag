package io.github.Earth1283.fixLag;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.logging.Level;

public class TaskManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final PerformanceMonitor performanceMonitor;

    public TaskManager(JavaPlugin plugin, ConfigManager configManager, MessageManager messageManager, PerformanceMonitor performanceMonitor) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.performanceMonitor = performanceMonitor;
    }

    public void startDeletionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (configManager.getEntitiesToDelete() == null || configManager.getEntitiesToDelete().isEmpty()) {
                    return;
                }

                if (configManager.isEnableWarning()) {
                    long warningSchedule = configManager.getDeletionIntervalTicks() - configManager.getWarningTimeTicks();
                    if (warningSchedule >= 0) {
                        Bukkit.getScheduler().runTaskLater(plugin, TaskManager.this::sendWarning, warningSchedule);
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, TaskManager.this::deleteAndAnnounce, configManager.getDeletionIntervalTicks());
            }
        }.runTaskTimer(plugin, 0L, configManager.getDeletionIntervalTicks());
    }

    private void sendWarning() {
        long warningTimeSeconds = configManager.getWarningTimeTicks() / 20L;
        String formattedMessage = messageManager.getMessage("entity_clear_warning", "%time%", String.valueOf(warningTimeSeconds));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formattedMessage);
        }
    }

    public void deleteAndAnnounce() {
        int deletedCount = deleteEntities();
        if (deletedCount > 0) {
            String broadcastMessage = messageManager.getMessage("entity_clear_broadcast", "%count%", String.valueOf(deletedCount));
            Bukkit.getServer().broadcast(Component.text(broadcastMessage));
            if (configManager.isLogMemoryStats()) {
                performanceMonitor.logMemoryUsage();
            }
            plugin.getLogger().log(Level.INFO, messageManager.getLogMessage("log_entity_deleted", "%count%", String.valueOf(deletedCount)));
        }
    }

    private int deleteEntities() {
        int deletedEntities = 0;
        Set<String> entitiesToDelete = configManager.getEntitiesToDelete();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.isValid() && entitiesToDelete.contains(entity.getType().name())) {
                    entity.remove();
                    deletedEntities++;
                }
            }
        }
        return deletedEntities;
    }
}
