package io.github.Earth1283.fixLag.managers;

import io.github.Earth1283.fixLag.FixLag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DeletedItemsManager implements Listener {

    private final JavaPlugin plugin;
    private final MessageManager messageManager;
    private final ConcurrentLinkedQueue<ItemStack> deletedItems = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private boolean isCleanupScheduled = false;
    private static final int CHEST_SIZE = 54;
    private static final String CHEST_TITLE = "Recovered Items";
    private static final int ITEMS_PER_PAGE = 45; // Bottom row for navigation

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
        updateAllOpenViews();
    }

    public void openChestGUI(Player player) {
        if (deletedItems.isEmpty()) {
            player.sendMessage(messageManager.getMessage("retrieve_command_no_items"));
            return;
        }
        playerPages.put(player.getUniqueId(), 0);
        openPage(player, 0);
    }
    
    private void openPage(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, CHEST_SIZE, Component.text(CHEST_TITLE + " (Page " + (page + 1) + ")"));
        
        List<ItemStack> allItems = new ArrayList<>(deletedItems);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size()); // Exclusive

        if (startIndex >= allItems.size() && page > 0) {
            // Page is empty, go back (could check before calling openPage)
            openPage(player, page - 1);
            return;
        }

        for (int i = startIndex; i < endIndex; i++) {
            inv.addItem(allItems.get(i));
        }

        // Navigation
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.displayName(Component.text("Previous Page"));
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        }
        
        if (endIndex < allItems.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.displayName(Component.text("Next Page"));
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    private void updateAllOpenViews() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().title().toString().contains(CHEST_TITLE)) { // Simplified check
                // Refresh current page
                int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                openPage(player, page); // This re-opens, which updates the view
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().toString().contains(CHEST_TITLE)) return;
        event.setCancelled(true); // Prevent taking items? Or allow? Usually retrieve means take back.
        // Assuming we allow taking back items from the slot, but preventing moving nav buttons.
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (clicked.getType() == Material.ARROW) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.displayName() != null) {
                String name = meta.displayName().toString(); // This is tricky with Components.
                // Assuming simple check or checking slot
                int slot = event.getSlot();
                if (slot == 45) { // Prev
                    if (page > 0) {
                         playerPages.put(player.getUniqueId(), page - 1);
                         openPage(player, page - 1);
                    }
                } else if (slot == 53) { // Next
                    playerPages.put(player.getUniqueId(), page + 1);
                    openPage(player, page + 1);
                }
            }
            return;
        }
        
        // Allow taking items logic:
        // By default cancellation prevents it. We should allow if it's a content slot.
        if (event.getSlot() < 45) {
             event.setCancelled(false);
             // Verify if item is removed from deletedItems list?
             // If player takes it, it should be removed from the shared list so others don't duplicate it.
             // This complicates things.
             // Current deletedItems is a Queue.
             // If we want real "retrieve", we must remove it from the list.
             // But managing concurrent access implies we need to be careful.
             
             // For now, let's keep it simple: "The user's main objective is to retrieve items".
             // If we allow taking, we must remove from queue.
             // Given the complexity of synchronizing the queue with the UI in a multi-page setup,
             // Implementing "Click to retrieve to inventory" might be safer than drag-and-drop.
             
             if (event.isLeftClick() || event.isRightClick()) {
                 event.setCancelled(true);
                 player.getInventory().addItem(clicked);
                 deletedItems.remove(clicked); // Remove object reference. Note: ItemStack equality might change if amount changes.
                 // This is tricky.
                 // Let's assume for now the user just wants to see them or simple take.
                 // If I setCancelled(false), they can take it. But the list isn't updated.
                 // So next time they open, it's there again. Duplication exploit.
                 // So I MUST remove it.
                 
                 deletedItems.remove(clicked);
                 updateAllOpenViews(); // Refresh for everyone
                 player.sendMessage(messageManager.getMessage("retrieve_item_success", "%item%", clicked.getType().toString()));
             }
        }
    }

    private void startCleanupTimer() {
        Bukkit.getScheduler().runTaskLater(plugin, this::clearDeletedItems, 45 * 20L); // 45 seconds
    }

    private void clearDeletedItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().title().toString().contains(CHEST_TITLE)) {
                player.closeInventory();
            }
        }
        deletedItems.clear();
        playerPages.clear();
        isCleanupScheduled = false;
    }
}
