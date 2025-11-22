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
                val currencyName = plugin.getCurrencyName()
                player.sendMessage(
                    Component.text("Du hast nicht genug $currencyName! Benötigt: ${tradeData.playerPointsCost}, Du hast: $currentPoints")
                        .color(NamedTextColor.RED)
                )
                event.isCancelled = true
                player.closeInventory()
                plugin.debugLog("${player.name} hat nicht genug $currencyName: ${currentPoints}/${tradeData.playerPointsCost}")
                return
            }

            // Sunflower-Prüfung NICHT mehr nötig!
            // Grund: Sunflower kann nicht aus dem Inventar entfernt werden (blockiert)
            // Wenn Spieler das GUI öffnen konnte, hat er die Sunflower garantiert
            plugin.debugLog("${player.name} hat genug ${plugin.getCurrencyName()}, Trade wird durchgeführt")

            // Deduct points and remove sunflower after successful trade
            // Schedule with delay from config to ensure trade completes first
            val delay = plugin.config.getLong("trade-delay", 5L)
            plugin.debugLog("Trade-Abschluss geplant in $delay Ticks für ${player.name}")

            // Paper API: Verwende global region scheduler statt legacy Bukkit scheduler
            plugin.server.globalRegionScheduler.runDelayed(plugin, { _ ->
                // Ziehe PlayerPoints ab
                ppAPI.api.take(player.uniqueId, tradeData.playerPointsCost)

                // Entferne Sunflower (falls noch vorhanden)
                if (plugin.sunflowerListener.hasSunflower(player)) {
                    plugin.sunflowerListener.removeSunflower(player)
                    plugin.debugLog("Sunflower entfernt von ${player.name}")
                }

                val currencyName = plugin.getCurrencyName()
                player.sendMessage(
                    Component.text("✓ ${tradeData.playerPointsCost} $currencyName wurden abgezogen.")
                        .color(NamedTextColor.GREEN)
                )
                plugin.debugLog("${tradeData.playerPointsCost} $currencyName abgezogen von ${player.name}")

                // Entferne Preis-Lore von allen Items im Inventar die vom Trade stammen
                player.inventory.contents.forEachIndexed { index, item ->
                    if (item != null && item.type != org.bukkit.Material.AIR) {
                        val cleanedItem = de.customtrades.util.TradeUtil.removePriceLore(item)
                        player.inventory.setItem(index, cleanedItem)
                    }
                }
                plugin.debugLog("Preis-Lore von Items entfernt für ${player.name}")
            }, delay) // Ticks delay
        }
    }
}

