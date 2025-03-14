package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.World;
import org.bukkit.Chunk;

import java.util.List;

public class EntityCleaner {

    public void startCleanupTask() {
        int cleanupInterval = FixLag.getInstance().getConfig().getInt("entity_cleanup.cleanup_interval");

        Bukkit.getScheduler().runTaskTimer(FixLag.getInstance(), this::cleanUpEntities, cleanupInterval, cleanupInterval);
    }

    private void cleanUpEntities() {
        int maxEntitiesPerChunk = FixLag.getInstance().getConfig().getInt("entity_cleanup.max_entities_per_chunk");
        boolean cleanAllEntities = FixLag.getInstance().getConfig().getBoolean("entity_cleanup.clean_all_entities");
        List<String> mobTypesToClean = FixLag.getInstance().getConfig().getStringList("entity_cleanup.mob_cleanup_types");

        // Loop through all the worlds
        for (World world : Bukkit.getWorlds()) {
            // Iterate through all loaded chunks
            for (Chunk chunk : world.getLoadedChunks()) {
                // Get all entities in the chunk (Note: This is an array, not a list)
                Entity[] entities = chunk.getEntities();

                // If the number of entities in this chunk exceeds the threshold, we need to clean
                if (entities.length > maxEntitiesPerChunk) {
                    // Loop through each entity and decide whether to remove it
                    for (Entity entity : entities) {
                        if (cleanAllEntities) {
                            entity.remove();
                        } else if (entity instanceof Mob && mobTypesToClean.contains(entity.getType().toString())) {
                            entity.remove();
                        }
                    }
                }
            }
        }
    }
}