package de.customtrades.util

import com.nexomc.nexo.api.NexoItems
import de.customtrades.CustomTradesPlugin
import de.customtrades.data.ItemData
import de.customtrades.data.TradeData
import de.customtrades.data.TraderData
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe

object TradeUtil {

    private fun buildPriceInfo(plugin: CustomTradesPlugin, trade: TradeData): String? {
        val parts = mutableListOf<String>()

        // Input 1
        if (trade.input1.type != ItemData.ItemType.NONE) {
            val desc = getItemDescription(trade.input1)
            parts.add("§7Preis: §e$desc")
        }

        // Input 2
        if (trade.input2 != null && trade.input2!!.type != ItemData.ItemType.NONE) {
            val desc = getItemDescription(trade.input2!!)
            parts.add("§7+ §e$desc")
        }

        // PlayerPoints
        if (trade.playerPointsCost > 0) {
            val currencyName = plugin.getCurrencyName()
            if (parts.isEmpty()) {
                parts.add("§7Preis: §6${trade.playerPointsCost} $currencyName")
            } else {
                parts.add("§7+ §6${trade.playerPointsCost} $currencyName")
            }
        }

        return if (parts.isNotEmpty()) parts.joinToString(" ") else null
    }

    private fun getItemDescription(itemData: ItemData): String {
        return when (itemData.type) {
            ItemData.ItemType.VANILLA -> {
                val material = itemData.material?.replace("_", " ")?.lowercase()
                    ?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                "${itemData.amount}x $material"
            }
            ItemData.ItemType.NEXO -> "${itemData.amount}x ${itemData.nexoId}"
            ItemData.ItemType.NONE -> "Nichts"
        }
    }

    /**
     * Entfernt die Preis-Lore von einem Item
     * Preis-Lore beginnt immer mit "§7Preis:" oder "§7+"
     */
    fun removePriceLore(item: ItemStack): ItemStack {
        val meta = item.itemMeta ?: return item
        val lore = meta.lore() ?: return item

        // Finde Index wo Preis-Lore beginnt
        val priceStartIndex = lore.indexOfFirst { component ->
            val text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component)
            text.startsWith("Preis:") || text.startsWith("+")
        }

        if (priceStartIndex != -1) {
            // Entferne Preis-Lore (und die Leerzeile davor falls vorhanden)
            val newLore = if (priceStartIndex > 0 &&
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(lore[priceStartIndex - 1]).isBlank()) {
                // Entferne auch die Leerzeile vor dem Preis
                lore.subList(0, priceStartIndex - 1)
            } else {
                lore.subList(0, priceStartIndex)
            }

            meta.lore(newLore)
            item.itemMeta = meta
        }

