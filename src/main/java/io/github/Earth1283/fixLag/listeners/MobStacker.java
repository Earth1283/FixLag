package io.github.Earth1283.fixLag.listeners;

import io.github.Earth1283.fixLag.managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public class MobStacker implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final NamespacedKey stackKey;

    public MobStacker(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.stackKey = new NamespacedKey(plugin, "stack_size");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!configManager.isMobStackingEnabled()) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        LivingEntity entity = event.getEntity();
        if (!configManager.getMobStackingAllowedEntities().contains(entity.getType().name())) return;
        
        // Don't stack named or tamed entities
        if (entity.getCustomName() != null) return;
        if (entity instanceof org.bukkit.entity.Tameable && ((org.bukkit.entity.Tameable) entity).isTamed()) return;

        double radius = configManager.getMobStackingRadius();
        int maxStack = configManager.getMobStackingMaxStackSize();
        Location loc = event.getLocation();

        // Check for nearby entities to stack into
        if (loc.getWorld() == null) return;

        Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, radius, radius, radius);

        for (Entity nearby : nearbyEntities) {
            if (nearby.getType() == entity.getType() && nearby.isValid() && !nearby.isDead() && nearby instanceof LivingEntity) {
                LivingEntity nearbyLiving = (LivingEntity) nearby;
                
                // Don't stack into named/tamed entities either
                if (nearbyLiving.getCustomName() != null && getStackSize(nearbyLiving) <= 1) continue;
                if (nearbyLiving instanceof org.bukkit.entity.Tameable && ((org.bukkit.entity.Tameable) nearbyLiving).isTamed()) continue;
                
                int currentStack = getStackSize(nearbyLiving);

                if (currentStack < maxStack) {
                    event.setCancelled(true);
                    setStackSize(nearbyLiving, currentStack + 1);
                    updateName(nearbyLiving);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!configManager.isMobStackingEnabled()) return;

        LivingEntity entity = event.getEntity();
        int stackSize = getStackSize(entity);

        if (stackSize > 1) {
            // Spawn a new entity with stackSize - 1
            Location loc = entity.getLocation();
            Entity newEntity = loc.getWorld().spawnEntity(loc, entity.getType());
            
            if (newEntity instanceof LivingEntity) {
                LivingEntity newLiving = (LivingEntity) newEntity;
                setStackSize(newLiving, stackSize - 1);
                updateName(newLiving);
                
                // Transfer metadata
                if (entity instanceof org.bukkit.entity.Ageable && newLiving instanceof org.bukkit.entity.Ageable) {
                    ((org.bukkit.entity.Ageable) newLiving).setAge(((org.bukkit.entity.Ageable) entity).getAge());
                }
                if (entity instanceof org.bukkit.entity.Sheep && newLiving instanceof org.bukkit.entity.Sheep) {
                    ((org.bukkit.entity.Sheep) newLiving).setColor(((org.bukkit.entity.Sheep) entity).getColor());
                }
                if (entity instanceof org.bukkit.entity.Slime && newLiving instanceof org.bukkit.entity.Slime) {
                    ((org.bukkit.entity.Slime) newLiving).setSize(((org.bukkit.entity.Slime) entity).getSize());
                }
            }
        }
    }

    private int getStackSize(LivingEntity entity) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        if (data.has(stackKey, PersistentDataType.INTEGER)) {
            Integer val = data.get(stackKey, PersistentDataType.INTEGER);
            return val != null ? val : 1;
        }
        return 1;
    }

    private void setStackSize(LivingEntity entity, int size) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        data.set(stackKey, PersistentDataType.INTEGER, size);
    }

    private void updateName(LivingEntity entity) {
        int size = getStackSize(entity);
        if (size > 1) {
            String format = configManager.getMobStackingNameFormat();
            String typeName = entity.getType().name();
            // Capitalize simple
            typeName = typeName.charAt(0) + typeName.substring(1).toLowerCase().replace("_", " ");
            
            String name = format.replace("%type%", typeName)
                                .replace("%count%", String.valueOf(size));
            entity.setCustomName(ChatColor.translateAlternateColorCodes('&', name));
            entity.setCustomNameVisible(true);
        } else {
             entity.setCustomName(null);
             entity.setCustomNameVisible(false);
        }
    }
}
