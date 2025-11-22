package de.customtrades

import de.customtrades.command.CustomTradesCommand
import de.customtrades.manager.TraderManager
import de.customtrades.manager.ConfigManager
import de.customtrades.listener.TraderInteractListener
import de.customtrades.listener.TradeListener
import org.bukkit.plugin.java.JavaPlugin

class CustomTradesPlugin : JavaPlugin() {

    lateinit var traderManager: TraderManager
        private set

    lateinit var configManager: ConfigManager
        private set

    lateinit var sunflowerListener: de.customtrades.listener.PlayerPointsSunflowerListener
        private set

    var hasPlayerPoints = false
        private set

    var hasNexo = false
        private set

    fun debugLog(message: String) {
        if (config.getBoolean("debug", false)) {
            logger.info("[DEBUG] $message")
        }
    }

    fun getCurrencyName(): String {
        return config.getString("currency-name", "PlayerPoints") ?: "PlayerPoints"
    }

    override fun onEnable() {
        // Create plugin data folder
        dataFolder.mkdirs()

        // Save default config
        saveDefaultConfig()

        // Log version and settings
        logger.info("CustomTrades v${description.version} wird geladen...")
        logger.info("Währung: ${config.getString("currency-name", "PlayerPoints")}")
        logger.info("Debug-Modus: ${config.getBoolean("debug", false)}")

        // Initialize managers
        configManager = ConfigManager(this)
        traderManager = TraderManager(this)

        // Check for soft dependencies
        hasPlayerPoints = server.pluginManager.getPlugin("PlayerPoints") != null
        hasNexo = server.pluginManager.getPlugin("Nexo") != null

        if (hasPlayerPoints) {
            logger.info("PlayerPoints gefunden! PlayerPoints-Support aktiviert.")
        } else {
            logger.warning("PlayerPoints nicht gefunden! PlayerPoints-Trades werden nicht funktionieren.")
        }

        if (hasNexo) {
            logger.info("Nexo gefunden! Custom-Items-Support aktiviert.")
        } else {
            logger.warning("Nexo nicht gefunden! Custom-Items werden nicht funktionieren.")
        }

        // Initialize sunflower listener
        sunflowerListener = de.customtrades.listener.PlayerPointsSunflowerListener(this)

        // Register commands
        getCommand("customtrades")?.setExecutor(CustomTradesCommand(this))

        // Register listeners
        server.pluginManager.registerEvents(TraderInteractListener(this), this)
        server.pluginManager.registerEvents(TradeListener(this), this)
        server.pluginManager.registerEvents(sunflowerListener, this)
        server.pluginManager.registerEvents(de.customtrades.listener.TraderProtectionListener(this), this)

        // Load all traders
        traderManager.loadAllTraders()

        logger.info("CustomTrades v${description.version} wurde aktiviert!")
    }

    override fun onDisable() {
        // Save all traders
        traderManager.saveAllTraders()

        logger.info("CustomTrades wurde deaktiviert!")
    }
}