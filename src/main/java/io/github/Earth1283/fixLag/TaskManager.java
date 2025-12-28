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
import org.bukkit.scheduler.BukkitTask;

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
    private final DynamicDistanceManager dynamicDistanceManager;
    private BukkitTask deletionTask;
    private BukkitTask smartClearTask;
    private BukkitTask dynamicDistanceTask;

    public TaskManager(FixLag plugin, ConfigManager configManager, MessageManager messageManager, PerformanceMonitor performanceMonitor, DeletedItemsManager deletedItemsManager, DynamicDistanceManager dynamicDistanceManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.performanceMonitor = performanceMonitor;
        this.deletedItemsManager = deletedItemsManager;
        this.dynamicDistanceManager = dynamicDistanceManager;
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
        if (smartClearTask != null) {
            smartClearTask.cancel();
        }
        if (configManager.isSmartClearEnabled()) {
            smartClearTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkSmartClear, 20L * 30, configManager.getSmartClearCheckIntervalTicks());
        }
    }

    public void startDynamicDistanceTask() {
        if (dynamicDistanceTask != null) {
            dynamicDistanceTask.cancel();
        }
        if (configManager.isDynamicDistanceEnabled()) {
            dynamicDistanceTask = dynamicDistanceManager.runTaskTimer(plugin, 20L * 60, configManager.getDynamicDistanceCheckIntervalTicks());
        }
    }

    private long lastSmartClearTime = 0;

    private void checkSmartClear() {
        if (!configManager.isSmartClearEnabled()) return;

        double tps = Bukkit.getServer().getTPS()[0];
        if (tps < configManager.getSmartClearTpsThreshold()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSmartClearTime >= configManager.getSmartClearCooldownTicks() * 50L) {
                lastSmartClearTime = currentTime;
                plugin.getLogger().info(messageManager.getLogMessage("log_smart_clear_triggered", "<tps>", String.format("%.2f", tps)));
                deleteAndAnnounce();
            }
        }
    }

    private void sendWarning() {
        long warningTimeSeconds = configManager.getWarningTimeTicks() / 20L;
        
        if ("ACTION_BAR".equalsIgnoreCase(configManager.getNotificationType())) {
            Component warningComponent = messageManager.getComponentMessage("entity_clear_warning", "%fixlag_time%", String.valueOf(warningTimeSeconds));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(warningComponent);
            }
        } else {
            String formattedMessage = messageManager.getMessage("entity_clear_warning", "%fixlag_time%", String.valueOf(warningTimeSeconds));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(formattedMessage);
            }
        }
    }

    public void deleteAndAnnounce() {
        List<ItemStack> deletedItems = deleteEntities();
        int deletedCount = deletedItems.size();
        if (deletedCount > 0) {
            deletedItemsManager.addDeletedItems(deletedItems);
            
            if ("ACTION_BAR".equalsIgnoreCase(configManager.getNotificationType())) {
                 Component broadcastComponent = messageManager.getComponentMessage("entity_clear_broadcast", "%fixlag_count%", String.valueOf(deletedCount));
                 for (Player player : Bukkit.getOnlinePlayers()) {
                     player.sendActionBar(broadcastComponent);
                 }
            } else {
                String broadcastMessage = messageManager.getMessage("entity_clear_broadcast", "%fixlag_count%", String.valueOf(deletedCount));
                Bukkit.getServer().broadcast(Component.text(broadcastMessage));
            }

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
                if (!entity.isValid()) continue;

                boolean shouldDelete = entitiesToDelete.contains(entity.getType().name());
                boolean isProjectile = entity instanceof org.bukkit.entity.AbstractArrow;

                if (shouldDelete || isProjectile) {
                    if (configManager.isIgnoreCustomNamedItems() && entity.customName() != null) {
                        continue;
                    }

                    if (entity instanceof org.bukkit.entity.Trident) {
                        org.bukkit.entity.Trident trident = (org.bukkit.entity.Trident) entity;
                        if (!trident.getItemStack().getEnchantments().isEmpty()) {
                            continue;
                        }
                        deletedItems.add(trident.getItemStack());
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
