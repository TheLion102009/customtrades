package de.customtrades.data

import kotlinx.serialization.Serializable

@Serializable
data class TraderData(
    val name: String,
    val mobType: String,
    val location: LocationData,
    val trades: MutableList<TradeData> = mutableListOf(),
    val displayName: String? = null,
    val persistent: Boolean = true,
    val hasAI: Boolean = false, // Kann Spieler anschauen/bewegen
    val invulnerable: Boolean = true // Kann nicht getötet werden
)

@Serializable
data class LocationData(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f
)

@Serializable
data class TradeData(
    val input1: ItemData,
    val input2: ItemData? = null,
    val output: ItemData,
    val maxUses: Int = -1, // -1 = unlimited
    val playerPointsCost: Int = 0 // 0 = keine PlayerPoints benötigt
)

@Serializable
data class ItemData(
    val type: ItemType,
    val material: String? = null, // für VANILLA type
    val nexoId: String? = null, // für NEXO type
    val amount: Int = 1,
    val displayName: String? = null,
    val lore: List<String> = emptyList()
) {
    enum class ItemType {
        VANILLA,
        NEXO,
        NONE // für PlayerPoints-only Trades
    }
}