        return item
    }

    fun createMerchantRecipes(plugin: CustomTradesPlugin, trader: TraderData): List<MerchantRecipe> {
        return trader.trades.mapNotNull { trade ->
            createSingleMerchantRecipe(plugin, trade)
        }
    }

    fun createSingleMerchantRecipe(plugin: CustomTradesPlugin, trade: TradeData): MerchantRecipe? {
        // Überspringe Trades ohne gültiges Output
        if (trade.output.type == ItemData.ItemType.NONE) {
            return null
        }

        // Erstelle Preis-Info-String für Anzeige
        val priceInfo = buildPriceInfo(plugin, trade)

        // Erstelle Output MIT Preis-Lore (für Anzeige im GUI)
        val output = createItemStack(plugin, trade.output, priceInfo) ?: return null

        val recipe = MerchantRecipe(output, if (trade.maxUses > 0) trade.maxUses else Int.MAX_VALUE)
        recipe.priceMultiplier = 0f

        // Input 1
        val input1 = createItemStack(plugin, trade.input1)
        if (input1 != null) {
            recipe.addIngredient(input1)
        } else if (trade.playerPointsCost > 0) {
            // If no input1 and has PlayerPoints cost, use a placeholder
            val placeholder = createPlayerPointsPlaceholder(plugin, trade.playerPointsCost)
            recipe.addIngredient(placeholder)
        } else {
            // Kein Input und keine PlayerPoints - invalider Trade
            return null
        }

        // Input 2 (optional)
        trade.input2?.let { input2Data ->
            if (input2Data.type != ItemData.ItemType.NONE) {
                val input2 = createItemStack(plugin, input2Data)
                if (input2 != null) {
                    recipe.addIngredient(input2)
                }
            }
        }

        return recipe
    }

    fun createItemStack(plugin: CustomTradesPlugin, itemData: ItemData, priceInfo: String? = null): ItemStack? {
        return when (itemData.type) {
            ItemData.ItemType.VANILLA -> {
                val material = itemData.material?.let {
                    try {
                        Material.valueOf(it.uppercase())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                } ?: return null

                val item = ItemStack(material, itemData.amount)
                val meta = item.itemMeta

                itemData.displayName?.let {
                    meta.displayName(Component.text(it))
                }

                // Kombiniere original Lore mit Preis-Info
                val loreList = mutableListOf<Component>()
                if (itemData.lore.isNotEmpty()) {
                    loreList.addAll(itemData.lore.map { Component.text(it) })
                }
                if (priceInfo != null) {
                    if (loreList.isNotEmpty()) {
                        loreList.add(Component.text("")) // Leerzeile
                    }
                    loreList.add(Component.text(priceInfo))
                }
                if (loreList.isNotEmpty()) {
                    meta.lore(loreList)
                }

                item.itemMeta = meta
                item
            }

            ItemData.ItemType.NEXO -> {
                if (!plugin.hasNexo) {
                    plugin.logger.warning("Nexo-Item '${itemData.nexoId}' konnte nicht erstellt werden - Nexo ist nicht installiert!")
                    return null
                }

                val nexoId = itemData.nexoId ?: return null

                try {
                    val nexoItem = NexoItems.itemFromId(nexoId)
                    if (nexoItem != null) {
                        val item = nexoItem.build()
                        item.amount = itemData.amount

                        // Füge Preis-Info zur Lore hinzu
                        if (priceInfo != null) {
                            val meta = item.itemMeta
                            val lore = meta.lore()?.toMutableList() ?: mutableListOf()
                            lore.add(Component.text(""))
                            lore.add(Component.text(priceInfo))
                            meta.lore(lore)
                            item.itemMeta = meta
                        }

                        item
                    } else {
                        plugin.logger.warning("Nexo-Item '$nexoId' existiert nicht!")
                        null
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Fehler beim Erstellen von Nexo-Item '$nexoId': ${e.message}")
                    null
                }
            }

            ItemData.ItemType.NONE -> null
        }
    }

    private fun createPlayerPointsPlaceholder(plugin: CustomTradesPlugin, cost: Int): ItemStack {
        val currencyName = plugin.getCurrencyName()
        val item = ItemStack(Material.SUNFLOWER, 1)
        val meta = item.itemMeta

        // EXAKT die gleichen Properties wie die echte Sunflower!
        meta.displayName(Component.text("§6§l$currencyName Währung"))
        meta.lore(listOf(
            Component.text("§7Diese Sonnenblume repräsentiert"),
            Component.text("§7deine $currencyName für diesen Trade"),
            Component.text(""),
            Component.text("§c§lKann nicht bewegt oder weggeworfen werden!")
        ))

        // Wichtig: Den gleichen NBT-Tag wie die echte Sunflower!
        meta.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "pp_sunflower"),
            org.bukkit.persistence.PersistentDataType.BYTE,
            1
        )

        item.itemMeta = meta
        return item
    }

    fun parseEntityType(entityTypeName: String): org.bukkit.entity.EntityType? {
        return try {
            org.bukkit.entity.EntityType.valueOf(entityTypeName.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

