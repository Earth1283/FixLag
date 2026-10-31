package io.github.Earth1283.fixLag.utils

import be.seeseemelk.mockbukkit.WorldMock
import io.github.Earth1283.fixLag.TestFixture
import org.bukkit.Chunk
import org.bukkit.entity.Entity
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import kotlin.test.*

class EntityScanTest : TestFixture() {
    @Test
    fun `a failed visitor releases the scan and flushes its batch`() {
        val entity = mock(Entity::class.java)
        scanWorld(entity)
        val scan = EntityScan(plugin)
        var flushed = false
        scan.start({ throw IllegalStateException("test visitor failure") }, afterBatch = { flushed = true })
        repeat(10) { server.scheduler.performOneTick() }
        assertFalse(scan.isRunning)
        assertTrue(flushed)
        var complete = false
        assertTrue(scan.start({}, { complete = true }))
        repeat(10) { server.scheduler.performOneTick() }
        assertTrue(complete)
    }

    @Test
    fun `large chunks are processed over multiple ticks and overlapping scans are rejected`() {
        val world = spy(WorldMock())
        server.addWorld(world)
        val entities = Array(400) { mock(Entity::class.java).also { `when`(it.isValid).thenReturn(true) } }
        val chunk = mock(Chunk::class.java)
        `when`(chunk.isLoaded).thenReturn(true)
        `when`(chunk.isEntitiesLoaded).thenReturn(true)
        `when`(chunk.entities).thenReturn(entities)
        doReturn(arrayOf(chunk)).`when`(world).loadedChunks
        val scan = EntityScan(plugin)
        var visited = 0
        var complete = false
        assertTrue(scan.start({ visited++ }, { complete = true }))
        assertFalse(scan.start({ error("overlapping scan") }))
        server.scheduler.performOneTick()
        assertTrue(visited <= 128)
        assertFalse(complete)
        repeat(100) { if (!complete) server.scheduler.performOneTick() }
        assertTrue(complete)
        assertEquals(400, visited)
        verify(chunk, times(1)).entities
        assertFalse(scan.isRunning)
    }

    @Test
    fun `chunks unloaded before their turn are never read or loaded`() {
        val world = spy(WorldMock())
        server.addWorld(world)
        val chunk = mock(Chunk::class.java)
        `when`(chunk.isLoaded).thenReturn(false)
        doReturn(arrayOf(chunk)).`when`(world).loadedChunks
        var complete = false
        EntityScan(plugin).start({ error("unloaded entity visited") }, { complete = true })
        repeat(5) { server.scheduler.performOneTick() }
        assertTrue(complete)
        verify(chunk, never()).entities
        verify(world, never()).getChunkAt(anyInt(), anyInt())
    }
}
