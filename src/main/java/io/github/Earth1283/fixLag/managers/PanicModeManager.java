package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PanicModeManager extends BukkitRunnable {

    private final FixLag plugin;
    private final ConfigManager config;
    private final MessageManager messageManager;
    private boolean isPanicModeActive = false;
    private final Set<UUID> frozenEntities = new HashSet<>();

    public PanicModeManager(FixLag plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public void run() {
        if (!config.isPanicModeEnabled()) return;

        double tps = Bukkit.getServer().getTPS()[0];

        if (!isPanicModeActive && tps < config.getPanicModeTpsThreshold()) {
            activatePanicMode();
        } else if (isPanicModeActive && tps >= config.getPanicModeRecoverTps()) {
            deactivatePanicMode();
        }
    }

    private void activatePanicMode() {
        isPanicModeActive = true;
        int frozenCount = 0;

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (mob.getCustomName() != null) continue;
                    if (mob instanceof Tameable && ((Tameable) mob).isTamed()) continue;
                    
                    if (mob.isAware()) {
                        mob.setAware(false);
                        frozenEntities.add(mob.getUniqueId());
                        frozenCount++;
                    }
                }
            }
        }

        plugin.getLogger().warning("[Panic Mode] Activated! TPS dropped below threshold. Froze " + frozenCount + " mobs to save CPU.");
    }

    private void deactivatePanicMode() {
        isPanicModeActive = false;
        int thawedCount = 0;

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (frozenEntities.contains(entity.getUniqueId())) {
                    if (entity instanceof Mob) {
                        ((Mob) entity).setAware(true);
                        thawedCount++;
                    }
                }
            }
        }
        
        frozenEntities.clear();
        plugin.getLogger().info("[Panic Mode] Deactivated! TPS recovered. Unfroze " + thawedCount + " mobs.");
    }
}
