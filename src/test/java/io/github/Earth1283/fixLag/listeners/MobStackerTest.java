package io.github.Earth1283.fixLag.listeners;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.Earth1283.fixLag.FixLag;
import io.github.Earth1283.fixLag.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MobStackerTest {

    private ServerMock server;
    private FixLag plugin;
    private ConfigManager configManager;
    private MobStacker mobStacker;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(FixLag.class);
        configManager = mock(ConfigManager.class);
        mobStacker = new MobStacker(plugin, configManager);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testMobStackingEnabled() {
        when(configManager.isMobStackingEnabled()).thenReturn(true);
        when(configManager.getMobStackingAllowedEntities()).thenReturn(Set.of("ZOMBIE"));
        when(configManager.getMobStackingRadius()).thenReturn(10);
        when(configManager.getMobStackingMaxStackSize()).thenReturn(5);
        when(configManager.getMobStackingNameFormat()).thenReturn("x%count%");

        World world = server.addSimpleWorld("world");
        Location loc = new Location(world, 0, 0, 0);

        // Spawn first zombie
        Zombie z1 = world.spawn(loc, Zombie.class);
        
        // Ensure z1 is recognized as a valid candidate for stacking (mock behavior might limit this)
        // We create a second zombie to spawn
        Zombie z2 = world.createEntity(loc, Zombie.class);
        
        CreatureSpawnEvent event = new CreatureSpawnEvent(z2, CreatureSpawnEvent.SpawnReason.NATURAL);
        
        // Run logic
        mobStacker.onCreatureSpawn(event);
        
        // Verification:
        // We expect z2 spawn to be cancelled (stacked into z1)
        // But implementation checks nearby entities. MockBukkit's `world.getNearbyEntities` logic needs to find z1.
        // `world.spawn` adds it to the world.
        
        // Assert: 
        assertTrue(event.isCancelled(), "Event should be cancelled due to stacking");
        
        // Check z1 stack data/name
        // This relies on MobStacker implementation details (PersistentDataContainer)
        // Since we can't easily check PDC in this basic mock without deep introspection or helpers, 
        // we assume the interaction worked if canceled.
        // We can check if name was updated.
        // verify(z1).setCustomName(anyString()); // z1 is a real object from MockBukkit, not a Mockito mock. Use getters.
        
        assertNotNull(z1.getCustomName(), "Stacked entity should have a custom name");
    }
}
