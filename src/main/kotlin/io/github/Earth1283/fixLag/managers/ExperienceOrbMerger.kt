package io.github.Earth1283.fixLag.managers

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.Bukkit
import org.bukkit.entity.ExperienceOrb
import org.bukkit.scheduler.BukkitRunnable

class ExperienceOrbMerger(private val plugin: FixLag) : BukkitRunnable() {

    private val config: ConfigManager = plugin.configManager

    override fun run() {
        if (!config.isXpOrbMergerEnabled) return

        val radius = config.xpOrbMergerRadius
        val processed = mutableSetOf<ExperienceOrb>()

        for (world in Bukkit.getWorlds()) {
            for (orb in world.getEntitiesByClass(ExperienceOrb::class.java)) {
                if (orb in processed || orb.isDead) continue

                val nearby = world.getNearbyEntities(orb.location, radius, radius, radius) { e ->
                    e is ExperienceOrb && e !== orb && !e.isDead && e !in processed
                }

                if (nearby.isNotEmpty()) {
                    var totalXp = orb.experience
                    for (e in nearby) {
                        val other = e as ExperienceOrb
                        totalXp += other.experience
                        other.remove()
                        processed.add(other)
                    }
                    orb.experience = totalXp
                }
                processed.add(orb)
            }
        }
    }
}
