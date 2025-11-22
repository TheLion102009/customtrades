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

        val output = createItemStack(plugin, trade.output) ?: return null

        val recipe = MerchantRecipe(output, if (trade.maxUses > 0) trade.maxUses else Int.MAX_VALUE)

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

    fun createItemStack(plugin: CustomTradesPlugin, itemData: ItemData): ItemStack? {
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

                if (itemData.lore.isNotEmpty()) {
                    meta.lore(itemData.lore.map { Component.text(it) })
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
        val item = ItemStack(Material.SUNFLOWER, 1)
        val meta = item.itemMeta

        // EXAKT die gleichen Properties wie die echte Sunflower!
        meta.displayName(Component.text("§6§lPlayerPoints Währung"))
        meta.lore(listOf(
            Component.text("§7Diese Sonnenblume repräsentiert"),
            Component.text("§7deine PlayerPoints für diesen Trade"),
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

