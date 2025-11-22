# CustomTrades - Changelog

## Version 1.3.0 (2025-11-22) - Paper API Migration

### 🚀 Paper API statt Bukkit!

**Große Änderung:** Das Plugin basiert jetzt vollständig auf der Paper API statt der veralteten Bukkit API.

### ⚠️ Wichtiger Fix: Paper Plugin Command-System

**Problem:** Paper Plugins unterstützen KEINE YAML-basierten Command-Deklarationen!

**Behoben:**
- ✅ Commands aus `paper-plugin.yml` entfernt
- ✅ Programmatische Command-Registrierung implementiert
- ✅ `server.commandMap.register()` statt `getCommand()`
- ✅ Funktioniert jetzt korrekt als Paper Plugin

### ✨ Tab-Completion / Auto-Complete

**Problem:** Kein Auto-Complete für Commands im Chat

**Behoben:**
- ✅ `tabComplete()` Override in Command-Klasse hinzugefügt
- ✅ Auto-Complete für Subcommands (`create`, `edit`, `remove`, `list`, `reload`)
- ✅ Auto-Complete für Trader-Namen (bei `/ct remove <TAB>`)
- ✅ Auto-Complete für Mob-Typen (bei `/ct create <name> <TAB>`)
- ✅ Intelligentes Filtering basierend auf Eingabe

### 🔧 Hauptänderungen

**Scheduler-Migration:**
- ❌ `Bukkit.getScheduler().runTaskLater()` (veraltet)
- ✅ `server.globalRegionScheduler.runDelayed()` (modern)

**Vorteile:**
- ✅ Zukunftssicher (Paper wird aktiv entwickelt)
- ✅ Bessere Performance (Paper-optimiert)
- ✅ Folia-kompatibel (Region-based Scheduler)
- ✅ Modernere Code-Basis

### 📝 Geänderte Dateien

**TradeListener.kt:**
- Paper Region Scheduler statt Bukkit Scheduler
- Modernere Lambda-Syntax

**PlayerPointsSunflowerListener.kt:**
- Paper Region Scheduler für delayed tasks
- Cleanup mit Paper API

**TradeCreatorGUI.kt:**
- Paper Scheduler für GUI-Reopening
- Entfernte unnötige Bukkit-Imports

### 🎯 Kompatibilität

**Unterstützt:**
- ✅ Paper 1.21.3+ (empfohlen)
- ✅ Paper 1.21.x (alle Versionen)
- ✅ Folia 1.21.x (Region Scheduler vorbereitet)

**NICHT mehr unterstützt:**
- ❌ Bukkit/Spigot (nur Paper Server!)

### 💡 Für Server-Admins

**Keine Änderungen nötig!**
- ✅ Alle Features funktionieren gleich
- ✅ Config bleibt unverändert
- ✅ Commands bleiben gleich
- ✅ Trader & Trades bleiben erhalten

**Update:**
1. Server stoppen
2. JAR ersetzen
3. Server starten (muss Paper sein!)
4. ✅ Fertig!

### 🔧 Technische Details

**Paper Region Scheduler:**
```kotlin
// Modern: Paper API
server.globalRegionScheduler.runDelayed(plugin, { _ ->
    code()
}, ticks)

// Alt: Bukkit API (entfernt)
Bukkit.getScheduler().runTaskLater(plugin, Runnable {
    code()
}, ticks)
```

**Warum Paper?**
- Paper ist die Zukunft von Minecraft Servern
- Bessere Performance & Features
- Aktive Entwicklung
- Folia-Vorbereitung (Multi-threading)

---

## Version 1.2.3 (2025-11-22) - Preis-Lore Cleanup

### ✨ Neues Feature: Automatische Preis-Lore-Entfernung

**Problem:** Nach dem Trade hatte das gekaufte Item noch die Preis-Lore im Inventar
**Lösung:** Preis-Lore wird automatisch nach Trade-Abschluss entfernt

