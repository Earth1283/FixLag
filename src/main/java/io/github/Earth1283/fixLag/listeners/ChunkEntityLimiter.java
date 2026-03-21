package io.github.Earth1283.fixLag.listeners;

import io.github.Earth1283.fixLag.FixLag;
import io.github.Earth1283.fixLag.managers.ConfigManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Map;

public class ChunkEntityLimiter implements Listener {

    private final FixLag plugin;
    private final ConfigManager config;

    public ChunkEntityLimiter(FixLag plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!config.isChunkEntityLimitsEnabled()) return;

        EntityType type = event.getEntityType();
        String typeName = type.name();
        Map<String, Integer> limits = config.getChunkEntityLimits();

        if (limits.containsKey(typeName)) {
            // Check skip rules before counting/applying limit to current event
            if (config.isChunkEntityLimitsSkipNamed() && event.getEntity().getCustomName() != null) {
                return;
            }
            if (config.isChunkEntityLimitsSkipTamed() && event.getEntity() instanceof Tameable) {
                Tameable tameable = (Tameable) event.getEntity();
                if (tameable.isTamed()) {
                    return;
                }
            }

            int limit = limits.get(typeName);
            Chunk chunk = event.getLocation().getChunk();
            
            int count = 0;
            for (Entity e : chunk.getEntities()) {
                if (e.getType() == type) {
                    count++;
                }
            }

            if (count >= limit) {
                event.setCancelled(true);
            }
        }
    }
}
