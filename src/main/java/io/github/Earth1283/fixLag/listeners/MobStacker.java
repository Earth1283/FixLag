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

        double radius = configManager.getMobStackingRadius();
        int maxStack = configManager.getMobStackingMaxStackSize();
        Location loc = event.getLocation();

        // Check for nearby entities to stack into
        // Note: We use the world from the location, not the entity, as the entity might not be fully world-linked yet
        if (loc.getWorld() == null) return;

        Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, radius, radius, radius);

        for (Entity nearby : nearbyEntities) {
            if (nearby.getType() == entity.getType() && nearby.isValid() && !nearby.isDead() && nearby instanceof LivingEntity) {
                LivingEntity nearbyLiving = (LivingEntity) nearby;
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
                
                // Optional: Transfer fire ticks or other states if strictly needed,
                // but usually a fresh entity is cleaner for "unstacking".
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
