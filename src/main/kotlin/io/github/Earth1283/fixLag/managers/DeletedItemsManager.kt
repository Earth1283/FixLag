package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class DeletedItemsManager(private val plugin: JavaPlugin) : Listener {

    private val configManager: ConfigManager = (plugin as FixLag).configManager
    private val messageManager: MessageManager = (plugin as FixLag).messageManager

    private data class StoredItem(val item: ItemStack, val addedAt: Long)

    private val deletedItems = ConcurrentLinkedQueue<StoredItem>()
    private val playerPages = mutableMapOf<UUID, Int>()
    private var isCleanupScheduled = false

    val deletedItemCount: Int get() = deletedItems.size

    companion object {
        private const val CHEST_SIZE = 54
        private const val CHEST_TITLE = "Recovered Items"
        private const val ITEMS_PER_PAGE = 45
        private const val PERSISTENCE_FILE = "deleted_items.yml"
    }

    fun addDeletedItems(items: List<ItemStack>) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        val maxItems = if (configManager.isDeletedItemsPersistenceEnabled) configManager.deletedItemsPersistenceMaxItems else Int.MAX_VALUE
        val available = (maxItems - deletedItems.size).coerceAtLeast(0)
        items.take(available).forEach { deletedItems.offer(StoredItem(it, now)) }

        if (!isCleanupScheduled) {
            isCleanupScheduled = true
            startCleanupTimer()
        }
        if (configManager.isDeletedItemsPersistenceEnabled) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { saveItemsToDisk() })
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
        val endIndex = (startIndex + ITEMS_PER_PAGE).coerceAtMost(allItems.size)

        if (startIndex >= allItems.size && page > 0) {
            openPage(player, page - 1)
            return
        }

        for (i in startIndex until endIndex) {
            inv.addItem(allItems[i].item)
        }

        if (page > 0) {
            val prev = ItemStack(Material.ARROW)
            prev.itemMeta = prev.itemMeta?.apply { displayName(Component.text("Previous Page")) }
            inv.setItem(45, prev)
        }

        if (endIndex < allItems.size) {
            val next = ItemStack(Material.ARROW)
            next.itemMeta = next.itemMeta?.apply { displayName(Component.text("Next Page")) }
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
                val allItems = ArrayList(deletedItems)
                val itemIndex = page * ITEMS_PER_PAGE + event.slot
                if (itemIndex >= allItems.size) return

                val storedItem = allItems[itemIndex]
                player.inventory.addItem(storedItem.item.clone())
                deletedItems.remove(storedItem)
                if (configManager.isDeletedItemsPersistenceEnabled) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { saveItemsToDisk() })
                }
                updateAllOpenViews()
                player.sendMessage(messageManager.getMessage("retrieve_item_success", "%item%", storedItem.item.type.toString()))
            }
        }
    }

    fun saveItemsToDisk() {
        if (!configManager.isDeletedItemsPersistenceEnabled) return
        try {
            val file = File(plugin.dataFolder, PERSISTENCE_FILE)
            val config = YamlConfiguration()
            val snapshot = ArrayList(deletedItems)
            config.set("count", snapshot.size)
            for ((i, stored) in snapshot.withIndex()) {
                config.set("$i.added-at", stored.addedAt)
                config.set("$i.item", stored.item)
            }
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.warning("Could not save deleted items: ${e.message}")
        }
    }

    fun loadItemsFromDisk() {
        if (!configManager.isDeletedItemsPersistenceEnabled) return
        try {
            val file = File(plugin.dataFolder, PERSISTENCE_FILE)
            if (!file.exists()) return

            val config = YamlConfiguration.loadConfiguration(file)
            val count = config.getInt("count", 0)
            val now = System.currentTimeMillis()
            val expiryMs = configManager.deletedItemsPersistenceExpirySeconds * 1000L
            val maxItems = configManager.deletedItemsPersistenceMaxItems
            var loaded = 0

            for (i in 0 until count) {
                if (loaded >= maxItems) break
                val addedAt = config.getLong("$i.added-at", 0L)
                if (addedAt <= 0L || (now - addedAt) >= expiryMs) continue
                val item = config.getItemStack("$i.item") ?: continue
                deletedItems.offer(StoredItem(item, addedAt))
                loaded++
            }

            if (loaded > 0) {
                plugin.logger.info("Restored $loaded deleted item(s) from disk.")
                if (!isCleanupScheduled) {
                    isCleanupScheduled = true
                    startCleanupTimer()
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Could not load deleted items: ${e.message}")
        }
    }

    private fun startCleanupTimer() {
        val cleanupTicks = configManager.deletedItemsPersistenceCleanupSeconds * 20L
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { clearDeletedItems() }, cleanupTicks)
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
        if (configManager.isDeletedItemsPersistenceEnabled) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                File(plugin.dataFolder, PERSISTENCE_FILE).delete()
            })
        }
    }
}
