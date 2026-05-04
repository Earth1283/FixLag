package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class DeletedItemsManager(private val plugin: JavaPlugin) : Listener {

    private val messageManager: MessageManager = (plugin as FixLag).messageManager
    private val deletedItems = ConcurrentLinkedQueue<ItemStack>()
    private val playerPages = mutableMapOf<UUID, Int>()
    private var isCleanupScheduled = false

    companion object {
        private const val CHEST_SIZE = 54
        private const val CHEST_TITLE = "Recovered Items"
        private const val ITEMS_PER_PAGE = 45 // Bottom row for navigation
    }

    fun addDeletedItems(items: List<ItemStack>) {
        if (items.isEmpty()) return
        deletedItems.addAll(items)
        if (!isCleanupScheduled) {
            isCleanupScheduled = true
            startCleanupTimer()
        }
        updateAllOpenViews()
    }

    fun openChestGUI(player: Player) {
        if (deletedItems.isEmpty()) {
            player.sendMessage(messageManager.getMessage("retrieve_command_no_items"))
            return
        }
        playerPages[player.uniqueId] = 0
        openPage(player, 0)
    }

    private fun openPage(player: Player, page: Int) {
        val inv = Bukkit.createInventory(null, CHEST_SIZE, Component.text("$CHEST_TITLE (Page ${page + 1})"))

        val allItems = ArrayList(deletedItems)
        val startIndex = page * ITEMS_PER_PAGE
        val endIndex = (startIndex + ITEMS_PER_PAGE).coerceAtMost(allItems.size) // Exclusive

        if (startIndex >= allItems.size && page > 0) {
            openPage(player, page - 1)
            return
        }

        for (i in startIndex until endIndex) {
            inv.addItem(allItems[i])
        }

        // Navigation
        if (page > 0) {
            val prev = ItemStack(Material.ARROW)
            prev.itemMeta = prev.itemMeta?.apply {
                displayName(Component.text("Previous Page"))
            }
            inv.setItem(45, prev)
        }

        if (endIndex < allItems.size) {
            val next = ItemStack(Material.ARROW)
            next.itemMeta = next.itemMeta?.apply {
                displayName(Component.text("Next Page"))
            }
            inv.setItem(53, next)
        }

        player.openInventory(inv)
    }

    private fun updateAllOpenViews() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.openInventory.title().toString().contains(CHEST_TITLE)) {
                val page = playerPages.getOrDefault(player.uniqueId, 0)
                openPage(player, page)
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (!event.view.title().toString().contains(CHEST_TITLE)) return
        event.isCancelled = true

        val clicked = event.currentItem ?: return
        if (clicked.type == Material.AIR) return

        val player = event.whoClicked as Player
        val page = playerPages.getOrDefault(player.uniqueId, 0)

        if (clicked.type == Material.ARROW) {
            val meta = clicked.itemMeta
            if (meta?.displayName() != null) {
                val slot = event.slot
                if (slot == 45) {
                    if (page > 0) {
                        playerPages[player.uniqueId] = page - 1
                        openPage(player, page - 1)
                    }
                } else if (slot == 53) {
                    playerPages[player.uniqueId] = page + 1
                    openPage(player, page + 1)
                }
            }
            return
        }

        if (event.slot < 45) {
            if (event.isLeftClick || event.isRightClick) {
                event.isCancelled = true
                player.inventory.addItem(clicked)
                deletedItems.remove(clicked)
                updateAllOpenViews()
                player.sendMessage(messageManager.getMessage("retrieve_item_success", "%item%", clicked.type.toString()))
            }
        }
    }

    private fun startCleanupTimer() {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { clearDeletedItems() }, 45 * 20L)
    }

    private fun clearDeletedItems() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.openInventory.title().toString().contains(CHEST_TITLE)) {
                player.closeInventory()
            }
        }
        deletedItems.clear()
        playerPages.clear()
        isCleanupScheduled = false
    }
}
