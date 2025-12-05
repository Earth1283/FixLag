package io.github.Earth1283.fixLag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

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
            fixlag.setTabCompleter(this);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("fixlag")) {
            if (args.length == 1) {
                List<String> subcommands = new ArrayList<>(Arrays.asList("retrieve", "reload"));
                subcommands.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));
                return subcommands;
            }
        }
        return null;
    }

    private boolean handleFixLagCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("retrieve")) {
            if (!sender.hasPermission("fixlag.retrieve")) {
                sender.sendMessage(messageManager.getMessage("permission_denied"));
                return true;
            }
            if (sender instanceof Player) {
                Player player = (Player) sender;
                deletedItemsManager.openChestGUI(player);
            } else {
                sender.sendMessage("This command can only be used by players.");
            }
            return true;
        }

        if (!sender.hasPermission("fixlag.command")) {
            sender.sendMessage(messageManager.getMessage("permission_denied"));
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
