# CustomTrades Plugin

Ein umfangreiches Paper/Folia 1.21.x Plugin für Custom Trader NPCs mit Nexo und PlayerPoints Support.

## Features

- ✅ Erstelle Custom Trader NPCs (Villager, Wandering Trader, Zombie Villager)
- ✅ Benutzerfreundliche GUI für Trade-Verwaltung
- ✅ Unterstützung für Vanilla-Items und Nexo Custom-Items
- ✅ PlayerPoints als alternative Währung
- ✅ Folia-kompatibel
- ✅ YAML-basierte Konfiguration pro Trader
- ✅ Persistente Trader (Überleben Server-Neustarts)

## Commands

- `/ct create <Name> <Mob>` - Erstellt einen neuen Trader an deiner Position
- `/ct edit` - Öffnet den Trade-Editor für den angeschauten Trader
- `/ct remove <Name>` - Entfernt einen Trader
- `/ct list` - Zeigt alle Trader an
- `/ct reload` - Lädt alle Trader neu

## Permissions

- `customtrades.use` - Zugriff auf alle Commands (default: op)
- `customtrades.create` - Trader erstellen (default: op)
- `customtrades.edit` - Trader bearbeiten (default: op)
- `customtrades.remove` - Trader entfernen (default: op)
- `customtrades.list` - Trader auflisten (default: op)

## Setup

### 1. Dependencies

Das Plugin benötigt folgende Dependencies:
- **Paper 1.21.3+** (oder Folia)
- **PlayerPoints 3.2.7+** (optional, für PlayerPoints-Trades)
- **Nexo 0.1.0+** (optional, für Custom-Items)

### 2. Installation

1. Lade das Plugin in den `plugins` Ordner
2. Starte den Server
3. (Optional) Installiere PlayerPoints und/oder Nexo
4. Nutze `/ct create` um deinen ersten Trader zu erstellen

## Verwendung

### Trader erstellen

```
/ct create MeinTrader villager
```

Spawnt einen Villager-Trader an deiner Position.

### Trades konfigurieren

1. Schaue den Trader an
2. Führe `/ct edit` aus
3. Klicke auf "Trade hinzufügen"
4. Lege Items aus deinem Inventar in die Slots oder nutze die Optionen
5. Stelle PlayerPoints-Kosten ein (optional)
6. Speichere den Trade

### YAML-Konfiguration

Jeder Trader wird in `plugins/customtraders/traders/<name>.yml` gespeichert:

```yaml
name: MeinTrader
mobType: VILLAGER
displayName: "§6Händler"
persistent: true
location:
  world: world
  x: 100.5
  y: 64.0
  z: 200.5
  yaw: 0.0
  pitch: 0.0

trades:
  0:
    input1:
      type: VANILLA
      material: DIAMOND
      amount: 10
    output:
      type: VANILLA
      material: EMERALD
      amount: 1
    maxUses: -1
    playerPointsCost: 0
    
  1:
    input1:
      type: NONE  # Kein Item - nur PlayerPoints
    output:
      type: NEXO
      nexoId: custom_sword
      amount: 1
    maxUses: 1
    playerPointsCost: 1000
```

## Item-Typen

- **VANILLA**: Normale Minecraft-Items
  ```yaml
  type: VANILLA
  material: DIAMOND_SWORD
  amount: 1
  displayName: "§bSpecial Sword"
  lore:
    - "§7A special sword"
  ```

- **NEXO**: Custom-Items von Nexo
  ```yaml
  type: NEXO
  nexoId: my_custom_item
  amount: 1
  ```

- **NONE**: Kein Item (für PlayerPoints-only Trades)
  ```yaml
  type: NONE
  ```

## PlayerPoints Integration

Wenn PlayerPoints installiert ist:
- Setze `playerPointsCost` auf einen Wert > 0
- Der Spieler benötigt die angegebenen Points für den Trade
- Bei erfolgreichem Trade werden die Points automatisch abgezogen
- Du kannst `input1` auf `type: NONE` setzen für reine PlayerPoints-Trades

## Folia-Kompatibilität

Das Plugin nutzt:
- Entity Scheduler für entity-bezogene Tasks
- Region Scheduler für region-bezogene Tasks
- Automatische Erkennung von Folia vs. Paper

## Build

```bash
./gradlew build
```

Die fertige JAR findest du in `build/libs/CustomTrades-1.0.0.jar`
## License

MIT License

