package io.github.Earth1283.fixLag.managers

import be.seeseemelk.mockbukkit.entity.PlayerMock
import io.github.Earth1283.fixLag.TestFixture
import org.bukkit.Material
import org.bukkit.event.inventory.*
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import kotlin.test.*

class DeletedItemsManagerTest : TestFixture() {
    private lateinit var manager: DeletedItemsManager
    private lateinit var player: PlayerMock

    @BeforeEach
    fun setupRecovery() {
        manager = DeletedItemsManager(plugin, config, messages)
        player = server.addPlayer().apply { isOp = true }
    }

    @AfterEach
    fun closeRecovery() { manager.shutdown() }

    private fun click(rawSlot: Int, who: PlayerMock = player, kind: ClickType = ClickType.LEFT): InventoryClickEvent {
        val event = mock(InventoryClickEvent::class.java)
        val view = who.openInventory
        val item = if (rawSlot in 0 until 54) view.topInventory.getItem(rawSlot) else null
        `when`(event.view).thenReturn(view)
        `when`(event.whoClicked).thenReturn(who)
        `when`(event.rawSlot).thenReturn(rawSlot)
        `when`(event.click).thenReturn(kind)
        `when`(event.currentItem).thenReturn(item)
        manager.onInventoryClick(event)
        return event
    }

    @Test
    fun `a chest named like the recovery GUI grants no access`() {
        manager.addDeletedItems(listOf(ItemStack(Material.DIAMOND)))
        player.isOp = false
        val chest = server.createInventory(null, 54, "Recovered Items")
        chest.setItem(0, ItemStack(Material.DIRT))
        player.openInventory(chest)
        val event = click(0)
        verify(event, never()).setCancelled(true)
        assertEquals(1, manager.deletedItemCount)
        assertFalse(player.inventory.contains(Material.DIAMOND))
    }

    @Test
    fun `full inventories leave the item available`() {
        for (i in 0 until player.inventory.size) player.inventory.setItem(i, ItemStack(Material.STONE, 64))
        manager.addDeletedItems(listOf(ItemStack(Material.DIAMOND)))
        manager.openChestGUI(player)
        click(0)
        assertEquals(1, manager.deletedItemCount)
        assertFalse(player.inventory.contains(Material.DIAMOND))
    }

    @Test
    fun `partial insertion keeps the unaccepted amount`() {
        for (i in 0 until player.inventory.size) player.inventory.setItem(i, ItemStack(Material.STONE, 64))
        player.inventory.setItem(0, ItemStack(Material.DIAMOND, 63))
        manager.addDeletedItems(listOf(ItemStack(Material.DIAMOND, 64)))
        manager.openChestGUI(player)
        click(0)
        server.scheduler.performOneTick()
        assertEquals(64, player.inventory.getItem(0)!!.amount)
        assertEquals(1, manager.deletedItemCount)
        assertEquals(63, player.openInventory.topInventory.getItem(0)!!.amount)
    }

    @Test
    fun `similar stacks do not change the displayed slot mapping`() {
        manager.addDeletedItems(listOf(ItemStack(Material.STONE, 32), ItemStack(Material.STONE, 32), ItemStack(Material.DIAMOND)))
        manager.openChestGUI(player)
        assertEquals(Material.DIAMOND, player.openInventory.topInventory.getItem(2)!!.type)
        click(2)
        assertTrue(player.inventory.contains(Material.DIAMOND))
        assertFalse(player.inventory.contains(Material.STONE))
        assertEquals(2, manager.deletedItemCount)
    }

    @Test
    fun `bottom inventory and shift clicks cannot claim entries`() {
        manager.addDeletedItems(listOf(ItemStack(Material.DIAMOND)))
        manager.openChestGUI(player)
        click(81)
        click(0, kind = ClickType.SHIFT_LEFT)
        assertEquals(1, manager.deletedItemCount)
        assertFalse(player.inventory.contains(Material.DIAMOND))
    }

    @Test
    fun `arrows are recoverable and stale viewers cannot duplicate claims`() {
        val other = server.addPlayer().apply { isOp = true }
        manager.addDeletedItems(listOf(ItemStack(Material.ARROW, 12)))
        manager.openChestGUI(player)
        manager.openChestGUI(other)
        click(0)
        click(0, other)
        assertEquals(0, manager.deletedItemCount)
        assertTrue(player.inventory.contains(Material.ARROW, 12))
        assertFalse(other.inventory.contains(Material.ARROW))
    }

    @Test
    fun `permission revocation and dragging are enforced`() {
        manager.addDeletedItems(listOf(ItemStack(Material.DIAMOND)))
        manager.openChestGUI(player)
        player.isOp = false
        click(0)
        assertEquals(1, manager.deletedItemCount)
        val drag = mock(InventoryDragEvent::class.java)
        val view = player.openInventory
        `when`(drag.view).thenReturn(view)
        `when`(drag.rawSlots).thenReturn(setOf(1, 54))
        manager.onInventoryDrag(drag)
        verify(drag).setCancelled(true)
    }

    @Test
    fun `navigation is deferred until the following tick`() {
        manager.addDeletedItems(List(46) { ItemStack(Material.DIAMOND) })
        manager.openChestGUI(player)
        val first = player.openInventory.topInventory
        click(53)
        assertSame(first, player.openInventory.topInventory)
        server.scheduler.performOneTick()
        assertNotSame(first, player.openInventory.topInventory)
        assertNotNull(player.openInventory.topInventory.getItem(45))
        assertNull(player.openInventory.topInventory.getItem(1))
    }
}