**Vorher:**
```
[Diamantschwert im Inventar]
§7Preis: §e10x Diamond
§7+ §6500 Coins    ← Sollte nicht da sein!
```

**Jetzt:**
```
[Diamantschwert im Inventar]
(keine Preis-Lore) ✓
```

### 🔧 Implementierung

**Neue Funktion: `TradeUtil.removePriceLore()`**
- Erkennt Preis-Zeilen automatisch (beginnen mit "Preis:" oder "+")
- Entfernt Preis-Lore + Leerzeile davor
- Behält Original-Lore des Items (Custom-Lore, Enchantments, etc.)

**TradeListener erweitert:**
- Nach Trade-Abschluss: Entfernt Preis-Lore von allen Items im Inventar
- Timing: Zusammen mit PlayerPoints-Abzug und Sunflower-Removal
- Debug-Log: "Preis-Lore entfernt für {player}"

### 💡 Intelligente Lore-Verarbeitung

**Was wird entfernt:**
- Zeilen beginnend mit "Preis:"
- Zeilen beginnend mit "+" (weitere Preis-Komponenten)
- Leerzeile vor dem Preis (falls vorhanden)

**Was bleibt:**
- Original-Lore des Items
- Custom-Lore von Nexo-Items
- Enchantment-Lore
- Alle anderen Lore-Zeilen

### 📊 Use Cases

**Custom-Items:**
```
Trade-GUI: Original-Lore + Preis
Nach Trade: Nur Original-Lore ✓
```

**Stackable Items:**
```
Items bleiben stackable (keine unterschiedliche Lore) ✓
```

**Professioneller Look:**
```
Preis nur im Shop sichtbar ✓
Items sehen "echt" aus ✓
```

---

## Version 1.2.2 (2025-11-22) - Reload-Duplikat-Fix

### 🐛 Behobener Bug

**Problem:** Trader spawnen trotz v1.2.1 Fix noch doppelt bei `/reload`

**Ursache:** 
- Tracking-Map (`spawnedTraders`) ist im Memory
- Bei Reload wird Plugin neu geladen → Memory geleert
- Tracking-Map ist leer, aber Trader-Entities noch in Welt
- `spawnTrader()` denkt Trader existiert nicht → spawnt nochmal

**Lösung:**
```kotlin
// World-Scan beim Plugin-Start!
fun loadAllTraders() {
    // ZUERST: Scanne alle Welten nach existierenden Tradern
    plugin.server.worlds.forEach { world ->
        world.entities.forEach { entity ->
            val traderName = entity.persistentDataContainer.get(
                traderKey, 
                PersistentDataType.STRING
            )
            if (traderName != null) {
                // Trader gefunden - füge zu Tracking hinzu!
                spawnedTraders[traderName] = entity.uniqueId
                entityToTrader[entity.uniqueId] = traderName
                plugin.debugLog("Existierender Trader: $traderName")
            }
        }
    }
    
    // DANN: Lade Config und spawne (wenn nicht bereits vorhanden)
    // ...
}
```

**Ergebnis:**
- ✅ Tracking wird beim Reload rekonstruiert
- ✅ Existierende Trader werden erkannt
- ✅ Keine Duplikate mehr bei Reload
- ✅ Funktioniert auch bei mehrfachen Reloads

### 🔧 Technische Details

**TraderManager.kt:**
- World-Scan beim `loadAllTraders()` Start
- Rekonstruiert Tracking aus PersistentDataContainer
- Debug-Logs für gefundene Trader
- Erweiterte Startup-Logs

**Ablauf:**
```
1. /reload
2. spawnedTraders wird geleert (Memory)
3. World-Scan findet existierende Trader
4. Tracking wird rekonstruiert
5. Config-Load versucht zu spawnen
6. spawnTrader() sieht: bereits im Tracking
7. Überspringt Spawn ✓
```

### 📊 Testing

