package io.github.Earth1283.fixLag.listeners;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CollisionOptimizer extends BukkitRunnable {

    private final FixLag plugin;
    private final Set<UUID> disabledCollisions = new HashSet<>();

    public CollisionOptimizer(FixLag plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().isCollisionOptimizerEnabled()) {
            if (!disabledCollisions.isEmpty()) {
                restoreAll();
            }
            return;
        }

        double tps = Bukkit.getServer().getTPS()[0];
        if (tps < plugin.getConfigManager().getCollisionOptimizerTpsThreshold()) {
            optimize();
        } else {
            if (!disabledCollisions.isEmpty()) {
                restoreAll();
            }
        }
    }

    private void optimize() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (shouldDisableCollision(entity)) {
                    LivingEntity living = (LivingEntity) entity;
                    if (living.isCollidable()) {
                        living.setCollidable(false);
                        disabledCollisions.add(entity.getUniqueId());
                    }
                }
            }
        }
    }

    private boolean shouldDisableCollision(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof Player) return false;
        
        return entity instanceof Animals || entity instanceof Monster || entity instanceof Ambient;
    }

    private void restoreAll() {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : disabledCollisions) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setCollidable(true);
            }
            toRemove.add(uuid);
        }
        disabledCollisions.removeAll(toRemove);
    }
}
