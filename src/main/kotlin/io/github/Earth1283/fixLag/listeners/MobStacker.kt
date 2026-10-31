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
        // Other species carry variants/state that this stack format cannot preserve.
        if (entity.type !in supportedTypes || !isStackable(entity)) return
        
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
            if (nearby !== entity && nearby.type == entity.type && nearby.isValid && !nearby.isDead && nearby is LivingEntity) {
                if (!isStackable(nearby) || !compatible(entity, nearby)) continue
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

    private val supportedTypes = setOf(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
        EntityType.SPIDER, EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN)

    private fun isStackable(entity: LivingEntity): Boolean {
        if (entity.isInsideVehicle || entity.passengers.isNotEmpty() || entity.isLeashed || entity.isInvulnerable) return false
        if (!entity.hasAI() || entity is Mob && !entity.isAware || entity.activePotionEffects.isNotEmpty()) return false
        if (entity.scoreboardTags.isNotEmpty() || entity.persistentDataContainer.keys.any { it != stackKey }) return false
        return entity !is Tameable || !entity.isTamed
    }

    internal fun compatible(first: LivingEntity, second: LivingEntity): Boolean {
        if (first.type != second.type || first.health != second.health) return false
        if (first is Ageable && second is Ageable &&
            (first.age != second.age || first.ageLock != second.ageLock || first.canBreed() != second.canBreed())) return false
        if (first is Zombie && second is Zombie && first.isBaby != second.isBaby) return false
        if (first is Chicken && second is Chicken && first.isChickenJockey != second.isChickenJockey) return false
        if (first is Sheep && second is Sheep && (first.color != second.color || first.isSheared != second.isSheared)) return false
        if (first is Pig && second is Pig && (first.hasSaddle() || second.hasSaddle())) return false
        if (first is Creeper && second is Creeper &&
            (first.isPowered != second.isPowered || first.explosionRadius != second.explosionRadius ||
                first.maxFuseTicks != second.maxFuseTicks || first.fuseTicks > 0 || second.fuseTicks > 0)) return false
        if (first.isSilent != second.isSilent || first.isGlowing != second.isGlowing ||
            first.removeWhenFarAway != second.removeWhenFarAway || first.canPickupItems != second.canPickupItems) return false
        val a = first.equipment
        val b = second.equipment
        if ((a == null) != (b == null)) return false
        if (a != null && b != null) {
            if (!a.armorContents.contentEquals(b.armorContents) || a.itemInMainHand != b.itemInMainHand || a.itemInOffHand != b.itemInOffHand) return false
            for (slot in org.bukkit.inventory.EquipmentSlot.entries) {
                if (a.getDropChance(slot) != b.getDropChance(slot)) return false
            }
        }
        return true
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val stackSize = getStackSize(entity)

        if (stackSize > 1) {
            // Spawn a new entity with stackSize - 1
            val loc = entity.location
            val world = loc.world ?: return
            val entityClass = entity.type.entityClass?.asSubclass(LivingEntity::class.java) ?: return
            world.spawn(loc, entityClass, CreatureSpawnEvent.SpawnReason.CUSTOM, false) { newEntity ->
                setStackSize(newEntity, stackSize - 1)
                updateName(newEntity)
                
                // Transfer metadata
                if (entity is Ageable && newEntity is Ageable) {
                    newEntity.age = entity.age
                    newEntity.ageLock = entity.ageLock
                }
                if (entity is Sheep && newEntity is Sheep) {
                    newEntity.color = entity.color
                    newEntity.isSheared = entity.isSheared
                }
                if (entity is Zombie && newEntity is Zombie) newEntity.isBaby = entity.isBaby
                if (entity is Chicken && newEntity is Chicken) {
                    newEntity.setIsChickenJockey(entity.isChickenJockey)
                    newEntity.eggLayTime = entity.eggLayTime
                }
                if (entity is Creeper && newEntity is Creeper) {
                    newEntity.isPowered = entity.isPowered
                    newEntity.explosionRadius = entity.explosionRadius
                    newEntity.maxFuseTicks = entity.maxFuseTicks
                }
                newEntity.isSilent = entity.isSilent
                newEntity.isGlowing = entity.isGlowing
                newEntity.removeWhenFarAway = entity.removeWhenFarAway
                newEntity.canPickupItems = entity.canPickupItems
                val equipment = entity.equipment
                newEntity.equipment?.let { target ->
                    if (equipment != null) {
                        target.armorContents = equipment.armorContents.map { it?.clone() }.toTypedArray()
                        target.setItemInMainHand(equipment.itemInMainHand.clone())
                        target.setItemInOffHand(equipment.itemInOffHand.clone())
                        for (slot in org.bukkit.inventory.EquipmentSlot.entries) target.setDropChance(slot, equipment.getDropChance(slot))
                    }
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
