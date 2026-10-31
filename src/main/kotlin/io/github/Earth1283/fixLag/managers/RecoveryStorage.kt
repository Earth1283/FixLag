package io.github.Earth1283.fixLag.managers

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/** The worker only receives strings and timestamps, never live Bukkit items. */
internal class RecoveryStorage(private val file: File, private val logger: Logger) {
    data class Record(val addedAt: Long, val encodedItem: String)
    private val executor = Executors.newSingleThreadExecutor { work ->
        Thread(work, "FixLag-recovery-save").apply { isDaemon = true }
    }
    private val lock = Any()
    private var pending: List<Record>? = null
    private var writing = false
    private var closed = false

    fun save(records: List<Record>) = synchronized(lock) {
        if (closed) return@synchronized
        pending = records.toList()
        if (!writing) {
            writing = true
            executor.execute { drain() }
        }
    }

    private fun drain() {
        while (true) {
            val snapshot = synchronized(lock) {
                val next = pending
                if (next == null) writing = false else pending = null
                next
            } ?: return
            try {
                val yaml = YamlConfiguration()
                yaml.set("format", 2)
                yaml.set("count", snapshot.size)
                snapshot.forEachIndexed { i, record ->
                    yaml.set("$i.added-at", record.addedAt)
                    yaml.set("$i.item-bytes", record.encodedItem)
                }
                Files.createDirectories(file.toPath().parent)
                val temporary = Files.createTempFile(file.toPath().parent, "recovery-", ".tmp")
                try {
                    Files.writeString(temporary, yaml.saveToString())
                    try {
                        Files.move(temporary, file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temporary, file.toPath(), REPLACE_EXISTING)
                    }
                } finally {
                    Files.deleteIfExists(temporary)
                }
            } catch (e: Exception) {
                logger.warning("Could not save deleted items: ${e.message}")
            }
        }
    }

    fun close() {
        synchronized(lock) {
            closed = true
            executor.shutdown()
        }
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Recovery save is still pending after 10 seconds.")
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
