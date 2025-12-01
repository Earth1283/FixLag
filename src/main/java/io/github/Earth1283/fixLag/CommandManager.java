package io.github.Earth1283.fixLag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandManager implements CommandExecutor {

    private final JavaPlugin plugin;
    private final TaskManager taskManager;
    private final PerformanceMonitor performanceMonitor;
    private final MessageManager messageManager;
    private final DeletedItemsManager deletedItemsManager;

    public CommandManager(JavaPlugin plugin, TaskManager taskManager, PerformanceMonitor performanceMonitor, MessageManager messageManager, DeletedItemsManager deletedItemsManager) {
        this.plugin = plugin;
        this.taskManager = taskManager;
        this.performanceMonitor = performanceMonitor;
        this.messageManager = messageManager;
        this.deletedItemsManager = deletedItemsManager;
        registerCommands();
    }

    private void registerCommands() {
        PluginCommand fixlag = plugin.getCommand("fixlag");
        if (fixlag != null) {
            fixlag.setExecutor(this);
        }
        PluginCommand gcinfo = plugin.getCommand("gcinfo");
        if (gcinfo != null) {
            gcinfo.setExecutor(this);
        }
        PluginCommand serverinfo = plugin.getCommand("serverinfo");
        if (serverinfo != null) {
            serverinfo.setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "fixlag":
                return handleFixLagCommand(sender, args);
            case "gcinfo":
                return handleGcİnfoCommand(sender);
            case "serverinfo":
                return handleServerİnfoCommand(sender);
            default:
                return false;
        }
    }

    private boolean handleFixLagCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fixlag.command")) {
            sender.sendMessage(messageManager.getMessage("permission_denied"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("retrieve")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                deletedItemsManager.openChestGUI(player);
            } else {
                sender.sendMessage("This command can only be used by players.");
            }
            return true;
        }

        sender.sendMessage(messageManager.getMessage("entity_clear_manual"));
        taskManager.deleteAndAnnounce();
        return true;
    }

    private boolean handleGcİnfoCommand(CommandSender sender) {
        if (!sender.hasPermission("fixlag.gcinfo")) {
            sender.sendMessage(messageManager.getMessage("permission_denied"));
            return true;
        }
        sender.sendMessage(performanceMonitor.getMemoryAndGCInfo());
        return true;
    }

    private boolean handleServerİnfoCommand(CommandSender sender) {
        if (!sender.hasPermission("fixlag.serverinfo")) {
            sender.sendMessage(messageManager.getMessage("permission_denied"));
            return true;
        }
        sender.sendMessage(performanceMonitor.getServerInfo());
        return true;
    }
}