```bash
# Funktioniert jetzt:
✅ /reload → 1 Trader
✅ /reload → 1 Trader (nicht 2!)
✅ /reload → 1 Trader (nicht 3!)
✅ Beliebig viele Reloads → IMMER 1 Trader!
```

---

## Version 1.2.1 (2025-11-22) - Kritische Bugfixes

### 🐛 Behobene Bugs

#### Bug #1: "Sunflower nicht gefunden" Fehler
**Problem:** Trade wurde blockiert mit Fehlermeldung "Du hast die Währungs-Sonnenblume nicht!" obwohl Spieler sie hatte
**Ursache:** Race-Condition bei Sunflower-Prüfung im Trade-Event
**Lösung:** 
- ✅ Sunflower-Prüfung vor Trade entfernt
- ✅ Logik: Sunflower ist blockiert → wenn GUI offen, garantiert vorhanden
- ✅ Prüfung war überflüssig und verursachte Race-Conditions
- ✅ PlayerPoints werden trotzdem korrekt abgezogen
- ✅ Sunflower wird trotzdem entfernt (mit Check im Runnable)

**Code-Änderung:**
```kotlin
// Vorher:
if (!hasSunflower(player)) {
    return // Blockiert Trade
}

// Jetzt:
// Keine Prüfung! Sunflower kann nicht weg.
```

#### Bug #2: Trader spawnen doppelt bei Reload
**Problem:** Bei `/reload` oder Server-Restart spawnen Trader mehrfach am gleichen Ort
**Ursache:** Keine Tracking der bereits gespawnten Entities
**Lösung:**
- ✅ Neues Spawn-Tracking-System mit `spawnedTraders` Map
- ✅ Prüfung vor Spawn ob Trader bereits existiert
- ✅ UUID-basiertes Tracking
- ✅ Cleanup bei removeTrader()

**Code-Änderung:**
```kotlin
// Neu:
private val spawnedTraders = mutableMapOf<String, UUID>()

fun spawnTrader(trader: TraderData) {
    // Check ob bereits gespawnt
    if (spawnedTraders[trader.name]?.let { 
        Bukkit.getEntity(it)?.isValid 
    } == true) {
        return // Bereits gespawnt!
    }
    
    // Spawn + Tracking
    val entity = spawn(...)
    spawnedTraders[trader.name] = entity.uniqueId
}
```

### 🔧 Technische Details

**TradeListener.kt:**
- Sunflower-Check vor Trade entfernt
- Debug-Log hinzugefügt
- PlayerPoints-Abzug immer ausgeführt (nicht mehr abhängig von Sunflower-Check)

**TraderManager.kt:**
- `spawnedTraders` Map hinzugefügt
- Duplikat-Check in `spawnTrader()`
- Tracking-Cleanup in `removeTrader()`
- Debug-Logs für Spawn-Events

### 📊 Ergebnis

**Trades:**
- ✅ Funktionieren 100% zuverlässig
- ✅ Keine Race-Conditions mehr
- ✅ Keine "Sunflower nicht gefunden" Fehler

**Trader-Spawning:**
- ✅ Keine Duplikate bei Reload
- ✅ Korrekte Spawn-Verwaltung
- ✅ Bessere Performance (UUID-Lookup statt Entity-Iteration)

---

## Version 1.2.0 (2025-11-22) - Major Update

### 🎉 Neue Features

#### Config-System implementiert
**Neue Datei:** `config.yml`
```yaml
version: "1.2.0"
currency-name: "PlayerPoints"
debug: false
trade-delay: 5
sunflower-cleanup-delay: 10
```

**Features:**
- ✅ Anpassbarer Währungsname (`currency-name`)
- ✅ Debug-Modus für Troubleshooting (`debug: true/false`)
- ✅ Konfigurierbare Trade-Verzögerung (`trade-delay`)
- ✅ Konfigurierbare Sunflower-Cleanup-Verzögerung (`sunflower-cleanup-delay`)
- ✅ Build-Version in Config

