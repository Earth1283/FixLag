package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ExperienceOrbMerger extends BukkitRunnable {

    private final FixLag plugin;
    private final ConfigManager config;

    public ExperienceOrbMerger(FixLag plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override
    public void run() {
        if (!config.isXpOrbMergerEnabled()) return;

        double radius = config.getXpOrbMergerRadius();
        Set<ExperienceOrb> processed = new HashSet<>();

        for (World world : Bukkit.getWorlds()) {
            for (ExperienceOrb orb : world.getEntitiesByClass(ExperienceOrb.class)) {
                if (processed.contains(orb) || orb.isDead()) continue;

                Collection<Entity> nearby = world.getNearbyEntities(orb.getLocation(), radius, radius, radius, e -> e instanceof ExperienceOrb && e != orb && !e.isDead() && !processed.contains(e));
                
                if (!nearby.isEmpty()) {
                    int totalXp = orb.getExperience();
                    for (Entity e : nearby) {
                        ExperienceOrb other = (ExperienceOrb) e;
                        totalXp += other.getExperience();
                        other.remove();
                        processed.add(other);
                    }
                    orb.setExperience(totalXp);
                }
                processed.add(orb);
            }
        }
    }
}
