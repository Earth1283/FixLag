package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.TestFixture
import org.bukkit.entity.ExperienceOrb
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import kotlin.test.*

class ExperienceOrbMergerTest : TestFixture() {
    @Test
    fun `merging preserves the experience in Paper orb counts`() {
        val first = mock(ExperienceOrb::class.java)
        val second = mock(ExperienceOrb::class.java)
        `when`(first.experience).thenReturn(5)
        `when`(first.count).thenReturn(3)
        `when`(second.experience).thenReturn(7)
        `when`(second.count).thenReturn(2)
        assertTrue(ExperienceOrbMerger(featurePlugin).mergeOrbs(first, second))
        verify(first).setExperience(29)
        verify(first).setCount(1)
        verify(second).remove()
    }

    @Test
    fun `experience overflow leaves both orbs intact`() {
        val first = mock(ExperienceOrb::class.java)
        val second = mock(ExperienceOrb::class.java)
        `when`(first.experience).thenReturn(Int.MAX_VALUE)
        `when`(second.experience).thenReturn(1)
        assertFalse(ExperienceOrbMerger(featurePlugin).mergeOrbs(first, second))
        verify(second, never()).remove()
    }
}