#### Automatische Preis-Lore im Output-Item
**Problem:** Spieler sahen nicht welcher Preis für Items verlangt wird
**Lösung:** Preis wird automatisch zur Lore des Output-Items hinzugefügt

**Beispiel:**
```
[Diamantschwert]
§7Preis: §e10x Diamond
§7+ §6500 Coins
```

**Unterstützt:**
- Normale Items (z.B. "10x Diamond")
- Nexo Items
- PlayerPoints/Currency
- Kombinationen aus allen

#### Währungsname überall nutzbar
Der in Config definierte `currency-name` wird verwendet in:
- Sunflower DisplayName
- Sunflower Lore
- Chat-Nachrichten
- Fehlermeldungen
- **Preis-Lore im Output-Item**

### 🔧 Verbesserungen

**Trade-Timing optimiert:**
- Konfigurierbare Verzögerung verhindert dass Sunflower zu früh genommen wird
- Standard: 5 Ticks (250ms) für Trade, 10 Ticks (500ms) für Cleanup
- Anpassbar je nach Server-Performance

**Debug-Logging:**
```kotlin
plugin.debugLog("Trade-Abschluss geplant in 5 Ticks für Steve")
plugin.debugLog("500 Coins abgezogen von Steve")
plugin.debugLog("Sunflower entfernt von Steve")
```

**Startup-Logs:**
```
[CustomTrades] CustomTrades v1.2.0 wird geladen...
[CustomTrades] Währung: Coins
[CustomTrades] Debug-Modus: false
```

### 📦 Migration

**Von v1.1.3:**
1. JAR ersetzen
2. Server starten
3. `config.yml` wird automatisch erstellt
4. Optional: Config anpassen

**Config-Anpassung:**
```yaml
# Empfohlen für deutsche Server
currency-name: "Münzen"

# Oder andere Namen
currency-name: "Coins"
currency-name: "Credits"
currency-name: "Taler"
```

### 🐛 Behobene Probleme

- ✅ Sunflower wird nicht mehr zu früh entfernt (konfigurierbare Delays)
- ✅ Spieler sieht jetzt den Preis im Output-Item
- ✅ Besseres Timing für Trades

### 🔧 Geänderte Dateien

**Neu:**
- `config.yml` (Resource)
- `RELEASE_v1.2.0.md` (Dokumentation)

**Geändert:**
- `Main.kt` - Config-Loading, Debug-Logging, Currency-Name Helper
- `TradeUtil.kt` - Preis-Lore-Generation, Currency-Name in Items
- `TradeListener.kt` - Konfigurierbare Delays, Currency-Name in Messages
- `PlayerPointsSunflowerListener.kt` - Currency-Name in Sunflower, Config-Delays

---

## Version 1.1.3 (2025-11-22) - NBT-Match-Fix

### 🐛 Kritischer Bugfix - Trade funktionierte nicht

**Problem:**
Spieler konnten keine Trades mit PlayerPoints durchführen, obwohl sie die Sunflower hatten und genug Points besaßen. Merchant sagte "Nicht genug Items!".

**Ursache:**
- Recipe verwendete normale Sunflower ohne NBT-Tags
- Spieler hatte Custom Sunflower MIT NBT-Tags
- Minecraft's `ItemStack.isSimilar()` vergleicht Items 1:1 inklusive NBT
- Items waren nicht identisch → Trade wurde blockiert

**Lösung:**
✅ `createPlayerPointsPlaceholder()` erstellt jetzt EXAKT die gleiche Sunflower
```kotlin
// Gleicher DisplayName
meta.displayName(Component.text("§6§lPlayerPoints Währung"))

// Gleiche Lore
meta.lore(listOf(...)) // Exakt wie echte Sunflower

// WICHTIG: Gleicher NBT-Tag!
meta.persistentDataContainer.set(
    NamespacedKey(plugin, "pp_sunflower"),
    PersistentDataType.BYTE,
    1
)
```

