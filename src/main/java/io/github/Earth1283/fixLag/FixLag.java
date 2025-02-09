package io.github.Earth1283.fixLag;

import io.github.Earth1283.fixLag.tasks.EntityCleanupTask;
import io.github.Earth1283.fixLag.commands.FixLagCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class FixLag extends JavaPlugin {

    private EntityCleanupTask cleanupTask;

    @Override
    public void onEnable() {
        // Load the configuration (creates config.yml if missing)
        saveDefaultConfig();

        // Initialize cleanup task with config values
        cleanupTask = new EntityCleanupTask(this);

        // Schedule the cleanup task at the configured interval
        int cleanupInterval = getConfig().getInt("cleanup-interval", 300);
        scheduleEntityCleanupTask(cleanupInterval);

        // Register the /fixlag command
        this.getCommand("fixlag").setExecutor(new FixLagCommand(cleanupTask));
    }

    private void scheduleEntityCleanupTask(int interval) {
        getServer().getScheduler().runTaskTimer(this, cleanupTask, 0L, interval * 20L); // 20 ticks = 1 second
    }

    public EntityCleanupTask getCleanupTask() {
        return cleanupTask;
    }
}
