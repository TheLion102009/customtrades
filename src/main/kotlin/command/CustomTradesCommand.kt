package de.customtrades.command

import de.customtrades.CustomTradesPlugin
import de.customtrades.ui.TradeEditorGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class CustomTradesCommand(private val plugin: CustomTradesPlugin) : CommandExecutor, TabCompleter {

    private val allowedMobs = listOf(
        EntityType.VILLAGER,
        EntityType.WANDERING_TRADER,
        EntityType.ZOMBIE_VILLAGER
    )

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "edit" -> handleEdit(sender)
            "remove", "delete" -> handleRemove(sender, args)
            "list" -> handleList(sender)
            "reload" -> handleReload(sender)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleCreate(player: Player, args: Array<out String>) {
        if (!player.hasPermission("customtrades.create")) {
            player.sendMessage(Component.text("Du hast keine Berechtigung für diesen Befehl!").color(NamedTextColor.RED))
            return
        }

        if (args.size < 3) {
            player.sendMessage(Component.text("Verwendung: /ct create <Name> <Mob>").color(NamedTextColor.RED))
            return
        }

        val name = args[1]
        val mobTypeString = args[2].uppercase()

        val entityType = try {
            EntityType.valueOf(mobTypeString)
        } catch (e: IllegalArgumentException) {
            player.sendMessage(Component.text("Ungültiger Mob-Typ: $mobTypeString").color(NamedTextColor.RED))
            return
        }

        if (!allowedMobs.contains(entityType)) {
            player.sendMessage(Component.text("Dieser Mob-Typ kann nicht als Trader verwendet werden!").color(NamedTextColor.RED))
            player.sendMessage(Component.text("Erlaubte Typen: ${allowedMobs.joinToString()}").color(NamedTextColor.GRAY))
            return
        }

        if (plugin.traderManager.getTrader(name) != null) {
            player.sendMessage(Component.text("Ein Trader mit diesem Namen existiert bereits!").color(NamedTextColor.RED))
            return
        }

        val success = plugin.traderManager.createTrader(name, entityType, player.location)

        if (success) {
            player.sendMessage(Component.text("Trader '$name' wurde erfolgreich erstellt!").color(NamedTextColor.GREEN))
        } else {
            player.sendMessage(Component.text("Fehler beim Erstellen des Traders!").color(NamedTextColor.RED))
        }
    }

    private fun handleEdit(player: Player) {
        if (!player.hasPermission("customtrades.edit")) {
            player.sendMessage(Component.text("Du hast keine Berechtigung für diesen Befehl!").color(NamedTextColor.RED))
            return
        }

        // Nutze Raycast für bessere Entity-Erkennung
        val rayTraceResult = player.world.rayTraceEntities(
            player.eyeLocation,
            player.eyeLocation.direction,
            5.0,
            0.5
        ) { entity -> plugin.traderManager.isTrader(entity) }

        val targetEntity = rayTraceResult?.hitEntity

        if (targetEntity == null) {
            player.sendMessage(Component.text("Schaue einen Trader an, um ihn zu bearbeiten!").color(NamedTextColor.RED))
            return
        }

        val trader = plugin.traderManager.getTraderByEntity(targetEntity)

        if (trader == null) {
            player.sendMessage(Component.text("Dies ist kein Trader!").color(NamedTextColor.RED))
            return
        }

        TradeEditorGUI(plugin, trader).open(player)
    }

    private fun handleRemove(player: Player, args: Array<out String>) {
        if (!player.hasPermission("customtrades.remove")) {
            player.sendMessage(Component.text("Du hast keine Berechtigung für diesen Befehl!").color(NamedTextColor.RED))
            return
        }

        if (args.size < 2) {
            player.sendMessage(Component.text("Verwendung: /ct remove <Name>").color(NamedTextColor.RED))
            return
        }

        val name = args[1]
        val success = plugin.traderManager.removeTrader(name)

        if (success) {
            player.sendMessage(Component.text("Trader '$name' wurde entfernt!").color(NamedTextColor.GREEN))
        } else {
            player.sendMessage(Component.text("Trader '$name' wurde nicht gefunden!").color(NamedTextColor.RED))
        }
    }

    private fun handleList(player: Player) {
        if (!player.hasPermission("customtrades.list")) {
            player.sendMessage(Component.text("Du hast keine Berechtigung für diesen Befehl!").color(NamedTextColor.RED))
            return
        }

        val traders = plugin.traderManager.getAllTraders()

        if (traders.isEmpty()) {
            player.sendMessage(Component.text("Es existieren keine Trader!").color(NamedTextColor.YELLOW))
            return
        }

        player.sendMessage(Component.text("=== Trader Liste ===").color(NamedTextColor.GOLD))
        traders.forEach { trader ->
            player.sendMessage(
                Component.text("- ${trader.name} (${trader.mobType}) - ${trader.trades.size} Trades")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }

    private fun handleReload(player: Player) {
        if (!player.hasPermission("customtrades.reload")) {
            player.sendMessage(Component.text("Du hast keine Berechtigung für diesen Befehl!").color(NamedTextColor.RED))
            return
        }

        plugin.traderManager.saveAllTraders()
        plugin.traderManager.loadAllTraders()

        player.sendMessage(Component.text("CustomTrades wurde neu geladen!").color(NamedTextColor.GREEN))
    }

    private fun sendHelp(player: Player) {
        player.sendMessage(Component.text("=== CustomTrades Hilfe ===").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("/ct create <Name> <Mob> - Erstellt einen neuen Trader").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/ct edit - Bearbeitet den angeschauten Trader").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/ct remove <Name> - Entfernt einen Trader").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/ct list - Zeigt alle Trader an").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/ct reload - Lädt alle Trader neu").color(NamedTextColor.YELLOW))
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            return listOf("create", "edit", "remove", "list", "reload")
                .filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2 && args[0].equals("remove", ignoreCase = true)) {
            return plugin.traderManager.getAllTraders()
                .map { it.name }
                .filter { it.startsWith(args[1], ignoreCase = true) }
        }

        if (args.size == 3 && args[0].equals("create", ignoreCase = true)) {
            return allowedMobs.map { it.name.lowercase() }
                .filter { it.startsWith(args[2].lowercase()) }
        }

        return emptyList()
    }
}

