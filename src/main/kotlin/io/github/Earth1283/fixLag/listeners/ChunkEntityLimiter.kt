package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.managers.ConfigManager
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent

class ChunkEntityLimiter(private val plugin: FixLag, private val config: ConfigManager) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        if (!config.isChunkEntityLimitsEnabled) return

        val type = event.entityType
        val typeName = type.name
        val limits = config.chunkEntityLimits

        if (limits.containsKey(typeName)) {
            // Check skip rules before counting/applying limit to current event
            if (config.isChunkEntityLimitsSkipNamed && event.entity.customName != null) {
                return
            }
            val entity = event.entity
            if (config.isChunkEntityLimitsSkipTamed && entity is Tameable) {
                if (entity.isTamed) {
                    return
                }
            }

            val limit = limits[typeName] ?: return
            val chunk = event.location.chunk
            
            var count = 0
            for (e in chunk.entities) {
                if (e.type == type) {
                    count++
                }
            }

            if (count >= limit) {
                event.isCancelled = true
            }
        }
    }
}
