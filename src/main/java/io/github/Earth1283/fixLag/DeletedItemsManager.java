package io.github.Earth1283.fixLag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DeletedItemsManager {

    private final JavaPlugin plugin;
    private final MessageManager messageManager;
    private final ConcurrentLinkedQueue<ItemStack> deletedItems = new ConcurrentLinkedQueue<>();
    private Inventory sharedChest;
    private boolean isCleanupScheduled = false;
    private static final int CHEST_SIZE = 54; // 6 rows of 9 slots
    private static final String CHEST_TITLE = "Recovered Items";

    public DeletedItemsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messageManager = ((FixLag) plugin).getMessageManager();
    }

    public void addDeletedItems(List<ItemStack> items) {
        if (items.isEmpty()) return;
        deletedItems.addAll(items);
        if (!isCleanupScheduled) {
            isCleanupScheduled = true;
            startCleanupTimer();
        }
    }

    public void openChestGUI(Player player) {
        if (deletedItems.isEmpty()) {
            player.sendMessage(messageManager.getMessage("retrieve_command_no_items"));
            return;
        }

        if (sharedChest == null) {
            sharedChest = Bukkit.createInventory(null, CHEST_SIZE, CHEST_TITLE);
            updateChestContents();
        }
        player.openInventory(sharedChest);
    }

    private void updateChestContents() {
        if (sharedChest == null) return;

        sharedChest.clear();
        for (ItemStack item : deletedItems) {
            if (item != null && item.getType() != Material.AIR) {
                sharedChest.addItem(item);
            }
        }
    }

    private void startCleanupTimer() {
        Bukkit.getScheduler().runTaskLater(plugin, this::clearDeletedItems, 45 * 20L); // 45 seconds
    }

    private void clearDeletedItems() {
        if (sharedChest != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTitle().equals(CHEST_TITLE)) {
                    player.closeInventory();
                }
            }
            sharedChest = null;
        }
        deletedItems.clear();
        isCleanupScheduled = false;
    }
}
