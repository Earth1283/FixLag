package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.managers.ConfigManager
import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class MobStacker(plugin: JavaPlugin, private val configManager: ConfigManager) : Listener {

    private val stackKey = NamespacedKey(plugin, "stack_size")

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!configManager.isMobStackingEnabled) return
        if (event.spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM) return

        val entity = event.entity
        if (!configManager.mobStackingAllowedEntities.contains(entity.type.name)) return
        
        // Don't stack named or tamed entities
        if (entity.customName != null) return
        if (entity is Tameable && entity.isTamed) return

        val radius = configManager.mobStackingRadius
        val maxStack = configManager.mobStackingMaxStackSize
        val loc = event.location

        // Check for nearby entities to stack into
        val world = loc.world ?: return

        val nearbyEntities = world.getNearbyEntities(loc, radius.toDouble(), radius.toDouble(), radius.toDouble())

        for (nearby in nearbyEntities) {
            if (nearby.type == entity.type && nearby.isValid && !nearby.isDead && nearby is LivingEntity) {
                // Don't stack into named/tamed entities either
                if (nearby.customName != null && getStackSize(nearby) <= 1) continue
                if (nearby is Tameable && nearby.isTamed) continue
                
                val currentStack = getStackSize(nearby)

                if (currentStack < maxStack) {
                    event.isCancelled = true
                    setStackSize(nearby, currentStack + 1)
                    updateName(nearby)
                    return
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!configManager.isMobStackingEnabled) return

        val entity = event.entity
        val stackSize = getStackSize(entity)

        if (stackSize > 1) {
            // Spawn a new entity with stackSize - 1
            val loc = entity.location
            val world = loc.world ?: return
            val newEntity = world.spawnEntity(loc, entity.type)
            
            if (newEntity is LivingEntity) {
                setStackSize(newEntity, stackSize - 1)
                updateName(newEntity)
                
                // Transfer metadata
                if (entity is Ageable && newEntity is Ageable) {
                    newEntity.age = entity.age
                }
                if (entity is Sheep && newEntity is Sheep) {
                    newEntity.color = entity.color
                }
                if (entity is Slime && newEntity is Slime) {
                    newEntity.size = entity.size
                }
            }
        }
    }

    private fun getStackSize(entity: LivingEntity): Int {
        val data = entity.persistentDataContainer
        return data.get(stackKey, PersistentDataType.INTEGER) ?: 1
    }

    private fun setStackSize(entity: LivingEntity, size: Int) {
        val data = entity.persistentDataContainer
        data.set(stackKey, PersistentDataType.INTEGER, size)
    }

    private fun updateName(entity: LivingEntity) {
        val size = getStackSize(entity)
        if (size > 1) {
            val format = configManager.mobStackingNameFormat
            var typeName = entity.type.name
            // Capitalize simple
            typeName = typeName.lowercase().replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            
            val name = format.replace("%type%", typeName)
                             .replace("%count%", size.toString())
            entity.customName = ChatColor.translateAlternateColorCodes('&', name)
            entity.isCustomNameVisible = true
        } else {
             entity.customName = null
             entity.isCustomNameVisible = false
        }
    }
}
