package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.TestFixture
import io.github.Earth1283.fixLag.listeners.CollisionOptimizer
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Zombie
import org.bukkit.event.world.ChunkUnloadEvent
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import kotlin.test.*

class EntityLifecycleTest : TestFixture() {
    @Test
    fun `disabling panic mode restores frozen mobs and clears ownership`() {
        val zombie = mock(Zombie::class.java)
        scanWorld(zombie)
        `when`(zombie.isAware).thenReturn(true)
        server.currentTps = 10.0
        val panic = PanicModeManager(featurePlugin)
        panic.run()
        repeat(10) { server.scheduler.performOneTick() }
        assertTrue(panic.isPanicActive)
        assertEquals(1, panic.frozenMobCount)
        verify(zombie).setAware(false)
        configure("panic-mode.enabled", false)
        panic.run()
        verify(zombie).setAware(true)
        assertFalse(panic.isPanicActive)
        assertEquals(0, panic.frozenMobCount)
    }

    @Test
    fun `chunk unload restores frozen mobs before they are saved`() {
        val zombie = mock(Zombie::class.java)
        val (_, chunk) = scanWorld(zombie)
        `when`(zombie.isAware).thenReturn(true)
        server.currentTps = 10.0
        val panic = PanicModeManager(featurePlugin)
        panic.run()
        repeat(10) { server.scheduler.performOneTick() }
        panic.onChunkUnload(ChunkUnloadEvent(chunk, true))
        verify(zombie).setAware(true)
        assertEquals(0, panic.frozenMobCount)
        panic.restoreAll()
    }

    @Test
    fun `panic recovery restores tracked ids even after their scan chunk unloads`() {
        val zombie = mock(Zombie::class.java)
        val (_, chunk) = scanWorld(zombie)
        `when`(zombie.isAware).thenReturn(true)
        server.currentTps = 10.0
        val panic = PanicModeManager(featurePlugin)
        panic.run()
        repeat(10) { server.scheduler.performOneTick() }
        // UUID lookup still finds the entity after it moves away from the original scan chunk.
        `when`(chunk.isLoaded).thenReturn(false)
        server.currentTps = 20.0
        panic.run()
        repeat(10) { server.scheduler.performOneTick() }
        verify(zombie).setAware(true)
        assertEquals(0, panic.frozenMobCount)
    }

    @Test
    fun `collision changes are undone on shutdown`() {
        val zombie = mock(Zombie::class.java)
        scanWorld(zombie)
        `when`(zombie.isCollidable).thenReturn(true)
        server.currentTps = 10.0
        val optimizer = CollisionOptimizer(featurePlugin)
        optimizer.run()
        repeat(10) { server.scheduler.performOneTick() }
        verify(zombie).setCollidable(false)
        optimizer.restoreAll()
        verify(zombie).setCollidable(true)
    }

    @Test
    fun `overload detection runs and honors player exemption`() {
        configure("overload-detection.critical-entity-count", 1)
        val a = mock(Item::class.java)
        val b = mock(Item::class.java)
        val (world, _) = scanWorld(a, b)
        for (item in listOf(a, b)) {
            `when`(item.type).thenReturn(EntityType.ITEM)
            `when`(item.location).thenReturn(Location(world, 0.0, 64.0, 0.0))
        }
        val player = server.addPlayer()
        player.teleport(Location(world, 0.0, 64.0, 0.0))
        val checker = OverloadChecker(featurePlugin)
        checker.startChecking(1L, 1000L)
        repeat(20) { server.scheduler.performOneTick() }
        verify(messages).getMessage("overload_warning", "<count>", "2", "<player>", player.name)
        clearInvocations(messages)
        player.addAttachment(plugin, "fixlag.overload.exempt", true)
        checker.startChecking(1L, 1000L)
        repeat(20) { server.scheduler.performOneTick() }
        verifyNoInteractions(messages)
        checker.stopChecking()
    }
}
