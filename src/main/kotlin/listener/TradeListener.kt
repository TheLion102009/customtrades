package de.customtrades.listener

import de.customtrades.CustomTradesPlugin
import de.customtrades.data.ItemData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.black_ixx.playerpoints.PlayerPoints
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.MerchantInventory

class TradeListener(private val plugin: CustomTradesPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onTrade(event: InventoryClickEvent) {
        val inventory = event.inventory

        if (inventory !is MerchantInventory) {
            return
        }

        val player = event.whoClicked as? Player ?: return
        val merchant = inventory.merchant ?: return

        // Check if this is a custom trader
        val entity = merchant as? org.bukkit.entity.Entity ?: return
        if (!plugin.traderManager.isTrader(entity)) {
            return
        }

        // Nur wenn auf das Result-Item geklickt wird (Slot 2)
        if (event.slotType != org.bukkit.event.inventory.InventoryType.SlotType.RESULT) {
            return
        }

        val trader = plugin.traderManager.getTraderByEntity(entity) ?: return

        // Get selected recipe
        val selectedRecipe = inventory.selectedRecipe ?: return

        // Find matching trade data
        val tradeData = trader.trades.find { trade ->
            val recipe = de.customtrades.util.TradeUtil.createSingleMerchantRecipe(plugin, trade)
            recipe?.result?.isSimilar(selectedRecipe.result) == true
        } ?: return

        // Check PlayerPoints cost
        if (tradeData.playerPointsCost > 0) {
            if (!plugin.hasPlayerPoints) {
                player.sendMessage(
                    Component.text("Dieser Trade benötigt PlayerPoints, aber das Plugin ist nicht installiert!")
                        .color(NamedTextColor.RED)
                )
                event.isCancelled = true
                return
            }

            val ppAPI = Bukkit.getPluginManager().getPlugin("PlayerPoints") as? PlayerPoints
            if (ppAPI == null) {
                event.isCancelled = true
                return
            }

            val currentPoints = ppAPI.api.look(player.uniqueId)

            if (currentPoints < tradeData.playerPointsCost) {
                player.sendMessage(
                    Component.text("Du hast nicht genug PlayerPoints! Benötigt: ${tradeData.playerPointsCost}, Du hast: $currentPoints")
                        .color(NamedTextColor.RED)
                )
                event.isCancelled = true
                player.closeInventory()
                return
            }

            // Prüfe ob Spieler die Sunflower hat
            if (!plugin.sunflowerListener.hasSunflower(player)) {
                player.sendMessage(
                    Component.text("Du hast die Währungs-Sonnenblume nicht! Trade abgebrochen.")
                        .color(NamedTextColor.RED)
                )
                event.isCancelled = true
                player.closeInventory()
                return
            }

            // Deduct points and remove sunflower after successful trade
            // Schedule with delay to ensure trade completes first
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                // Prüfe nochmal ob Sunflower noch da ist (Trade könnte abgebrochen sein)
                if (plugin.sunflowerListener.hasSunflower(player)) {
                    ppAPI.api.take(player.uniqueId, tradeData.playerPointsCost)
                    plugin.sunflowerListener.removeSunflower(player)
                    player.sendMessage(
                        Component.text("✓ ${tradeData.playerPointsCost} PlayerPoints wurden abgezogen.")
                            .color(NamedTextColor.GREEN)
                    )
                }
            }, 1L) // 1 Tick delay
        }
    }
}

