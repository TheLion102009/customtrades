package de.customtrades.listener

import de.customtrades.CustomTradesPlugin
import de.customtrades.util.TradeUtil
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent

class TraderInteractListener(private val plugin: CustomTradesPlugin) : Listener {

    @EventHandler
    fun onTraderInteract(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        val player = event.player

        if (!plugin.traderManager.isTrader(entity)) {
            return
        }

        event.isCancelled = true

        val trader = plugin.traderManager.getTraderByEntity(entity) ?: return

        // Prüfe ob Trader PlayerPoints-Trades hat
        val hasPlayerPointsTrades = trader.trades.any { it.playerPointsCost > 0 }

        if (hasPlayerPointsTrades && plugin.hasPlayerPoints) {
            // Gib Spieler die spezielle Sunflower
            val sunflowerListener = plugin.server.pluginManager.getPlugin("CustomTrades")
                ?.let { plugin.sunflowerListener }

            if (sunflowerListener != null && !sunflowerListener.hasSunflower(player)) {
                if (!sunflowerListener.giveSunflower(player)) {
                    // Inventar voll - breche ab
                    return
                }
            }
        }

        // Open merchant UI for villagers and wandering traders
        when (entity) {
            is Villager -> {
                openTraderUI(player, entity, trader.name)
            }
            is WanderingTrader -> {
                openTraderUI(player, entity, trader.name)
            }
        }
    }

    private fun openTraderUI(player: Player, merchant: org.bukkit.inventory.Merchant, traderName: String) {
        val trader = plugin.traderManager.getTrader(traderName) ?: return

        // Clear existing recipes
        merchant.recipes = mutableListOf()

        // Add custom trades
        val recipes = TradeUtil.createMerchantRecipes(plugin, trader)
        merchant.recipes = recipes

        // Open merchant UI
        player.openMerchant(merchant, true)
    }
}

