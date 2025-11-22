package de.customtrades.ui

import de.customtrades.CustomTradesPlugin
import de.customtrades.data.ItemData
import de.customtrades.data.TradeData
import de.customtrades.data.TraderData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class TradeEditorGUI(
    private val plugin: CustomTradesPlugin,
    private val trader: TraderData
) : Listener {

    private lateinit var inventory: Inventory
    private var currentPlayer: Player? = null

    fun open(player: Player) {
        currentPlayer = player
        inventory = Bukkit.createInventory(null, 54, Component.text("Trade Editor: ${trader.name}"))

        plugin.server.pluginManager.registerEvents(this, plugin)

        setupInventory()
        player.openInventory(inventory)
    }

    private fun setupInventory() {
        inventory.clear()

        // Header
        val info = createItem(
            Material.BOOK,
            "§6Trader: ${trader.name}",
            listOf(
                "§7Mob: ${trader.mobType}",
                "§7Trades: ${trader.trades.size}"
            )
        )
        inventory.setItem(4, info)

        // Add trade button
        val addTrade = createItem(
            Material.EMERALD,
            "§a+ Neuen Trade hinzufügen",
            listOf("§7Klicke, um einen neuen Trade zu erstellen")
        )
        inventory.setItem(48, addTrade)

        // Close button
        val close = createItem(
            Material.BARRIER,
            "§cSchließen",
            listOf("§7Änderungen werden gespeichert")
        )
        inventory.setItem(49, close)

        // AI Toggle button
        val aiToggle = createItem(
            if (trader.hasAI) Material.LIME_DYE else Material.GRAY_DYE,
            if (trader.hasAI) "§aAI: Aktiviert" else "§7AI: Deaktiviert",
            listOf(
                "§7Klicke zum Umschalten",
                "",
                if (trader.hasAI) {
                    "§aTrader kann sich bewegen"
                } else {
                    "§7Trader steht still"
                },
                if (trader.hasAI) {
                    "§aTrader schaut Spieler an"
                } else {
                    "§7Trader schaut geradeaus"
                }
            )
        )
        inventory.setItem(47, aiToggle)

        // Invulnerable Toggle button
        val invulnerableToggle = createItem(
            if (trader.invulnerable) Material.SHIELD else Material.WOODEN_SWORD,
            if (trader.invulnerable) "§aUnverwundbar: AN" else "§cUnverwundbar: AUS",
            listOf(
                "§7Klicke zum Umschalten",
                "",
                if (trader.invulnerable) {
                    "§aTrader kann nicht getötet werden"
                } else {
                    "§cTrader kann getötet werden"
                },
                if (trader.invulnerable) {
                    "§aTrader nimmt keinen Schaden"
                } else {
                    "§cTrader nimmt Schaden"
                }
            )
        )
        inventory.setItem(46, invulnerableToggle)

        // Save button
        val save = createItem(
            Material.WRITABLE_BOOK,
            "§aSpeichern",
            listOf("§7Speichert alle Änderungen")
        )
        inventory.setItem(50, save)

        // Display existing trades
        trader.trades.forEachIndexed { index, trade ->
            if (index < 36) { // Max 36 trades displayable
                val slot = index + 9 // Start at row 2
                val tradeItem = createTradeDisplayItem(trade, index)
                inventory.setItem(slot, tradeItem)
            }
        }
    }

    private fun createTradeDisplayItem(trade: TradeData, index: Int): ItemStack {
        val lore = mutableListOf<String>()

        lore.add("§7Input 1: ${getItemDescription(trade.input1)}")
        trade.input2?.let {
            lore.add("§7Input 2: ${getItemDescription(it)}")
        }
        lore.add("§7Output: ${getItemDescription(trade.output)}")

        if (trade.playerPointsCost > 0) {
            lore.add("§6PlayerPoints: ${trade.playerPointsCost}")
        }

        if (trade.maxUses > 0) {
            lore.add("§7Max Uses: ${trade.maxUses}")
        } else {
            lore.add("§7Max Uses: Unlimited")
        }

        lore.add("")
        lore.add("§eLinksklick: Trade bearbeiten")
        lore.add("§cRechtsklick: Trade löschen")

        return createItem(Material.PAPER, "§aTrade #${index + 1}", lore)
    }

    private fun getItemDescription(itemData: ItemData): String {
        return when (itemData.type) {
            ItemData.ItemType.VANILLA -> "${itemData.amount}x ${itemData.material}"
            ItemData.ItemType.NEXO -> "${itemData.amount}x Nexo:${itemData.nexoId}"
            ItemData.ItemType.NONE -> "Kein Item"
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory != inventory) return
        if (event.whoClicked != currentPlayer) return

        event.isCancelled = true

        val player = event.whoClicked as Player
        val slot = event.slot
        val item = event.currentItem ?: return

        when (slot) {
            46 -> { // Invulnerable Toggle
                // Toggle Invulnerable und aktualisiere Trader
                val newTrader = trader.copy(invulnerable = !trader.invulnerable)
                plugin.traderManager.updateTrader(newTrader)

                // Aktualisiere Entity Invulnerable in real-time
                val world = plugin.server.getWorld(trader.location.world)
                world?.entities?.forEach { entity ->
                    if (plugin.traderManager.isTrader(entity)) {
                        val traderName = entity.persistentDataContainer.get(
                            org.bukkit.NamespacedKey(plugin, "trader_name"),
                            org.bukkit.persistence.PersistentDataType.STRING
                        )
                        if (traderName == trader.name && entity is org.bukkit.entity.LivingEntity) {
                            entity.isInvulnerable = newTrader.invulnerable
                        }
                    }
                }

                player.sendMessage(
                    Component.text("Unverwundbar ${if (newTrader.invulnerable) "aktiviert" else "deaktiviert"}!")
                        .color(if (newTrader.invulnerable) NamedTextColor.GREEN else NamedTextColor.RED)
                )

                // Inventory schließen und neu öffnen mit aktualisiertem Trader
                player.closeInventory()
                TradeEditorGUI(plugin, newTrader).open(player)
            }
            47 -> { // AI Toggle
                // Toggle AI und aktualisiere Trader
                val newTrader = trader.copy(hasAI = !trader.hasAI)
                plugin.traderManager.updateTrader(newTrader)

                // Aktualisiere Entity AI in real-time
                val world = plugin.server.getWorld(trader.location.world)
                world?.entities?.forEach { entity ->
                    if (plugin.traderManager.isTrader(entity)) {
                        val traderName = entity.persistentDataContainer.get(
                            org.bukkit.NamespacedKey(plugin, "trader_name"),
                            org.bukkit.persistence.PersistentDataType.STRING
                        )
                        if (traderName == trader.name && entity is org.bukkit.entity.LivingEntity) {
                            entity.setAI(newTrader.hasAI)
                        }
                    }
                }

                player.sendMessage(
                    Component.text("AI ${if (newTrader.hasAI) "aktiviert" else "deaktiviert"}!")
                        .color(if (newTrader.hasAI) NamedTextColor.GREEN else NamedTextColor.GRAY)
                )

                // Inventory schließen und neu öffnen mit aktualisiertem Trader
                player.closeInventory()
                TradeEditorGUI(plugin, newTrader).open(player)
            }
            48 -> { // Add trade
                openTradeCreator(player)
            }
            49 -> { // Close
                player.closeInventory()
            }
            50 -> { // Save
                plugin.traderManager.updateTrader(trader)
                player.sendMessage(Component.text("Trader gespeichert!").color(NamedTextColor.GREEN))
                player.closeInventory()
            }
            in 9..44 -> { // Trade slots
                val tradeIndex = slot - 9
                if (tradeIndex < trader.trades.size) {
                    if (event.isLeftClick) {
                        openTradeEditor(player, tradeIndex)
                    } else if (event.isRightClick) {
                        trader.trades.removeAt(tradeIndex)
                        setupInventory()
                        player.sendMessage(Component.text("Trade gelöscht!").color(NamedTextColor.YELLOW))
                    }
                }
            }
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory) return
        if (event.player != currentPlayer) return

        HandlerList.unregisterAll(this)
    }

    private fun openTradeCreator(player: Player) {
        // Erstelle leeren Trade und füge ihn sofort hinzu
        val newTrade = TradeData(
            input1 = ItemData(type = ItemData.ItemType.NONE),
            input2 = null,
            output = ItemData(type = ItemData.ItemType.NONE),
            maxUses = -1,
            playerPointsCost = 0
        )
        trader.trades.add(newTrade)
        plugin.traderManager.updateTrader(trader)

        player.sendMessage(Component.text("§aLeerer Trade erstellt! Bearbeite ihn jetzt.").color(NamedTextColor.GREEN))

        // Öffne Editor für den neu erstellten Trade (letzter Index)
        TradeCreatorGUI(plugin, trader, trader.trades.size - 1).open(player)
    }

    private fun openTradeEditor(player: Player, tradeIndex: Int) {
        val trade = trader.trades.getOrNull(tradeIndex) ?: return
        player.sendMessage(Component.text("§6Trade Editor wird geöffnet..."))
        TradeCreatorGUI(plugin, trader, tradeIndex).open(player)
    }

    private fun createItem(material: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(Component.text(name))
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { Component.text(it) })
        }
        item.itemMeta = meta
        return item
    }
}

