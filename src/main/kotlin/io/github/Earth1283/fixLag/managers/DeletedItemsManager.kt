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
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.Base64
import java.util.UUID

class DeletedItemsManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager = (plugin as FixLag).configManager,
    private val messageManager: MessageManager = (plugin as FixLag).messageManager
) : Listener {
    private class StoredItem(val item: ItemStack, val addedAt: Long, val expiresAt: Long) {
        var encoded: String? = null
    }

    private class RecoveryHolder(val owner: DeletedItemsManager, val page: Int, val ids: List<UUID>) : InventoryHolder {
        lateinit var contents: Inventory
        override fun getInventory(): Inventory = contents
    }

    private val deletedItems = linkedMapOf<UUID, StoredItem>()
    private val viewers = mutableSetOf<UUID>()
    private var cleanupTask: BukkitTask? = null
    private var refreshTask: BukkitTask? = null
    private val storage = RecoveryStorage(File(plugin.dataFolder, "deleted_items.yml"), plugin.logger)
    val deletedItemCount: Int get() = deletedItems.size

    fun addDeletedItems(items: List<ItemStack>) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        for (item in items) {
            // Bound memory as well as the persistence file; discard the oldest entries first.
            while (deletedItems.size >= configManager.deletedItemsPersistenceMaxItems) {
                deletedItems.remove(deletedItems.keys.first())
            }
            deletedItems[UUID.randomUUID()] = StoredItem(item.clone(), now,
                now + configManager.deletedItemsPersistenceCleanupSeconds * 1000L)
        }
        scheduleCleanup()
        saveItemsToDisk()
        refreshViews()
    }

    fun openChestGUI(player: Player) {
        if (!player.hasPermission("fixlag.retrieve")) {
            player.sendMessage(messageManager.getMessage("permission_denied"))
            return
        }
        if (deletedItems.isEmpty()) {
            player.sendMessage(messageManager.getMessage("retrieve_command_no_items"))
            return
        }
        openPage(player, 0)
    }

    private fun openPage(player: Player, requestedPage: Int) {
        val entries = deletedItems.entries.toList()
        val page = requestedPage.coerceIn(0, ((entries.size - 1) / 45).coerceAtLeast(0))
        val displayed = entries.drop(page * 45).take(45)
        val holder = RecoveryHolder(this, page, displayed.map { it.key })
        val inventory = Bukkit.createInventory(holder, 54, Component.text("Recovered Items (Page ${page + 1})"))
        holder.contents = inventory
        displayed.forEachIndexed { slot, entry -> inventory.setItem(slot, entry.value.item.clone()) }
        if (page > 0) inventory.setItem(45, navigation("Previous Page"))
        if ((page + 1) * 45 < entries.size) inventory.setItem(53, navigation("Next Page"))
        player.openInventory(inventory)
        viewers.add(player.uniqueId)
    }

    private fun navigation(name: String) = ItemStack(Material.ARROW).apply {
        itemMeta = itemMeta?.apply { displayName(Component.text(name)) }
    }

    private fun holder(inventory: Inventory): RecoveryHolder? =
        (inventory.holder as? RecoveryHolder)?.takeIf { it.owner === this }

    // InventoryClickEvent forbids opening/replacing its inventory during the event itself.
    private fun refreshViews() {
        if (refreshTask != null || viewers.isEmpty()) return
        refreshTask = Bukkit.getScheduler().runTask(plugin, Runnable {
            refreshTask = null
            for (id in viewers.toList()) {
                val player = Bukkit.getPlayer(id)
                val current = player?.let { holder(it.openInventory.topInventory) }
                if (player == null || current == null) {
                    viewers.remove(id)
                } else if (deletedItems.isEmpty() || !player.hasPermission("fixlag.retrieve")) {
                    player.closeInventory()
                } else {
                    openPage(player, current.page)
                }
            }
        })
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = holder(event.view.topInventory) ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (!player.hasPermission("fixlag.retrieve")) return
        val slot = event.rawSlot
        if (slot !in 0 until 54 || event.click !in setOf(ClickType.LEFT, ClickType.RIGHT)) return
        if (slot == 45 || slot == 53) {
            if (event.currentItem == null) return
            val nextPage = holder.page + if (slot == 45) -1 else 1
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (this.holder(player.openInventory.topInventory) === holder && player.hasPermission("fixlag.retrieve")) {
                    openPage(player, nextPage)
                }
            })
            return
        }
        val id = holder.ids.getOrNull(slot) ?: return
        val stored = deletedItems[id] ?: return
        val originalAmount = stored.item.amount
        val remaining = player.inventory.addItem(stored.item.clone()).values.sumOf { it.amount }
        if (remaining == originalAmount) {
            player.sendMessage(messageManager.getMessage("retrieve_inventory_full"))
            return
        }
        if (remaining == 0) deletedItems.remove(id) else {
            stored.item.amount = remaining
            stored.encoded = null
        }
        saveItemsToDisk()
        refreshViews()
        player.sendMessage(messageManager.getMessage("retrieve_item_success", "%item%", stored.item.type.toString()))
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (holder(event.view.topInventory) != null && event.rawSlots.any { it < 54 }) event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (holder(event.inventory) != null) viewers.remove(event.player.uniqueId)
    }

    fun saveItemsToDisk() {
        if (!configManager.isDeletedItemsPersistenceEnabled) return
        // Serialize each changed item once, on the server thread. The worker gets immutable data.
        val snapshot = deletedItems.values.map { stored ->
            val encoded = stored.encoded ?: Base64.getEncoder().encodeToString(stored.item.serializeAsBytes())
                .also { stored.encoded = it }
            RecoveryStorage.Record(stored.addedAt, encoded)
        }
        storage.save(snapshot)
    }

    fun onConfigReload() {
        while (deletedItems.size > configManager.deletedItemsPersistenceMaxItems) {
            deletedItems.remove(deletedItems.keys.first())
        }
        if (configManager.isDeletedItemsPersistenceEnabled) saveItemsToDisk()
        else storage.save(emptyList())
        scheduleCleanup()
        refreshViews()
    }

    fun loadItemsFromDisk() {
        if (!configManager.isDeletedItemsPersistenceEnabled) return
        val file = File(plugin.dataFolder, "deleted_items.yml")
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        val now = System.currentTimeMillis()
        val count = yaml.getInt("count").coerceAtLeast(0)
        val first = (count - configManager.deletedItemsPersistenceMaxItems).coerceAtLeast(0)
        for (i in first until count) {
            val addedAt = yaml.getLong("$i.added-at")
            if (addedAt <= 0 || now - addedAt >= configManager.deletedItemsPersistenceExpirySeconds * 1000L) continue
            try {
                val encoded = yaml.getString("$i.item-bytes")
                val item = if (encoded != null) ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded))
                    else yaml.getItemStack("$i.item") ?: continue // Read files from older releases.
                deletedItems[UUID.randomUUID()] = StoredItem(item, addedAt,
                    now + configManager.deletedItemsPersistenceCleanupSeconds * 1000L).apply { this.encoded = encoded }
            } catch (e: Exception) {
                plugin.logger.warning("Could not restore deleted item $i: ${e.message}")
            }
        }
        scheduleCleanup()
    }

    private fun scheduleCleanup() {
        cleanupTask?.cancel()
        val earliest = deletedItems.values.minOfOrNull { it.expiresAt } ?: return
        val ticks = ((earliest - System.currentTimeMillis() + 49) / 50).coerceAtLeast(1L)
        cleanupTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            cleanupTask = null
            val now = System.currentTimeMillis()
            deletedItems.entries.removeIf { it.value.expiresAt <= now }
            saveItemsToDisk()
            refreshViews()
            scheduleCleanup()
        }, ticks)
    }

    fun shutdown() {
        cleanupTask?.cancel()
        refreshTask?.cancel()
        viewers.toList().forEach { Bukkit.getPlayer(it)?.closeInventory() }
        saveItemsToDisk()
        storage.close()
    }
}
