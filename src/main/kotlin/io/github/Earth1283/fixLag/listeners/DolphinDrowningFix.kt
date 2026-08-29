package io.github.Earth1283.fixLag.listeners

import io.github.Earth1283.fixLag.FixLag
import org.bukkit.entity.Dolphin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

// MC-306840: dolphin air-surfacing AI stalls without a nearby player, so they suffocate; they're never meant to drown.
class DolphinDrowningFix(plugin: FixLag) : Listener {

    private val config = plugin.configManager

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        if (!config.isDolphinDrowningFixEnabled) return
        if (event.cause != EntityDamageEvent.DamageCause.DROWNING) return
        if (event.entity !is Dolphin) return

        event.isCancelled = true
    }
}
