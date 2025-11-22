package de.customtrades.listener

import de.customtrades.CustomTradesPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Verwaltet die spezielle Sunflower die für PlayerPoints-Trades gegeben wird
 */
class PlayerPointsSunflowerListener(private val plugin: CustomTradesPlugin) : Listener {

    private val sunflowerKey = NamespacedKey(plugin, "pp_sunflower")
    private val activePlayers = mutableSetOf<Player>()

    /**
     * Gibt einem Spieler die spezielle Sunflower
     */
    fun giveSunflower(player: Player): Boolean {
        // Check ob Inventar Platz hat
        if (player.inventory.firstEmpty() == -1) {
            player.sendMessage(
                Component.text("Dein Inventar ist voll! Bitte räume etwas Platz frei.")
                    .color(NamedTextColor.RED)
            )
            return false
        }

        // Erstelle spezielle Sunflower
        val currencyName = plugin.getCurrencyName()
        val sunflower = ItemStack(Material.SUNFLOWER, 1)
        val meta = sunflower.itemMeta
        meta.displayName(Component.text("§6§l$currencyName Währung"))
        meta.lore(listOf(
            Component.text("§7Diese Sonnenblume repräsentiert"),
            Component.text("§7deine $currencyName für diesen Trade"),
            Component.text(""),
            Component.text("§c§lKann nicht bewegt oder weggeworfen werden!")
        ))
        // Markiere als spezielle Sunflower
        meta.persistentDataContainer.set(sunflowerKey, PersistentDataType.BYTE, 1)
        sunflower.itemMeta = meta

        // Gib Item
        player.inventory.addItem(sunflower)
        activePlayers.add(player)

        player.sendMessage(
            Component.text("Du hast eine spezielle Währungs-Sonnenblume erhalten!")
                .color(NamedTextColor.YELLOW)
        )

        return true
    }

    /**
     * Entfernt die Sunflower vom Spieler
     */
    fun removeSunflower(player: Player) {
        player.inventory.contents.forEachIndexed { index, item ->
            if (item != null && isSunflower(item)) {
                player.inventory.setItem(index, null)
            }
        }
        activePlayers.remove(player)
    }

    /**
     * Prüft ob Spieler die Sunflower hat
     */
    fun hasSunflower(player: Player): Boolean {
        return player.inventory.contents.any { item ->
            item != null && isSunflower(item)
        }
    }

    /**
     * Prüft ob ein Item die spezielle Sunflower ist
     */
    private fun isSunflower(item: ItemStack): Boolean {
        if (item.type != Material.SUNFLOWER) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(sunflowerKey, PersistentDataType.BYTE)
    }

    /**
     * Verhindere das Wegwerfen der Sunflower
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDrop(event: PlayerDropItemEvent) {
        if (isSunflower(event.itemDrop.itemStack)) {
            event.isCancelled = true
            event.player.sendMessage(
                Component.text("Du kannst die Währungs-Sonnenblume nicht wegwerfen!")
                    .color(NamedTextColor.RED)
            )
        }
    }

    /**
     * Verhindere das Bewegen der Sunflower im Inventar (außer für Trades)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        val currentItem = event.currentItem
        val cursorItem = event.cursor

        // Check beide Items
        val hasSunflowerCurrent = currentItem != null && isSunflower(currentItem)
        val hasSunflowerCursor = cursorItem != null && isSunflower(cursorItem)

        if (!hasSunflowerCurrent && !hasSunflowerCursor) {
            return // Keine Sunflower beteiligt
        }

        // Erlaube NUR in Merchant-Inventar
        if (event.inventory is org.bukkit.inventory.MerchantInventory) {
            // Erlaube nur das Platzieren im Merchant-Slot, nicht das Zurücknehmen
            if (event.clickedInventory is org.bukkit.inventory.MerchantInventory) {
                return // OK - in Merchant platzieren
            }
        }

        // Alle anderen Aktionen blocken
        event.isCancelled = true
        player.sendMessage(
            Component.text("Du kannst die Währungs-Sonnenblume nur für Trades verwenden!")
                .color(NamedTextColor.RED)
        )
    }

    /**
     * Entferne Sunflower wenn Spieler Inventar schließt
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        // Entferne Sunflower immer wenn GUI geschlossen wird
        if (activePlayers.contains(player) || hasSunflower(player)) {
            // Verzögerung aus Config
            val delay = plugin.config.getLong("sunflower-cleanup-delay", 10L)
            plugin.debugLog("Sunflower-Cleanup geplant in $delay Ticks für ${player.name}")

            // Paper API: Verwende global region scheduler
            plugin.server.globalRegionScheduler.runDelayed(plugin, { _ ->
                if (hasSunflower(player)) {
                    removeSunflower(player)
                    player.sendMessage(
                        Component.text("Währungs-Sonnenblume wurde entfernt.")
                            .color(NamedTextColor.YELLOW)
                    )
                    plugin.debugLog("Sunflower entfernt von ${player.name}")
                }
            }, delay) // Ticks delay
        }
    }

    /**
     * Cleanup bei Logout
     */
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        removeSunflower(event.player)
    }
}

