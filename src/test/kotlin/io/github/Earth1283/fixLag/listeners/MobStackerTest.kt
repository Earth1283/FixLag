package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.TestFixture
import be.seeseemelk.mockbukkit.persistence.PersistentDataContainerMock
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.*
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.persistence.PersistentDataContainer
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.UUID
import kotlin.test.*

class MobStackerTest : TestFixture() {
    @Test
    fun `matching mobs stack without unimplemented MockBukkit entity creation`() {
        configure("mob-stacking.enabled", true)
        val world = mock(World::class.java)
        val location = Location(world, 0.0, 64.0, 0.0)
        fun zombie(): Zombie = mock(Zombie::class.java).also { mob ->
            `when`(mob.type).thenReturn(EntityType.ZOMBIE)
            `when`(mob.isValid).thenReturn(true)
            `when`(mob.hasAI()).thenReturn(true)
            `when`(mob.isAware).thenReturn(true)
            `when`(mob.location).thenReturn(location)
            `when`(mob.uniqueId).thenReturn(UUID.randomUUID())
            `when`(mob.persistentDataContainer).thenReturn(PersistentDataContainerMock())
        }
        val first = zombie()
        val second = zombie()
        `when`(world.getNearbyEntities(location, 10.0, 10.0, 10.0)).thenReturn(listOf(first))
        val event = CreatureSpawnEvent(second, CreatureSpawnEvent.SpawnReason.NATURAL)
        MobStacker(plugin, config).onCreatureSpawn(event)
        assertTrue(event.isCancelled)
        verify(first).setCustomName(anyString())
    }

    @Test
    fun `sheep color and shearing state are preserved by compatibility checks`() {
        val first = mock(Sheep::class.java)
        val second = mock(Sheep::class.java)
        `when`(first.type).thenReturn(EntityType.SHEEP)
        `when`(second.type).thenReturn(EntityType.SHEEP)
        `when`(first.color).thenReturn(DyeColor.WHITE)
        `when`(second.color).thenReturn(DyeColor.RED)
        val stacker = MobStacker(plugin, config)
        assertFalse(stacker.compatible(first, second))
        `when`(second.color).thenReturn(DyeColor.WHITE)
        assertTrue(stacker.compatible(first, second))
        `when`(second.isSheared).thenReturn(true)
        assertFalse(stacker.compatible(first, second))
    }

    @Test
    fun `baby and adult zombies never share a stack`() {
        val first = mock(Zombie::class.java)
        val second = mock(Zombie::class.java)
        `when`(first.type).thenReturn(EntityType.ZOMBIE)
        `when`(second.type).thenReturn(EntityType.ZOMBIE)
        `when`(second.isBaby).thenReturn(true)
        assertFalse(MobStacker(plugin, config).compatible(first, second))
    }
}
