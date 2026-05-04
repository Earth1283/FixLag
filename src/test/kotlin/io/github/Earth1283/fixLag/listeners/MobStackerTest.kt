package io.github.Earth1283.fixLag.listeners

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import io.github.Earth1283.fixLag.FixLag
import io.github.Earth1283.fixLag.managers.ConfigManager
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.CreatureSpawnEvent
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MobStackerTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: FixLag
    private lateinit var configManager: ConfigManager
    private lateinit var mobStacker: MobStacker

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(FixLag::class.java)
        configManager = mock(ConfigManager::class.java)
        mobStacker = MobStacker(plugin, configManager)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun testMobStackingEnabled() {
        `when`(configManager.isMobStackingEnabled).thenReturn(true)
        `when`(configManager.mobStackingAllowedEntities).thenReturn(setOf("ZOMBIE"))
        `when`(configManager.mobStackingRadius).thenReturn(10)
        `when`(configManager.mobStackingMaxStackSize).thenReturn(5)
        `when`(configManager.mobStackingNameFormat).thenReturn("x%count%")

        val world = server.addSimpleWorld("world")
        val loc = Location(world, 0.0, 0.0, 0.0)

        // Spawn first zombie
        val z1 = world.spawn(loc, Zombie::class.java)
        
        // We create a second zombie to spawn
        val z2 = world.createEntity(loc, Zombie::class.java)
        
        val event = CreatureSpawnEvent(z2, CreatureSpawnEvent.SpawnReason.NATURAL)
        
        // Run logic
        mobStacker.onCreatureSpawn(event)
        
        // Verification:
        assertTrue(event.isCancelled, "Event should be cancelled due to stacking")
        assertNotNull(z1.customName, "Stacked entity should have a custom name")
    }
}
