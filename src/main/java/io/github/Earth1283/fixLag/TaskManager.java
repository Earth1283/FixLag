package io.github.Earth1283.fixLag;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class TaskManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final PerformanceMonitor performanceMonitor;
    private final DeletedItemsManager deletedItemsManager;
    private long lastSmartClearTime = 0;

    public TaskManager(JavaPlugin plugin, ConfigManager configManager, MessageManager messageManager, PerformanceMonitor performanceMonitor, DeletedItemsManager deletedItemsManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.performanceMonitor = performanceMonitor;
        this.deletedItemsManager = deletedItemsManager;
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

    public void startSmartClearTask() {
        if (!configManager.isSmartClearEnabled()) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                double tps = Bukkit.getServer().getTPS()[0]; // 1m average
                if (tps < configManager.getSmartClearTpsThreshold()) {
                    long currentTime = System.currentTimeMillis();
                    long cooldownMillis = configManager.getSmartClearCooldownTicks() / 20 * 1000;

                    if (currentTime - lastSmartClearTime >= cooldownMillis) {
                        plugin.getLogger().log(Level.WARNING, messageManager.getLogMessage("log_smart_clear_triggered", "<tps>", String.format("%.2f", tps)));
                        deleteAndAnnounce();
                        lastSmartClearTime = currentTime;
                    }
                }
            }
        }.runTaskTimer(plugin, configManager.getSmartClearCheckIntervalTicks(), configManager.getSmartClearCheckIntervalTicks());
    }

    private void sendWarning() {
        long warningTimeSeconds = configManager.getWarningTimeTicks() / 20L;
        String formattedMessage = messageManager.getMessage("entity_clear_warning", "%fixlag_time%", String.valueOf(warningTimeSeconds));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formattedMessage);
        }
    }

    public void deleteAndAnnounce() {
        List<ItemStack> deletedItems = deleteEntities();
        int deletedCount = deletedItems.size();
        if (deletedCount > 0) {
            deletedItemsManager.addDeletedItems(deletedItems);
            String broadcastMessage = messageManager.getMessage("entity_clear_broadcast", "%fixlag_count%", String.valueOf(deletedCount));
            Bukkit.getServer().broadcast(Component.text(broadcastMessage));
            if (configManager.isLogMemoryStats()) {
                performanceMonitor.logMemoryUsage();
            }
            plugin.getLogger().log(Level.INFO, messageManager.getLogMessage("log_entity_deleted", "%fixlag_count%", String.valueOf(deletedCount)));
        }
    }

    private List<ItemStack> deleteEntities() {
        List<ItemStack> deletedItems = new ArrayList<>();
        Set<String> entitiesToDelete = configManager.getEntitiesToDelete();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.isValid() && entitiesToDelete.contains(entity.getType().name())) {
                    if (configManager.isIgnoreCustomNamedItems() && entity.customName() != null) {
                        continue;
                    }

                    if (entity instanceof Item) {
                        deletedItems.add(((Item) entity).getItemStack());
                    }
                    entity.remove();
                }
            }
        }
        return deletedItems;
    }
}
