package io.github.Earth1283.fixLag

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.MockPlugin
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.inventory.InventoryMock
import be.seeseemelk.mockbukkit.WorldMock
import io.github.Earth1283.fixLag.managers.ConfigManager
import io.github.Earth1283.fixLag.managers.MessageManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.inventory.InventoryHolder
import org.bukkit.Chunk
import org.bukkit.entity.Entity
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*

class TestServer : ServerMock() {
    var currentTps = 20.0
    val tracked = mutableMapOf<UUID, Entity>()
    override fun getTPS(): DoubleArray = doubleArrayOf(currentTps, currentTps, currentTps)
    override fun getEntity(id: UUID): Entity? = tracked[id] ?: super.getEntity(id)
    override fun createInventory(holder: InventoryHolder?, size: Int, title: Component): InventoryMock =
        super.createInventory(holder, size, PlainTextComponentSerializer.plainText().serialize(title))
}

open class TestFixture {
    lateinit var server: TestServer
    lateinit var plugin: MockPlugin
    lateinit var config: ConfigManager
    lateinit var messages: MessageManager
    lateinit var featurePlugin: FixLag

    @BeforeEach
    fun setupFixture() {
        // This MockBukkit release lacks the Component-title overload. Keep real inventory storage.
        server = MockBukkit.mock(TestServer())
        plugin = MockBukkit.createMockPlugin()
        messages = mock(MessageManager::class.java) { call ->
            when (call.method.returnType) {
                String::class.java -> "test message"
                Component::class.java -> Component.text("test message")
                else -> RETURNS_DEFAULTS.answer(call)
            }
        }
        config = ConfigManager(plugin, messages)
        configure("deleted-items-persistence.enabled", false)
        featurePlugin = mock(FixLag::class.java)
        `when`(featurePlugin.configManager).thenReturn(config)
        `when`(featurePlugin.messageManager).thenReturn(messages)
        `when`(featurePlugin.server).thenReturn(server)
        `when`(featurePlugin.name).thenReturn("FixLag")
        `when`(featurePlugin.isEnabled).thenReturn(true)
        `when`(featurePlugin.logger).thenReturn(plugin.logger)
        `when`(featurePlugin.config).thenReturn(plugin.config)
    }

    fun configure(path: String, value: Any) {
        plugin.config.set(path, value)
        plugin.saveConfig()
        config.loadConfig()
        if (::featurePlugin.isInitialized) `when`(featurePlugin.config).thenReturn(plugin.config)
    }

    fun scanWorld(vararg entities: Entity): Pair<WorldMock, Chunk> {
        val world = spy(WorldMock())
        server.addWorld(world)
        val chunk = mock(Chunk::class.java)
        `when`(chunk.isLoaded).thenReturn(true)
        `when`(chunk.isEntitiesLoaded).thenReturn(true)
        `when`(chunk.entities).thenReturn(entities)
        doReturn(arrayOf(chunk)).`when`(world).loadedChunks
        for (entity in entities) {
            val id = UUID.randomUUID()
            `when`(entity.uniqueId).thenReturn(id)
            `when`(entity.isValid).thenReturn(true)
            `when`(entity.world).thenReturn(world)
            server.tracked[id] = entity
        }
        return world to chunk
    }

    @AfterEach
    fun cleanupFixture() { MockBukkit.unmock() }
}
