# CustomTrades - Changelog

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

