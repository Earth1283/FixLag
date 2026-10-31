package io.github.Earth1283.fixLag.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.logging.Level

/** Visits loaded entities without processing a whole world in one server tick. */
class EntityScan(private val plugin: JavaPlugin) {
    private var task: BukkitTask? = null
    val isRunning: Boolean get() = task != null

    fun start(visit: (Entity) -> Unit, complete: () -> Unit = {}, afterBatch: () -> Unit = {}): Boolean {
        if (isRunning) return false
        val chunks = Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.iterator()
        var entities: Iterator<Entity> = emptyList<Entity>().iterator()
        schedule({
            val deadline = System.nanoTime() + 2_000_000L
            var visited = 0
            var openedChunks = 0
            var finished = false
            try {
                while (isRunning && visited < 128 && openedChunks < 4 && System.nanoTime() < deadline) {
                    if (!entities.hasNext()) {
                        if (!chunks.hasNext()) { finished = true; break }
                        val chunk = chunks.next()
                        openedChunks++
                        // Never load a chunk that unloaded while the scan was in progress.
                        if (!chunk.isLoaded || !chunk.isEntitiesLoaded) continue
                        entities = chunk.entities.iterator()
                        continue
                    }
                    val entity = entities.next()
                    visited++
                    if (entity.isValid) visit(entity)
                }
            } finally {
                afterBatch()
            }
            finished
        }, complete)
        return true
    }

    /** Restore tracked entities by ID, even if they moved to another chunk or world. */
    fun startIds(ids: Collection<UUID>, visit: (Entity) -> Unit, complete: () -> Unit = {}): Boolean {
        if (isRunning) return false
        val iterator = ids.toList().iterator()
        schedule({
            val deadline = System.nanoTime() + 2_000_000L
            var visited = 0
            while (isRunning && iterator.hasNext() && visited < 128 && System.nanoTime() < deadline) {
                Bukkit.getEntity(iterator.next())?.let(visit)
                visited++
            }
            !iterator.hasNext()
        }, complete)
        return true
    }

    private fun schedule(batch: () -> Boolean, complete: () -> Unit) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            try {
                if (batch()) {
                    stop()
                    complete()
                }
            } catch (e: Exception) {
                // A failed callback must not leave the scan permanently marked as running.
                stop()
                plugin.logger.log(Level.WARNING, "Entity scan failed", e)
            }
        }, 1L, 1L)
    }

    fun stop() {
        task?.cancel()
        task = null
    }
}