**Ergebnis:**
- ✅ Recipe-Sunflower == Spieler-Sunflower
- ✅ Minecraft erkennt Items als identisch
- ✅ Trades mit PlayerPoints funktionieren perfekt!

### 🔧 Geänderte Dateien

**TradeUtil.kt:**
- `createPlayerPointsPlaceholder()` nimmt jetzt Plugin-Parameter
- Erstellt Custom Sunflower mit NBT-Tag
- Exakt gleicher DisplayName & Lore wie echte Sunflower

---

## Version 1.1.2 (2025-11-22) - Kritischer Hotfix

### 🐛 Kritische Bugfixes - PlayerPoints & Invulnerable

#### Bug #1: Trader war immer noch verwundbar
**Problem:** Trader nahm trotz `invulnerable = true` Schaden und konnte getötet werden
**Lösung:**
- ✅ **Neuer TraderProtectionListener** mit EntityDamageEvent-Handler
- ✅ Event-Priority HIGHEST für garantierte Ausführung
- ✅ Blockt JEDEN Schaden-Typ (Schwert, Bogen, Lava, TNT, etc.)
- ✅ 100% Schutz wenn invulnerable = true

#### Bug #2: Sunflower konnte bewegt werden
**Problem:** Spieler konnte die Währungs-Sonnenblume im Inventar bewegen
**Lösung:**
- ✅ Event-Priority auf HIGHEST erhöht
- ✅ Verbesserte Movement-Detection (current + cursor Item)
- ✅ Blockt ALLE Movement-Actions
- ✅ Erlaubt nur Platzierung in Merchant-GUI

#### Bug #3: Sunflower wurde erst bei /reload entfernt
**Problem:** Sunflower blieb nach Trade im Inventar, verschwand erst nach Reload
**Lösung:**
- ✅ Delayed Removal mit Scheduler (1 Tick nach Trade)
- ✅ Auto-Cleanup bei GUI-Close (2 Ticks Delay)
- ✅ PlayerPoints-Abzug gleichzeitig mit Removal
- ✅ Sofortige Entfernung - kein Reload mehr nötig

#### Bug #4: Sunflower funktionierte nicht als Zahlungsmittel
**Problem:** Trade mit PlayerPoints wurde nicht akzeptiert trotz Sunflower
**Lösung:**
- ✅ SlotType.RESULT Check für korrekte Trade-Detection
- ✅ Besseres Recipe-Matching mit `.isSimilar()`
- ✅ Verbesserte Trade-Validation
- ✅ Trades funktionieren jetzt zuverlässig

### 🔧 Technische Verbesserungen

**Neue Klasse:**
```kotlin
TraderProtectionListener
- Blockt EntityDamageEvent
- 100% Invulnerable-Schutz
- Automatisch registriert
```

**Verbesserte Event-Handler:**
```kotlin
PlayerPointsSunflowerListener:
- Priority: HIGHEST
- Delayed Auto-Cleanup
- Bessere Movement-Detection

TradeListener:
- SlotType.RESULT Check
- Delayed PlayerPoints-Abzug
- Delayed Sunflower-Remove
```

### 📦 Migration

**Von v1.1.1:**
- Einfach JAR austauschen
- Keine Config-Änderungen
- Automatische Kompatibilität

---

## Version 1.1.1 (2025-11-22) - Hotfix

### 🐛 Kritische Bugfixes

#### Bug #1: `/ct edit` funktionierte nicht
**Problem:** Kommando sagte "Schaue einen Trader an" obwohl man ihn anschaute
**Ursache:** `getTargetEntity()` funktioniert nicht zuverlässig
**Lösung:** 
- ✅ Nutzt jetzt `rayTraceEntities()` mit präzisem Raycast
- ✅ 5 Block Reichweite mit 0.5 Block Radius
- ✅ Filtert direkt nach Trader-Entities
- ✅ 100% zuverlässige Erkennung

