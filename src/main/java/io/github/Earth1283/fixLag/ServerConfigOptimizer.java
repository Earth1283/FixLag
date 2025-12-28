package io.github.Earth1283.fixLag;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ServerConfigOptimizer {

    private final JavaPlugin plugin;
    private final MessageManager messageManager;
    private final Map<CommandSender, List<ConfigChange>> pendingChanges = new HashMap<>();

    public ServerConfigOptimizer(JavaPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    public void analyze(CommandSender sender) {
        List<ConfigChange> changes = new ArrayList<>();

        // Analyze bukkit.yml
        File bukkitFile = new File("bukkit.yml");
        if (bukkitFile.exists()) {
            YamlConfiguration bukkitConfig = YamlConfiguration.loadConfiguration(bukkitFile);
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.monsters", 70, 50);
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.animals", 10, 8);
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.water-animals", 15, 3);
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.water-ambient", 20, 1);
            checkSetting(changes, bukkitFile, bukkitConfig, "spawn-limits.ambient", 15, 1);
            checkSetting(changes, bukkitFile, bukkitConfig, "chunk-gc.period-in-ticks", 600, 400);
            checkSetting(changes, bukkitFile, bukkitConfig, "ticks-per.monster-spawns", 1, 4);
            checkSetting(changes, bukkitFile, bukkitConfig, "ticks-per.animal-spawns", 400, 400);
        }

        // Analyze spigot.yml
        File spigotFile = new File("spigot.yml");
        if (spigotFile.exists()) {
            YamlConfiguration spigotConfig = YamlConfiguration.loadConfiguration(spigotFile);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.animals", 32, 16);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.monsters", 32, 24);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.raiders", 48, 48);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-activation-range.misc", 16, 8);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.players", 48, 48);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.animals", 48, 48);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.monsters", 48, 48);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.entity-tracking-range.misc", 32, 32);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.merge-radius.item", 2.5, 4.0);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.merge-radius.exp", 3.0, 6.0);
            checkSetting(changes, spigotFile, spigotConfig, "world-settings.default.view-distance", "default", 4);
        }

        // Analyze server.properties
        File serverPropsFile = new File("server.properties");
        if (serverPropsFile.exists()) {
             Properties props = new Properties();
             try (FileInputStream in = new FileInputStream(serverPropsFile)) {
                 props.load(in);
                 checkProperty(changes, serverPropsFile, props, "view-distance", 10, 6);
                 checkProperty(changes, serverPropsFile, props, "network-compression-threshold", 256, 256);
             } catch (IOException e) {
                 e.printStackTrace();
             }
        }

        // Analyze paper.yml (Old Paper)
        File paperFile = new File("paper.yml");
        if (paperFile.exists()) {
            YamlConfiguration paperConfig = YamlConfiguration.loadConfiguration(paperFile);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.max-auto-save-chunks-per-tick", 24, 8);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.optimize-explosions", false, true);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.mob-spawner-tick-rate", 1, 2);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.game-mechanics.disable-chest-cat-detection", false, true);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.hopper.disable-move-event", false, true);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.non-player-arrow-despawn-rate", -1, 60); // 3 seconds
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.creative-arrow-despawn-rate", -1, 60);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.prevent-moving-into-unloaded-chunks", false, true);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.use-faster-eigencraft-redstone", false, true);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.armor-stands-do-collision-entity-lookups", true, false);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.per-player-mob-spawns", false, true);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.alt-item-despawn-rate.enabled", false, true);
            checkSetting(changes, paperFile, paperConfig, "world-settings.default.alt-item-despawn-rate.items.cobblestone", 300, 100);
        }

        // Analyze config/paper-global.yml (New Paper Global)
        File paperGlobalFile = new File("config/paper-global.yml");
        if (paperGlobalFile.exists()) {
            YamlConfiguration paperGlobalConfig = YamlConfiguration.loadConfiguration(paperGlobalFile);
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "chunk-loading.min-load-radius", 2, 2);
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "misc.client-interaction-updates-per-tick-in-single-player", -1, 1);
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "spam-limiter.tab-spam-increment", 1, 10);
            checkSetting(changes, paperGlobalFile, paperGlobalConfig, "spam-limiter.tab-spam-limit", 500, 20);
        }

        // Analyze config/paper-world-defaults.yml (New Paper World Defaults)
        File paperWorldFile = new File("config/paper-world-defaults.yml");
        if (paperWorldFile.exists()) {
            YamlConfiguration paperWorldConfig = YamlConfiguration.loadConfiguration(paperWorldFile);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "environment.optimize-explosions", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "environment.disable-teleportation-suffocation-check", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "chunks.auto-save-interval", -1, 6000);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "chunks.max-auto-save-chunks-per-tick", 24, 8);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.spawning.per-player-mob-spawns", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.behavior.disable-chest-cat-detection", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.behavior.spawner-nerfed-mobs-should-jump", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "entities.entities-target-with-follow-range", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "collisions.only-players-collide", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "tick-rates.mob-spawner", 1, 2);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "tick-rates.grass-spread", 1, 4);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "tick-rates.container-update", 1, 2);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "hopper.disable-move-event", false, true);
            checkSetting(changes, paperWorldFile, paperWorldConfig, "hopper.ignore-occluding-blocks", false, true);
        }

        if (changes.isEmpty()) {
            sender.sendMessage(messageManager.getComponentMessage("optimize_config_no_changes"));
            return;
        }

        pendingChanges.put(sender, changes);
        
        sender.sendMessage(messageManager.getComponentMessage("optimize_config_header"));
        for (ConfigChange change : changes) {
            sender.sendMessage(messageManager.getComponentMessage("optimize_config_entry", 
                "%file%", change.file.getName(),
                "%key%", change.key,
                "%old%", String.valueOf(change.currentValue),
                "%new%", String.valueOf(change.newValue)
            ));
        }
        sender.sendMessage(messageManager.getComponentMessage("optimize_config_footer"));
    }

    private void checkSetting(List<ConfigChange> changes, File file, YamlConfiguration config, String key, Object threshold, Object optimized) {
        if (!config.contains(key)) return;
        
        Object current = config.get(key);
        
        if (current instanceof Boolean && optimized instanceof Boolean) {
            boolean currVal = (Boolean) current;
            boolean optVal = (Boolean) optimized;
            if (currVal != optVal) {
                 changes.add(new ConfigChange(file, key, current, optimized));
            }
        } else if (current instanceof Number && optimized instanceof Number) {
             double currVal = ((Number) current).doubleValue();
             double optVal = ((Number) optimized).doubleValue();
             
             boolean lowerIsBetter = !key.contains("merge-radius") && !key.contains("ticks-per") && !key.contains("tab-spam-limit");
             
             if (lowerIsBetter) {
                 if (currVal > optVal) {
                     changes.add(new ConfigChange(file, key, current, optimized));
                 }
             } else {
                 if (currVal < optVal) {
                     changes.add(new ConfigChange(file, key, current, optimized));
                 }
             }
        }
    }
    
    private void checkProperty(List<ConfigChange> changes, File file, Properties props, String key, Object threshold, Object optimized) {
        if (!props.containsKey(key)) return;
        
        String currentStr = props.getProperty(key);
        try {
            double currVal = Double.parseDouble(currentStr);
            double optVal = Double.parseDouble(String.valueOf(optimized));
            
             // For server.properties, usually lower is better (view-distance)
             boolean lowerIsBetter = !key.equals("network-compression-threshold"); 
             
             if (lowerIsBetter) {
                 if (currVal > optVal) {
                     changes.add(new ConfigChange(file, key, currentStr, optimized));
                 }
             } else {
                 // network-compression-threshold logic is complex, sticking to simple check if vastly different?
                 // Actually, if it's default -1 or too high/low, we might want to fix.
                 // But for now, let's just stick to > threshold check.
             }
        } catch (NumberFormatException ignored) {}
    }

    public void applyChanges(CommandSender sender, boolean overwrite) {
        List<ConfigChange> changes = pendingChanges.get(sender);
        if (changes == null || changes.isEmpty()) {
             sender.sendMessage(messageManager.getMessage("optimize_config_no_pending"));
             return;
        }
        
        // Group by file
        Map<File, List<ConfigChange>> byFile = changes.stream().collect(Collectors.groupingBy(c -> c.file));
        
        for (Map.Entry<File, List<ConfigChange>> entry : byFile.entrySet()) {
            File file = entry.getKey();
            List<ConfigChange> fileChanges = entry.getValue();
            
            try {
                if (file.getName().endsWith(".yml")) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    for (ConfigChange change : fileChanges) {
                        config.set(change.key, change.newValue);
                    }
                    File saveFile = overwrite ? file : new File(file.getParent(), file.getName() + ".changed");
                    config.save(saveFile);
                } else if (file.getName().equals("server.properties")) {
                    Properties props = new Properties();
                    try (FileInputStream in = new FileInputStream(file)) {
                        props.load(in);
                    }
                    for (ConfigChange change : fileChanges) {
                        props.setProperty(change.key, String.valueOf(change.newValue));
                    }
                    File saveFile = overwrite ? file : new File(file.getParent(), file.getName() + ".changed");
                    try (FileOutputStream out = new FileOutputStream(saveFile)) {
                        props.store(out, "Optimized by FixLag");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                sender.sendMessage(messageManager.getMessage("optimize_config_error", "%error%", e.getMessage()));
            }
        }
        
        sender.sendMessage(messageManager.getMessage("optimize_config_applied"));
        pendingChanges.remove(sender);
    }
    
    public void rejectChanges(CommandSender sender) {
        if (pendingChanges.remove(sender) != null) {
            sender.sendMessage(messageManager.getMessage("optimize_config_rejected"));
        } else {
            sender.sendMessage(messageManager.getMessage("optimize_config_no_pending"));
        }
    }

    private static class ConfigChange {
        File file;
        String key;
        Object currentValue;
        Object newValue;

        public ConfigChange(File file, String key, Object currentValue, Object newValue) {
            this.file = file;
            this.key = key;
            this.currentValue = currentValue;
            this.newValue = newValue;
        }
    }
}
