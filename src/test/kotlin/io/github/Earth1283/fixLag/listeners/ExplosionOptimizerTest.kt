package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.TestFixture
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.event.entity.EntityExplodeEvent
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import kotlin.test.*

class ExplosionOptimizerTest : TestFixture() {
    @Test
    fun `explosion storms retain limits without flooding the log`() {
        server.currentTps = 10.0
        val optimizer = ExplosionOptimizer(featurePlugin)
        val location = Location(null, 0.0, 64.0, 0.0)
        val block = mock(Block::class.java)
        repeat(100) {
            val blocks = MutableList(200) { block }
            val event = mock(EntityExplodeEvent::class.java)
            `when`(event.blockList()).thenReturn(blocks)
            `when`(event.location).thenReturn(location)
            optimizer.onEntityExplode(event)
            assertEquals(100, blocks.size)
        }
        // Config setup itself logs nothing with this valid configuration.
        val logs = mockingDetails(messages).invocations.count { it.method.name == "logInfo" && it.arguments.firstOrNull() == "log_explosion_optimized" }
        assertEquals(1, logs)
    }
}