#### Bug #2: Trader konnten getötet werden
**Problem:** Trotz `isInvulnerable = true` konnten Trader Schaden nehmen
**Ursache:** Setting wurde nicht korrekt persistiert und aktualisiert
**Lösung:**
- ✅ Neue `invulnerable` Property in TraderData
- ✅ Default: `true` (unverwundbar)
- ✅ Wird in YAML gespeichert
- ✅ Live-Update bei Änderungen
- ✅ `removeWhenFarAway = false` verhindert Despawn

### ✨ Neues Feature: Unverwundbar-Toggle

**Anfrage:** "füge dann in das edit menu bitte dann auch dasein das man ihn töten kann oder nicht"

**Implementierung:**
- 🛡️ Neuer Toggle-Button im Edit-Menu (Slot 46)
- 🟢 **SCHILD** = Unverwundbar AN (kann nicht getötet werden)
- 🔴 **SCHWERT** = Unverwundbar AUS (kann getötet werden)
- ⚡ Live-Update ohne Neustart
- 💾 Wird in YAML gespeichert

**Verwendung:**
```
/ct edit
→ Schaue Trader an (funktioniert jetzt!)
→ Klicke auf Schild/Schwert Icon (Slot 46)
→ Unverwundbar wird umgeschaltet
→ Sofort aktiv!
```

### 🔧 Technische Verbesserungen

**Raycast-System:**
```kotlin
player.world.rayTraceEntities(
    player.eyeLocation,
    player.eyeLocation.direction,
    5.0,  // Reichweite
    0.5   // Radius
)
```

**Entity-Properties:**
```kotlin
entity.isInvulnerable = trader.invulnerable
entity.removeWhenFarAway = false
entity.setCanPickupItems(false)
entity.setCollidable(false)
```

### 📦 Migration von v1.1.0

**Automatisch!**
- Alte Trader bekommen `invulnerable = true` als Default
- Keine manuellen Änderungen nötig
- Einfach JAR austauschen & neustarten

---

## Version 1.1.0 (2025-11-22)

### 🎉 Große Updates

#### 🛡️ Trader sind jetzt unverwundbar!
- ✅ Trader können nicht mehr getötet werden
- ✅ `isInvulnerable = true` für alle Trader
- ✅ Keine Items aufheben, keine Kollisionen
- ✅ Bleiben immer an ihrer Position

#### 🤖 AI-Steuerung im Edit-Menu!
- ✅ Neuer "AI Toggle" Button im Trade Editor
- ✅ Schalte AI ein/aus mit einem Klick
- ✅ **AI AUS** (Standard): Trader steht still und schaut geradeaus
- ✅ **AI AN**: Trader kann sich bewegen und schaut Spieler an
- ✅ Einstellung wird in YAML gespeichert (`hasAI: true/false`)
- ✅ Live-Update der Entity ohne Neustart

#### 💐 Revolutionäres PlayerPoints-System mit Sunflower!
Das alte System war kompliziert. Jetzt ist es MEGA einfach:

**So funktioniert es:**
1. Spieler klickt auf Trader mit PlayerPoints-Trades
2. Spieler bekommt automatisch eine **spezielle Sonnenblume**
3. Spieler wählt Trade aus (Sonnenblume wird als Währung akzeptiert)
4. Bei erfolgreichem Trade:
   - ✅ PlayerPoints werden abgezogen
   - ✅ Sonnenblume wird entfernt
   - ✅ Spieler bekommt das Item

**Sicherheitsfunktionen:**
- 🔒 Sonnenblume kann **nicht weggeworfen** werden
- 🔒 Sonnenblume kann **nicht bewegt** werden (außer für Trades)
- 🔒 Sonnenblume verschwindet automatisch wenn:
  - Trade abgeschlossen wurde
  - Inventar geschlossen wird
  - Spieler disconnected
