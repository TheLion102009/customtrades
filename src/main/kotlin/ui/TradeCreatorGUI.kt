package de.customtrades.ui

import com.nexomc.nexo.api.NexoItems
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
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class TradeCreatorGUI(
    private val plugin: CustomTradesPlugin,
    private val trader: TraderData,
    private val editIndex: Int? = null
) : Listener {

    private lateinit var inventory: Inventory
    private var currentPlayer: Player? = null

    private var input1: ItemData? = null
    private var input2: ItemData? = null
    private var output: ItemData? = null
    private var playerPointsCost: Int = 0
    private var maxUses: Int = -1

    private var waitingForInput: InputType? = null

    enum class InputType {
        PLAYER_POINTS,
        MAX_USES
    }

    init {
        // Load existing trade if editing
        editIndex?.let { index ->
            trader.trades.getOrNull(index)?.let { trade ->
                input1 = trade.input1
                input2 = trade.input2
                output = trade.output
                playerPointsCost = trade.playerPointsCost
                maxUses = trade.maxUses
            }
        }
    }

    fun open(player: Player) {
        currentPlayer = player
        inventory = Bukkit.createInventory(
            null,
            54,
            Component.text(if (editIndex != null) "Trade bearbeiten" else "Trade erstellen")
        )

        plugin.server.pluginManager.registerEvents(this, plugin)

        setupInventory()
        player.openInventory(inventory)
    }

    private fun setupInventory() {
        inventory.clear()

        // Input 1 slot
        val input1Valid = input1 != null && input1!!.type != ItemData.ItemType.NONE
        val input1Slot = createItem(
            Material.CHEST,
            "§6Input 1 ${if (input1Valid) "§a✓" else "§c✗"}",
            listOf(
                "§7Linksklick mit Item in Hand:",
                "§7  Item als Input setzen",
                "§7Rechtsklick: Optionen",
                if (input1Valid) "§a${getItemDescription(input1!!)}" else "§7Noch kein Item gesetzt"
            )
        )
        inventory.setItem(10, input1Slot)

        // Input 2 slot
        val input2Valid = input2 != null && input2!!.type != ItemData.ItemType.NONE
        val input2Slot = createItem(
            Material.CHEST,
            "§6Input 2 (Optional) ${if (input2Valid) "§a✓" else ""}",
            listOf(
                "§7Linksklick mit Item in Hand:",
                "§7  Item als Input setzen",
                "§7Rechtsklick: Optionen",
                if (input2Valid) "§a${getItemDescription(input2!!)}" else "§7Noch kein Item gesetzt"
            )
        )
        inventory.setItem(12, input2Slot)

        // Output slot
        val outputValid = output != null && output!!.type != ItemData.ItemType.NONE
        val outputSlot = createItem(
            Material.ENDER_CHEST,
            "§6Output ${if (outputValid) "§a✓" else "§c✗"}",
            listOf(
                "§7Linksklick mit Item in Hand:",
                "§7  Item als Output setzen",
                "§7Rechtsklick: Optionen",
                if (outputValid) "§a${getItemDescription(output!!)}" else "§7Noch kein Item gesetzt"
            )
        )
        inventory.setItem(16, outputSlot)

        // PlayerPoints cost
        val ppCost = createItem(
            Material.SUNFLOWER,
            "§6PlayerPoints Kosten: $playerPointsCost",
            listOf(
                "§7Linksklick: +10",
                "§7Rechtsklick: -10",
                "§7Shift+Klick: Manuell eingeben"
            )
        )
        inventory.setItem(28, ppCost)

        // Max uses
        val maxUsesItem = createItem(
            Material.HOPPER,
            "§6Max Uses: ${if (maxUses > 0) maxUses else "Unlimited"}",
            listOf(
                "§7Linksklick: +1",
                "§7Rechtsklick: -1",
                "§7Shift+Klick: Manuell eingeben",
                "§7-1 = Unlimited"
            )
        )
        inventory.setItem(30, maxUsesItem)

        // Clear input1
        val clearInput1 = createItem(
            Material.RED_STAINED_GLASS_PANE,
            "§cInput 1 löschen",
            emptyList()
        )
        inventory.setItem(19, clearInput1)

        // Clear input2
        val clearInput2 = createItem(
            Material.RED_STAINED_GLASS_PANE,
            "§cInput 2 löschen",
            emptyList()
        )
        inventory.setItem(21, clearInput2)

        // Clear output
        val clearOutput = createItem(
            Material.RED_STAINED_GLASS_PANE,
            "§cOutput löschen",
            emptyList()
        )
        inventory.setItem(25, clearOutput)

        // Save button - immer aktiv, da Trade bereits existiert
        val save = createItem(
            Material.EMERALD_BLOCK,
            "§aTrade speichern & schließen",
            listOf(
                "§7Speichert alle Änderungen",
                "§7und kehrt zurück zum Editor"
            )
        )
        inventory.setItem(49, save)

        // Cancel button
        val cancel = createItem(
            Material.BARRIER,
            "§cAbbrechen",
            listOf("§7Änderungen werden verworfen")
        )
        inventory.setItem(48, cancel)

        // Back button
        val back = createItem(
            Material.ARROW,
            "§eZurück",
            listOf("§7Zurück zum Trade Editor")
        )
        inventory.setItem(50, back)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory != inventory) return
        if (event.whoClicked != currentPlayer) return

        val player = event.whoClicked as Player
        val slot = event.slot
        val clickedItem = event.currentItem
        val cursorItem = event.cursor

        when (slot) {
            10 -> { // Input 1
                event.isCancelled = true
                if (event.isLeftClick) {
                    // Linksklick: Item aus Hand nehmen
                    val itemInHand = player.inventory.itemInMainHand
                    if (itemInHand.type != Material.AIR) {
                        input1 = itemStackToItemData(itemInHand)
                        setupInventory()
                        player.sendMessage(Component.text("Input 1 gesetzt!").color(NamedTextColor.GREEN))
                    }
                } else if (event.isRightClick) {
                    openItemTypeSelector(player, 1)
                }
            }
            12 -> { // Input 2
                event.isCancelled = true
                if (event.isLeftClick) {
                    // Linksklick: Item aus Hand nehmen
                    val itemInHand = player.inventory.itemInMainHand
                    if (itemInHand.type != Material.AIR) {
                        input2 = itemStackToItemData(itemInHand)
                        setupInventory()
                        player.sendMessage(Component.text("Input 2 gesetzt!").color(NamedTextColor.GREEN))
                    }
                } else if (event.isRightClick) {
                    openItemTypeSelector(player, 2)
                }
            }
            16 -> { // Output
                event.isCancelled = true
                if (event.isLeftClick) {
                    // Linksklick: Item aus Hand nehmen
                    val itemInHand = player.inventory.itemInMainHand
                    if (itemInHand.type != Material.AIR) {
                        output = itemStackToItemData(itemInHand)
                        setupInventory()
                        player.sendMessage(Component.text("Output gesetzt!").color(NamedTextColor.GREEN))
                    }
                } else if (event.isRightClick) {
                    openItemTypeSelector(player, 0)
                }
            }
            19 -> { // Clear input1
                event.isCancelled = true
                input1 = null
                setupInventory()
            }
            21 -> { // Clear input2
                event.isCancelled = true
                input2 = null
                setupInventory()
            }
            25 -> { // Clear output
                event.isCancelled = true
                output = null
                setupInventory()
            }
            28 -> { // PlayerPoints cost
                event.isCancelled = true
                when {
                    event.isShiftClick -> {
                        player.closeInventory()
                        player.sendMessage(Component.text("Gib die PlayerPoints-Kosten im Chat ein:").color(NamedTextColor.YELLOW))
                        waitingForInput = InputType.PLAYER_POINTS
                    }
                    event.isLeftClick -> {
                        playerPointsCost = (playerPointsCost + 10).coerceAtLeast(0)
                        setupInventory()
                    }
                    event.isRightClick -> {
                        playerPointsCost = (playerPointsCost - 10).coerceAtLeast(0)
                        setupInventory()
                    }
                }
            }
            30 -> { // Max uses
                event.isCancelled = true
                when {
                    event.isShiftClick -> {
                        player.closeInventory()
                        player.sendMessage(Component.text("Gib die maximale Anzahl an Verwendungen im Chat ein (-1 = Unlimited):").color(NamedTextColor.YELLOW))
                        waitingForInput = InputType.MAX_USES
                    }
                    event.isLeftClick -> {
                        maxUses = if (maxUses < 0) 1 else maxUses + 1
                        setupInventory()
                    }
                    event.isRightClick -> {
                        maxUses = (maxUses - 1).coerceAtLeast(-1)
                        setupInventory()
                    }
                }
            }
            48 -> { // Cancel
                event.isCancelled = true
                player.closeInventory()
            }
            49 -> { // Save
                event.isCancelled = true
                // Speichere den Trade (auch wenn unvollständig, da er bereits beim Öffnen erstellt wurde)
                if (editIndex != null) {
                    // Update existierenden Trade
                    trader.trades[editIndex] = TradeData(
                        input1 = input1 ?: ItemData(type = ItemData.ItemType.NONE),
                        input2 = input2,
                        output = output ?: ItemData(type = ItemData.ItemType.NONE),
                        maxUses = maxUses,
                        playerPointsCost = playerPointsCost
                    )
                    player.sendMessage(Component.text("Trade gespeichert!").color(NamedTextColor.GREEN))
                } else {
                    // Sollte nicht mehr passieren, da Trade bereits beim Öffnen erstellt wurde
                    player.sendMessage(Component.text("Fehler: Trade konnte nicht gespeichert werden!").color(NamedTextColor.RED))
                }

                plugin.traderManager.updateTrader(trader)
                player.closeInventory()

                // Reopen trade editor
                TradeEditorGUI(plugin, trader).open(player)
            }
            50 -> { // Back
                event.isCancelled = true
                player.closeInventory()
                TradeEditorGUI(plugin, trader).open(player)
            }
            else -> event.isCancelled = true
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        if (event.player != currentPlayer) return
        if (waitingForInput == null) return

        event.isCancelled = true

        val input = event.message

        when (waitingForInput) {
            InputType.PLAYER_POINTS -> {
                val value = input.toIntOrNull()
                if (value != null && value >= 0) {
                    playerPointsCost = value
                    event.player.sendMessage(Component.text("PlayerPoints-Kosten auf $value gesetzt!").color(NamedTextColor.GREEN))
                } else {
                    event.player.sendMessage(Component.text("Ungültige Eingabe!").color(NamedTextColor.RED))
                }
            }
            InputType.MAX_USES -> {
                val value = input.toIntOrNull()
                if (value != null && value >= -1) {
                    maxUses = value
                    event.player.sendMessage(Component.text("Max Uses auf $value gesetzt!").color(NamedTextColor.GREEN))
                } else {
                    event.player.sendMessage(Component.text("Ungültige Eingabe!").color(NamedTextColor.RED))
                }
            }
            null -> {}
        }

        waitingForInput = null

        // Speichere den aktuellen Trade-Stand
        if (editIndex != null) {
            trader.trades[editIndex] = TradeData(
                input1 = input1 ?: ItemData(type = ItemData.ItemType.NONE),
                input2 = input2,
                output = output ?: ItemData(type = ItemData.ItemType.NONE),
                maxUses = maxUses,
                playerPointsCost = playerPointsCost
            )
            plugin.traderManager.updateTrader(trader)
        }

        // Reopen inventory
        Bukkit.getScheduler().runTask(plugin, Runnable {
            open(event.player)
        })
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory) return
        if (event.player != currentPlayer) return

        if (waitingForInput == null) {
            HandlerList.unregisterAll(this)
        }
    }

    private fun openItemTypeSelector(player: Player, slot: Int) {
        // TODO: Implement item type selector (Vanilla, Nexo, None)
        player.sendMessage(Component.text("Item Type Selector kommt bald...").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("Nutze vorerst Items aus deinem Inventar!").color(NamedTextColor.GRAY))
    }

    private fun itemStackToItemData(item: ItemStack): ItemData {
        // Check if it's a Nexo item
        if (plugin.hasNexo) {
            try {
                val nexoItem = NexoItems.idFromItem(item)
                if (nexoItem != null) {
                    return ItemData(
                        type = ItemData.ItemType.NEXO,
                        nexoId = nexoItem,
                        amount = item.amount,
                        displayName = item.itemMeta.displayName()?.toString(),
                        lore = item.itemMeta.lore()?.map { it.toString() } ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                // Not a Nexo item, continue as vanilla
            }
        }

        // Vanilla item
        return ItemData(
            type = ItemData.ItemType.VANILLA,
            material = item.type.name,
            amount = item.amount,
            displayName = item.itemMeta.displayName()?.toString(),
            lore = item.itemMeta.lore()?.map { it.toString() } ?: emptyList()
        )
    }

    private fun getItemDescription(itemData: ItemData): String {
        return when (itemData.type) {
            ItemData.ItemType.VANILLA -> "${itemData.amount}x ${itemData.material}"
            ItemData.ItemType.NEXO -> "${itemData.amount}x Nexo:${itemData.nexoId}"
            ItemData.ItemType.NONE -> "Kein Item"
        }
    }

    private fun createItem(material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(Component.text(name))
        if (lore.isNotEmpty()) {
            meta.lore(lore.filter { it.isNotEmpty() }.map { Component.text(it) })
        }
        item.itemMeta = meta
        return item
    }
}

