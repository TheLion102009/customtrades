package de.customtrades.listener

import de.customtrades.CustomTradesPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

/**
 * Verhindert dass Trader Schaden nehmen wenn sie unverwundbar sind
 */
class TraderProtectionListener(private val plugin: CustomTradesPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity

        // Prüfe ob es ein Trader ist
        if (!plugin.traderManager.isTrader(entity)) {
            return
        }

        // Hole Trader-Daten
        val trader = plugin.traderManager.getTraderByEntity(entity) ?: return

        // Wenn Trader unverwundbar ist, verhindere Schaden
        if (trader.invulnerable) {
            event.isCancelled = true
        }
    }
}