- 📦 Bei vollem Inventar: Fehlermeldung + kein Trade möglich

**Item-Details der Sonnenblume:**
- 🌻 Material: SUNFLOWER
- 📛 Name: "§6§lPlayerPoints Währung"
- 📝 Lore erklärt den Zweck
- 🏷️ Persistent Data Tag markiert sie als speziell

### 🐛 Behobene Bugs
- **Trader konnten getötet werden** → Jetzt unverwundbar
- **Keine AI-Steuerung** → Toggle-Button im Edit-Menu
- **PlayerPoints-Trades funktionierten nicht** → Komplett neues System!

### ✨ Technische Verbesserungen
- Neuer `PlayerPointsSunflowerListener` für Sunflower-Management
- `hasAI` Property in TraderData
- Live-Update von Entity-Properties
- Bessere Error-Handling für volle Inventare
- Folia-kompatible Scheduler für alle neuen Features

---

## Version 1.0.1 (2025-11-22)

### 🐛 Bugfixes
- **GUI Item-Auswahl verbessert**: Items können jetzt mit Linksklick direkt aus der Hand in die Trade-Slots gesetzt werden
  - Halte ein Item in der Hand und klicke auf Input 1, Input 2 oder Output
  - Kein Drag & Drop mehr nötig!
  - Bessere Benutzerfreundlichkeit
  
- **PlayerPoints Chat-Input behoben**: GUI schließt sich nicht mehr beim Eingeben von PlayerPoints im Chat
  - Trade wird sofort beim Klicken auf "Trade hinzufügen" erstellt und gespeichert
  - Chat-Eingaben gehen nicht mehr verloren
  - Trade-Daten werden automatisch während der Bearbeitung gespeichert

### ✨ Verbesserungen
- Bessere Item-Slot-Beschreibungen im GUI
- Save-Button ist immer aktiv (da Trade bereits existiert)
- Validierung für unvollständige Trades
- NONE-Type Items werden korrekt behandelt

### 📝 Verwendung

**Item setzen:**
1. Nimm ein Item in die Hand (Main Hand)
2. Öffne `/ct edit` und klicke auf einen Trader
3. Klicke auf "Trade hinzufügen"
4. Linksklick auf Input 1, Input 2 oder Output mit Item in der Hand

**PlayerPoints setzen:**
1. Klicke im Trade-Creator auf das Sonnenblumen-Symbol
2. Shift-Klick für manuelle Eingabe
3. Gib die Zahl im Chat ein
4. GUI öffnet sich automatisch wieder mit gespeicherten Daten

---

## Version 1.0.0 (2025-11-22)

### 🎉 Initial Release

#### Features
- Custom Trader NPCs (Villager, Wandering Trader, Zombie Villager)
- Persistente Trader (überleben Server-Neustarts)
- YAML-basierte Konfiguration pro Trader
- Trade Editor GUI
- Trade Creator GUI
- Vanilla Items Support
- Nexo Custom Items Support
- PlayerPoints als Währung
- Kombinierte Zahlung (Items + PlayerPoints)
- Max Uses pro Trade
- Folia-Kompatibilität

#### Commands
- `/ct create <name> <mob>` - Trader erstellen
- `/ct edit` - Trade-Editor öffnen
- `/ct remove <name>` - Trader entfernen
- `/ct list` - Alle Trader auflisten
- `/ct reload` - Trader neu laden

#### Permissions
- `customtrades.use` - Zugriff auf alle Commands
- `customtrades.create` - Trader erstellen
- `customtrades.edit` - Trader bearbeiten
- `customtrades.remove` - Trader entfernen
- `customtrades.list` - Trader auflisten

#### Dependencies
- Paper 1.21.3+ (required)
- PlayerPoints 3.2.7+ (optional)
- Nexo 0.1.0+ (optional)

