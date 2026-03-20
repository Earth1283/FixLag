package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class LagNotifier {

    private final FixLag plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final Map<String, Long> lastAlertTime = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#.##");

    public LagNotifier(FixLag plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
    }

    public void checkLag() {
        if (!configManager.isLagNotificationsEnabled()) return;

        long now = System.currentTimeMillis();
        long cooldownMillis = configManager.getLagNotificationsCooldownTicks() * 50; // Convert ticks to ms

        // TPS Check
        if (configManager.isLagNotificationsTpsEnabled()) {
            double tps = Bukkit.getServer().getTPS()[0];
            if (tps < configManager.getLagNotificationsTpsThreshold()) {
                triggerAlert("tps", now, cooldownMillis, "lag_alert_tps", "%fixlag_tps%", df.format(tps));
            }
        }

        // RAM Check
        if (configManager.isLagNotificationsRamEnabled()) {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
            double usedPercent = (double) heapMemoryUsage.getUsed() / heapMemoryUsage.getMax() * 100;

            if (usedPercent > configManager.getLagNotificationsRamThreshold()) {
                triggerAlert("ram", now, cooldownMillis, "lag_alert_ram", "%fixlag_ram_percent%", df.format(usedPercent));
            }
        }

        // Ping Check
        if (configManager.isLagNotificationsPingEnabled()) {
            int totalPing = 0;
            int playerCount = Bukkit.getOnlinePlayers().size();
            if (playerCount > 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    totalPing += p.getPing();
                }
                int avgPing = totalPing / playerCount;

                if (avgPing > configManager.getLagNotificationsPingThreshold()) {
                    triggerAlert("ping", now, cooldownMillis, "lag_alert_ping", "%fixlag_avg_ping%", String.valueOf(avgPing));
                }
            }
        }
    }

    private void triggerAlert(String type, long now, long cooldownMillis, String messageKey, String... replacements) {
        long lastAlert = lastAlertTime.getOrDefault(type, 0L);
        if (now - lastAlert < cooldownMillis) return;

        String message = messageManager.getMessage(messageKey, true, replacements);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fixlag.notify.lag") || player.isOp()) {
                player.sendMessage(message);
            }
        }

        plugin.getLogger().warning("[Lag Alert] " + type.toUpperCase() + ": " + message);
        lastAlertTime.put(type, now);
    }
}
