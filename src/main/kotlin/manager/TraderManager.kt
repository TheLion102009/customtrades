package de.customtrades.manager

import de.customtrades.CustomTradesPlugin
import de.customtrades.data.TraderData
import de.customtrades.data.LocationData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import java.util.UUID

class TraderManager(private val plugin: CustomTradesPlugin) {

    private val traders = mutableMapOf<String, TraderData>()
    private val entityToTrader = mutableMapOf<UUID, String>()
    private val traderKey = NamespacedKey(plugin, "trader_name")
    private val spawnedTraders = mutableMapOf<String, UUID>() // Tracking für gespawnte Trader

    fun createTrader(name: String, entityType: EntityType, location: Location): Boolean {
        if (traders.containsKey(name)) {
            return false
        }

        val world = location.world ?: return false

        // Spawn entity
        val entityClass = entityType.entityClass
        if (entityClass == null || !LivingEntity::class.java.isAssignableFrom(entityClass)) {
            return false
        }
        @Suppress("UNCHECKED_CAST")
        val entity = world.spawn(location, entityClass as Class<out LivingEntity>)

        // Mark as trader
        entity.persistentDataContainer.set(traderKey, PersistentDataType.STRING, name)
        entity.isCustomNameVisible = true
        entity.customName(net.kyori.adventure.text.Component.text(name))
        entity.setAI(false) // Default: keine AI
        entity.isInvulnerable = true // Default: kann nicht getötet werden
        entity.setGravity(false)
        entity.setSilent(true)
        entity.setCanPickupItems(false)
        entity.setCollidable(false)
        entity.removeWhenFarAway = false // Despawnt nie

        // Create trader data
        val traderData = TraderData(
            name = name,
            mobType = entityType.name,
            location = LocationData(
                world = world.name,
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch
            ),
            displayName = name
        )

        traders[name] = traderData
        entityToTrader[entity.uniqueId] = name

        plugin.configManager.saveTrader(traderData)

        return true
    }

    fun getTrader(name: String): TraderData? {
        return traders[name]
    }

    fun getTraderByEntity(entity: Entity): TraderData? {
        val name = entity.persistentDataContainer.get(traderKey, PersistentDataType.STRING) ?: return null
        return traders[name]
    }

    fun removeTrader(name: String): Boolean {
        val trader = traders.remove(name) ?: return false

        // Remove entity
        entityToTrader.entries.removeIf { it.value == name }

        // Find and remove entity from world
        val world = Bukkit.getWorld(trader.location.world)
        world?.entities?.forEach { entity ->
            val traderName = entity.persistentDataContainer.get(traderKey, PersistentDataType.STRING)
            if (traderName == name) {
                entity.remove()
            }
        }

        // Entferne aus Tracking
        spawnedTraders.remove(name)

        plugin.configManager.deleteTrader(name)
        plugin.debugLog("Trader $name entfernt")
        return true
    }

    fun updateTrader(traderData: TraderData) {
        traders[traderData.name] = traderData
        plugin.configManager.saveTrader(traderData)

        // Update live entity wenn vorhanden
        val world = Bukkit.getWorld(traderData.location.world)
        world?.entities?.forEach { entity ->
            if (isTrader(entity)) {
                val name = entity.persistentDataContainer.get(traderKey, PersistentDataType.STRING)
                if (name == traderData.name && entity is LivingEntity) {
                    // Update AI
                    entity.setAI(traderData.hasAI)
                    // Update Invulnerable
                    entity.isInvulnerable = traderData.invulnerable
                    // Update Display Name
                    entity.customName(net.kyori.adventure.text.Component.text(traderData.displayName ?: traderData.name))
                }
            }
        }
    }

    fun loadAllTraders() {
        // Zuerst: Finde alle bereits existierenden Trader in der Welt
        // und füge sie zum Tracking hinzu (wichtig bei Reload!)
        plugin.server.worlds.forEach { world ->
            world.entities.forEach { entity ->
                val traderName = entity.persistentDataContainer.get(traderKey, PersistentDataType.STRING)
                if (traderName != null) {
                    // Trader bereits in Welt - füge zu Tracking hinzu
                    spawnedTraders[traderName] = entity.uniqueId
                    entityToTrader[entity.uniqueId] = traderName
                    plugin.debugLog("Existierender Trader gefunden: $traderName (UUID: ${entity.uniqueId})")
                }
            }
        }

        val traderNames = plugin.configManager.getAllTraderNames()

        traderNames.forEach { name ->
            val trader = plugin.configManager.loadTrader(name)
            if (trader != null) {
                traders[name] = trader

                if (trader.persistent) {
                    spawnTrader(trader)
                }
            }
        }

        plugin.logger.info("${traders.size} Trader geladen, ${spawnedTraders.size} in Welt gefunden.")
    }

    fun saveAllTraders() {
        traders.values.forEach { trader ->
            plugin.configManager.saveTrader(trader)
        }
    }

    private fun spawnTrader(trader: TraderData) {
        // Prüfe ob Trader bereits gespawnt ist
        val existingUUID = spawnedTraders[trader.name]
        if (existingUUID != null) {
            // Prüfe ob Entity noch existiert
            val existingEntity = Bukkit.getEntity(existingUUID)
            if (existingEntity != null && existingEntity.isValid) {
                plugin.debugLog("Trader ${trader.name} ist bereits gespawnt, überspringe...")
                return
            } else {
                // Entity existiert nicht mehr, entferne aus Tracking
                spawnedTraders.remove(trader.name)
                entityToTrader.remove(existingUUID)
                plugin.debugLog("Alter Trader ${trader.name} nicht mehr gefunden, spawne neu...")
            }
        }

        val world = Bukkit.getWorld(trader.location.world) ?: return
        val location = Location(
            world,
            trader.location.x,
            trader.location.y,
            trader.location.z,
            trader.location.yaw,
            trader.location.pitch
        )

        try {
            val entityType = EntityType.valueOf(trader.mobType)
            val entityClass = entityType.entityClass
            if (entityClass == null || !LivingEntity::class.java.isAssignableFrom(entityClass)) {
                return
            }
            @Suppress("UNCHECKED_CAST")
            val entity = world.spawn(location, entityClass as Class<out LivingEntity>)

            entity.persistentDataContainer.set(traderKey, PersistentDataType.STRING, trader.name)
            entity.isCustomNameVisible = true
            entity.customName(net.kyori.adventure.text.Component.text(trader.displayName ?: trader.name))
            entity.setAI(trader.hasAI) // AI basierend auf Einstellung
            entity.isInvulnerable = trader.invulnerable // Invulnerable basierend auf Einstellung
            entity.setGravity(false)
            entity.setSilent(true)
            entity.setCanPickupItems(false)
            entity.setCollidable(false)
            entity.removeWhenFarAway = false // Despawnt nie

            entityToTrader[entity.uniqueId] = trader.name
            spawnedTraders[trader.name] = entity.uniqueId // Tracking hinzufügen

            plugin.debugLog("Trader ${trader.name} erfolgreich gespawnt (UUID: ${entity.uniqueId})")
        } catch (e: Exception) {
            plugin.logger.warning("Konnte Trader ${trader.name} nicht spawnen: ${e.message}")
        }
    }

    fun getAllTraders(): Collection<TraderData> {
        return traders.values
    }

    fun isTrader(entity: Entity): Boolean {
        return entity.persistentDataContainer.has(traderKey, PersistentDataType.STRING)
    }
}

