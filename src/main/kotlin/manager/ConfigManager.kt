package de.customtrades.manager

import de.customtrades.CustomTradesPlugin
import de.customtrades.data.TraderData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: CustomTradesPlugin) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val tradersFolder = File(plugin.dataFolder, "traders")

    init {
        if (!tradersFolder.exists()) {
            tradersFolder.mkdirs()
        }
    }

    fun saveTrader(trader: TraderData) {
        val file = File(tradersFolder, "${trader.name}.yml")
        val yaml = YamlConfiguration()

        yaml.set("name", trader.name)
        yaml.set("mobType", trader.mobType)
        yaml.set("displayName", trader.displayName)
        yaml.set("persistent", trader.persistent)
        yaml.set("hasAI", trader.hasAI)
        yaml.set("invulnerable", trader.invulnerable)

        // Location
        yaml.set("location.world", trader.location.world)
        yaml.set("location.x", trader.location.x)
        yaml.set("location.y", trader.location.y)
        yaml.set("location.z", trader.location.z)
        yaml.set("location.yaw", trader.location.yaw)
        yaml.set("location.pitch", trader.location.pitch)

        // Trades
        trader.trades.forEachIndexed { index, trade ->
            val path = "trades.$index"

            // Input 1
            yaml.set("$path.input1.type", trade.input1.type.name)
            yaml.set("$path.input1.material", trade.input1.material)
            yaml.set("$path.input1.nexoId", trade.input1.nexoId)
            yaml.set("$path.input1.amount", trade.input1.amount)
            yaml.set("$path.input1.displayName", trade.input1.displayName)
            yaml.set("$path.input1.lore", trade.input1.lore)

            // Input 2 (optional)
            trade.input2?.let { input2 ->
                yaml.set("$path.input2.type", input2.type.name)
                yaml.set("$path.input2.material", input2.material)
                yaml.set("$path.input2.nexoId", input2.nexoId)
                yaml.set("$path.input2.amount", input2.amount)
                yaml.set("$path.input2.displayName", input2.displayName)
                yaml.set("$path.input2.lore", input2.lore)
            }

            // Output
            yaml.set("$path.output.type", trade.output.type.name)
            yaml.set("$path.output.material", trade.output.material)
            yaml.set("$path.output.nexoId", trade.output.nexoId)
            yaml.set("$path.output.amount", trade.output.amount)
            yaml.set("$path.output.displayName", trade.output.displayName)
            yaml.set("$path.output.lore", trade.output.lore)

            yaml.set("$path.maxUses", trade.maxUses)
            yaml.set("$path.playerPointsCost", trade.playerPointsCost)
        }

        yaml.save(file)
        plugin.logger.info("Trader ${trader.name} wurde gespeichert.")
    }

    fun loadTrader(name: String): TraderData? {
        val file = File(tradersFolder, "$name.yml")
        if (!file.exists()) {
            return null
        }

        val yaml = YamlConfiguration.loadConfiguration(file)

        try {
            val traderName = yaml.getString("name") ?: return null
            val mobType = yaml.getString("mobType") ?: return null

            val location = de.customtrades.data.LocationData(
                world = yaml.getString("location.world") ?: return null,
                x = yaml.getDouble("location.x"),
                y = yaml.getDouble("location.y"),
                z = yaml.getDouble("location.z"),
                yaw = yaml.getDouble("location.yaw", 0.0).toFloat(),
                pitch = yaml.getDouble("location.pitch", 0.0).toFloat()
            )

            val trades = mutableListOf<de.customtrades.data.TradeData>()

            yaml.getConfigurationSection("trades")?.let { tradesSection ->
                tradesSection.getKeys(false).forEach { key ->
                    val path = "trades.$key"

                    val input1 = loadItemData(yaml, "$path.input1") ?: return@forEach
                    val input2 = loadItemData(yaml, "$path.input2")
                    val output = loadItemData(yaml, "$path.output") ?: return@forEach

                    trades.add(
                        de.customtrades.data.TradeData(
                            input1 = input1,
                            input2 = input2,
                            output = output,
                            maxUses = yaml.getInt("$path.maxUses", -1),
                            playerPointsCost = yaml.getInt("$path.playerPointsCost", 0)
                        )
                    )
                }
            }

            return TraderData(
                name = traderName,
                mobType = mobType,
                location = location,
                trades = trades,
                displayName = yaml.getString("displayName"),
                persistent = yaml.getBoolean("persistent", true),
                hasAI = yaml.getBoolean("hasAI", false),
                invulnerable = yaml.getBoolean("invulnerable", true)
            )
        } catch (e: Exception) {
            plugin.logger.severe("Fehler beim Laden von Trader $name: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun loadItemData(yaml: YamlConfiguration, path: String): de.customtrades.data.ItemData? {
        val typeString = yaml.getString("$path.type") ?: return null
        val type = try {
            de.customtrades.data.ItemData.ItemType.valueOf(typeString)
        } catch (e: IllegalArgumentException) {
            return null
        }

        return de.customtrades.data.ItemData(
            type = type,
            material = yaml.getString("$path.material"),
            nexoId = yaml.getString("$path.nexoId"),
            amount = yaml.getInt("$path.amount", 1),
            displayName = yaml.getString("$path.displayName"),
            lore = yaml.getStringList("$path.lore")
        )
    }

    fun getAllTraderNames(): List<String> {
        return tradersFolder.listFiles()
            ?.filter { it.extension == "yml" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    fun deleteTrader(name: String): Boolean {
        val file = File(tradersFolder, "$name.yml")
        return file.delete()
    }
}

