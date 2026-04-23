package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;

public class VillagerLobotomizer extends BukkitRunnable {

    private final FixLag plugin;
    private final ConfigManager config;
    private final MessageManager messageManager;

    public VillagerLobotomizer(FixLag plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public void run() {
        if (!config.isVillagerLobotomizationEnabled()) return;

        int lobotomizedCount = 0;
        int restoredCount = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                boolean trapped = isTrapped(villager);
                
                if (trapped && villager.isAware()) {
                    villager.setAware(false);
                    lobotomizedCount++;
                } else if (!trapped && !villager.isAware()) {
                    villager.setAware(true);
                    restoredCount++;
                }
            }
        }

        if (lobotomizedCount > 0) {
            messageManager.logInfo("log_villager_lobotomized", "<count>", String.valueOf(lobotomizedCount));
        }
        if (restoredCount > 0) {
            messageManager.logInfo("log_villager_restored", "<count>", String.valueOf(restoredCount));
        }
    }

    private boolean isTrapped(Villager villager) {
        // Check if trapped in a minecart
        if (villager.getVehicle() instanceof Minecart) {
            return true;
        }

        // Check if trapped in a 1x1 space (solid blocks on all 4 sides at foot level)
        Block footBlock = villager.getLocation().getBlock();
        int solidCount = 0;
        
        BlockFace[] sides = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        for (BlockFace face : sides) {
            if (footBlock.getRelative(face).getType().isSolid()) {
                solidCount++;
            }
        }

        return solidCount >= 4;
    }
}
