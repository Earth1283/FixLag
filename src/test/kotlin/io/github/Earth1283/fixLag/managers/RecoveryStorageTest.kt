package io.github.Earth1283.fixLag.managers

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.test.*

class RecoveryStorageTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `the last snapshot wins and shutdown flushes it`() {
        val file = directory.resolve("deleted_items.yml").toFile()
        val storage = RecoveryStorage(file, Logger.getAnonymousLogger())
        repeat(100) { generation ->
            storage.save(List(20) { RecoveryStorage.Record(generation.toLong(), "item-$generation") })
        }
        storage.save(listOf(RecoveryStorage.Record(1000, "final")))
        storage.close()
        val yaml = YamlConfiguration.loadConfiguration(file)
        assertEquals(2, yaml.getInt("format"))
        assertEquals(1, yaml.getInt("count"))
        assertEquals("final", yaml.getString("0.item-bytes"))
        assertEquals(1000L, yaml.getLong("0.added-at"))
        assertEquals(listOf("deleted_items.yml"), directory.toFile().list()!!.toList())
    }

    @Test
    fun `clearing replaces an earlier snapshot with an empty file`() {
        val file = directory.resolve("deleted_items.yml").toFile()
        val storage = RecoveryStorage(file, Logger.getAnonymousLogger())
        storage.save(List(500) { RecoveryStorage.Record(1, "old") })
        storage.save(emptyList())
        storage.close()
        val yaml = YamlConfiguration.loadConfiguration(file)
        assertEquals(0, yaml.getInt("count", -1))
        assertFalse(yaml.contains("0"))
    }
}
