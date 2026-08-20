# Bedwars - Umsetzungsplan

Orientierung: Hypixel Bedwars. Ziel ist ein Plugin, das eine Runde auf einem eigenen Server
austrägt, seine Map aus einer Sammlung heruntergeladener Maps wählt und sich um alles kümmert,
was zwischen Warte-Lobby und Siegerbildschirm passiert - plus eigene Addons, die einzeln
zuschaltbar sind.

## Getroffene Entscheidungen

| Thema | Entscheidung |
|-------|--------------|
| Modi | Solo, Doubles, 3v3v3v3, 4v4v4v4 **und** freie Kombinationen (Teamzahl × Teamgröße aus der Config) |
| Arena | Genau **eine Arena pro Server**. Runde vorbei = Server wird weggeworfen, kein Map-Reset im Plugin |
| Rundenstart | Über das Event-System, das auf `feature/events-and-marketplace` bereits steht. Dazu ein Debug-Command in der Lobby, der denselben Weg ohne Event nimmt |
| Maps | Mehrere heruntergeladene Maps liegen bereit, beim Start wird eine gewählt. Punkte (Betten, Spawns, Generatoren, Händler) werden per Command gesetzt |
| Shop | 1:1 Hypixel-Sortiment und -Preise, aber vollständig aus der Config |
| Upgrades | Sharpened Swords, Reinforced Armor, Maniac Miner, Iron Forge, Heal Pool, Dragon Buff, Traps - **und** die Custom-Addons werden hier gekauft |
| Generatoren | Hypixel-Timings als Standard, jeder Wert pro Map überschreibbar |
| Endgame | Bed Destruction → Sudden Death mit Drachen → hartes Zeitlimit mit Punktentscheid |
| Bett-Addon | Ein sehr teures Item, das **nur am Händler eines fremden Teams** gekauft werden kann. Heimtragen, in der eigenen Basis platzieren |
| Weitere Addons | Kits/Perks, Custom-Items, Killstreaks & Kopfgeld, Zufalls-Events - alle einzeln an- und abschaltbar |
| Addon-Schalter | Drei Wege: `addons.yml` als Basis, Launcher darf überschreiben, GUI in der Warte-Lobby für den Einzelfall |
| Stats | Rundenstatistik sofort, netzwerkweite Persistenz sauber gekapselt für später |
| Warte-Lobby | Eigene Warte-Lobby auf dem Bedwars-Server mit Countdown, Teamwahl und Auto-Balance |
| Sprache | Englisch, alle Texte aus `messages.yml` |
| Reihenfolge | **Gerüst für alles zuerst**, danach Feature für Feature |

## Ausgangslage

Im Modul stehen vier Klassen, von denen zwei fehlerhaft sind:

- `StartHandler.buildTeams()` kickt bei **zu wenigen** statt bei zu vielen Spielern
  (`players.size() < teamsize * 8`), ruft sich danach rekursiv auf und legt Scoreboard-Teams
  ohne Farbe, Namen und Präfix an.
- `GameSettingsCommand` prüft `args.length < 2` und greift im selben Zweig auf `args[1]` zu -
  der Zweig kann nur mit einer `ArrayIndexOutOfBoundsException` enden. Gespeichert wird
  außerdem nie (`saveConfig()` fehlt).
- `ConfigurationManager` kann nur eine einzige `config.yml`.
- `ChatColor` ist auf Paper 26.2 überall deprecated; im Rest des Projekts (`CustomInventory`,
  `ItemApi`) wird schon Adventure benutzt.

Beides wird in Phase 0 ersetzt, nicht geflickt.

## Was wir wiederverwenden

| Vorhanden | Wofür im Bedwars |
|-----------|------------------|
| `de.hems.paper.customInventory.CustomInventory` | Shop, Upgrade-Händler, Teamwahl, Kit-Auswahl, Addon-Menü |
| `de.hems.api.ItemApi` | Alle Items inklusive Teamfarben und Lore |
| `de.hems.api.ServerApi` | Debug-Command: Runden-Server erstellen, Zustand abfragen, am Ende stoppen |
| `de.hems.paper.warp` | Spieler auf den frisch erstellten Server bringen |
| `de.hems.paper.util.ChatPrompt` | Texteingaben im Setup (Map-Name, Anzeigename) |
| `de.hems.paper.event.*` (Event-Branch) | Kalender, Warteschlange, Preisvergabe - siehe nächster Abschnitt |

## Anschluss an das Event-System

Auf `feature/events-and-marketplace` steht das Event-System bereits fertig, inklusive der Teile,
die Bedwars sonst selbst hätte bauen müssen. Der Plan baut darauf auf, statt daneben.

**Was schon da ist:**

| Vorhanden | Was es tut |
|-----------|------------|
| `EventType` | Aufzählung der Eventarten (`SIMPLE`, `OTHER_WORLD`, `END`, `UHC_BOSSES`, `UHC_DRAGON`) |
| `EventData` | Ein Event im Netzwerk, mit freier `Map<String,String>` für eigene Einstellungen und Revision |
| `UhcSettings` | Typisierter Zugriff auf genau diese Einstellungen - die Vorlage für Bedwars |
| `RunQueue` | Warteschlange → Server über `ServerApi` erstellen → Spieler per `ServerConnector` verbinden |
| `RunData` / `RunService` | Ein Durchlauf: Teilnehmer, Servername, Zustand, Ergebnis. Launcher besitzt sie, jeder Server hält eine Kopie |
| `AwardService` | Preise (Geld und Items), die in `awards.yml` warten, bis der Gewinner joint |
| `RunPlugin` | Wie ein Wegwerf-Server sich selbst findet und am Ende abschaltet |
| `ServerTemplate.BEDWARS` / `FileType.PLUGIN.BEDWARS` | Vorlage und Jar sind schon registriert |

**Die wichtigste Erkenntnis daraus:** `RequestServerStartEvent` transportiert *keine* Parameter.
Ein frisch gestarteter Server weiß also nicht von allein, welchen Modus, welche Map und welche
Addons er spielen soll. `RunPlugin` löst das so, und Bedwars macht es genauso:

```java
// der Server kennt seinen eigenen Namen aus dem Verzeichnis, in dem er läuft
String self = ListenerAdapter.getName().toString();
// und sucht sich damit seine eigene Partie aus der Kopie, die jeder Server hält
MatchData mine = MatchService.getOpenMatchOf(self);
```

Damit steht die Antwort auf eine Frage, die im Plan vorher offen war: **die Runden-Konfiguration
reist als `MatchData` über den Launcher, nicht als Startparameter.**

**Was dazukommt** - jeweils als Zwilling zu dem, was für die UHC-Läufe schon existiert:

| Neu | Zwilling | Inhalt |
|-----|----------|--------|
| `EventType.BEDWARS` | `UHC_DRAGON` | Neue Eventart. Braucht eine eigene Kennung neben `isTimed()`, weil eine Partie kein Rennen gegen die Uhr ist |
| `BedwarsSettings` | `UhcSettings` | Modus, Map-Auswahl, Mindest-/Maximalspieler **und die Addon-Schalter**, gelesen aus den freien Einstellungen des Events |
| `MatchData` / `MatchService` | `RunData` / `RunService` | Eine Partie: Teilnehmer, Servername, Modus, Map, Ergebnis (Sieger, Kills, Betten) |
| `MatchQueue` | `RunQueue` | Warteschlange in der Lobby, startet bei voller Gruppe den Server und verbindet alle |
| Preisvergabe | `AwardService` | Sieger bekommen Geld und Items über denselben Weg - ohne eine Zeile neuen Auslieferungscode |

Das räumt zwei Punkte aus dem Plan ab, die vorher offen standen:

- **Die drei Wege der Addon-Schalter** sind jetzt konkret: `addons.yml` als Standard,
  `EventData.settings` über die `MatchData` als Override des Launchers, GUI in der Warte-Lobby
  als letztes Wort. Genau die Rangfolge, die du wolltest, und der mittlere Weg existiert schon.
- **Die Stats-Persistenz**, die ich nach hinten geschoben hatte, ist das Ergebnisfeld der
  `MatchData` beim Launcher. Auch die Bestenliste gibt es in `RunService.getLeaderboard` schon
  in der passenden Form.

Nebenbei steht im `TODO.md` des Branches unter 1.5 offen: *"Eigene Lobby-Minispiel-Events mit
Teilnehmerzahl und Preisen über einen kurzen Zeitraum - die Event-Typen dafür gibt es noch nicht."*
Bedwars ist genau dieser fehlende Typ.

**Eine Namenskollision vermeiden:** `de.hems.paper.event` sind die *Kalender*-Events des
Netzwerks. Die Bukkit-Events des Bedwars-Plugins heißen deshalb `de.schnorrenbergers.bedwars.api`,
nicht `...bedwars.event` - sonst steht in jeder Datei ein Import, der das Gegenteil von dem
bedeutet, was er zu sagen scheint.

---

# Architektur

Ein Server trägt genau eine Runde aus, also gibt es genau ein `Game`. Das macht den ganzen
Zustand überschaubar: kein Arena-Register, keine Zuordnung Spieler → Arena, keine
Welt-Verwaltung zur Laufzeit.

```
de.schnorrenbergers.bedwars
├── Bedwars.java                  Lifecycle, hängt alles zusammen
├── game/
│   ├── Game.java                 der eine Spielzustand: Teams, Spieler, Map, Phase, Timeline
│   ├── GameLoop.java             ein Task, treibt Phase, Generatoren und Timeline
│   ├── GameTeam.java             Farbe, Mitglieder, Bett, Upgrade-Stufen, Trap-Queue, Spawn
│   ├── GamePlayer.java           Team, lebendig?, Kills, Finals, Betten, Killstreak, Kopfgeld
│   ├── GameMode.java             Teamzahl × Teamgröße, aus modes.yml
│   ├── phase/
│   │   ├── GamePhase.java        onEnter / tick / onExit
│   │   ├── LobbyPhase.java       Warten, Countdown, Teamwahl
│   │   ├── IngamePhase.java      die Runde selbst
│   │   └── EndPhase.java         Siegerbildschirm, Aufräumen
│   └── timeline/Timeline.java    geplante Ereignisse mit Countdown ("Diamond II in 4:12")
├── api/                          eigene Bukkit-Events als einzige Andockstelle für Addons
├── map/
│   ├── ArenaMap.java             Map-Definition im Speicher
│   ├── MapRepository.java        maps/<name>.yml lesen, schreiben, auflisten
│   ├── MapLoader.java            Weltordner kopieren und laden
│   ├── MapValidator.java         "Team RED: bed missing"
│   └── setup/                    /bw setup, Zauberstab, Positions-Commands
├── generator/                    Generatoren, Stufen, Hologramme, Item-Cap
├── shop/
│   ├── item/                     shop.yml → Kategorien, Einträge, Kaufregeln
│   ├── upgrade/                  Team-Upgrades und ihre Wirkung
│   ├── trap/                     Trap-Queue, Auslösen
│   └── villager/                 Händler spawnen, schützen, anklicken
├── addon/
│   ├── Addon.java                id, Standard, enable(Game), disable(Game)
│   ├── AddonRegistry.java        Config + Launcher-Override + GUI
│   └── impl/                     BedToken, Kits, CustomItems, Killstreaks, RandomEvents
├── match/                        MatchData dieses Servers finden, Ergebnis zurückmelden
├── scoreboard/                   Sidebar, Tablist, BossBar
├── stats/                        Rundenzahlen jetzt, Persistenz-Schnittstelle für später
└── util/                         Configs, Messages, Cuboid, Hologramme, Countdown-Format
```

## Zwei Entscheidungen, die alles andere tragen

**1. Phasen statt Zustandsabfragen.** Jede Phase ist eine Klasse mit `onEnter`, `tick`,
`onExit`. Listener fragen `game.getPhase()`, statt an zwanzig Stellen `if (running)` zu prüfen.
Ein neuer Zustand (z.B. ein Warmup) ist damit eine Klasse und keine Änderung an dreißig Stellen.

**2. Eigene Events als einzige Andockstelle.** Alles, was im Spiel passiert, wird als Bukkit-Event
gefeuert:

```java
BedwarsGameStateChangeEvent   BedwarsPlayerKillEvent     BedwarsPurchaseEvent
BedwarsBedDestroyEvent        BedwarsTeamEliminatedEvent BedwarsUpgradeEvent
BedwarsPlayerRespawnEvent     BedwarsGameEndEvent        BedwarsResourceSpawnEvent
```

Addons hören ausschließlich darauf. Ein Addon fasst nie die Spiellogik an, und Abschalten heißt
Listener abmelden - genau deshalb kann jedes Addon wirklich aus. Nebenbei kann später jedes andere
Plugin (Event-System, Stats, Discord) an derselben Stelle andocken.

## Konfiguration

Statt einer `config.yml` mehrere Dateien unter `configs/bedwars/`, jeweils mit einer
Standardfassung aus dem Jar:

| Datei | Inhalt |
|-------|--------|
| `game.yml` | Modus, Mindest-/Maximalspieler, Countdowns, Respawn-Zeit, Bau-Regeln, Zeitlimits |
| `modes.yml` | Die vier Hypixel-Modi plus eigene: Teamzahl, Teamgröße, welche Teams benutzt werden |
| `generators.yml` | Standard-Timings, Stufen, Item-Caps, Hologramm-Texte |
| `shop.yml` | Kategorien und Einträge mit Preis, Menge, Kaufregeln |
| `upgrades.yml` | Team-Upgrades, Stufenpreise, Wirkung; Traps mit Preis-Eskalation |
| `timeline.yml` | Wann die Runde was mit sich macht, die Drachen, die Punktwertung |
| `addons.yml` | Jedes Addon an/aus plus seine eigenen Einstellungen |
| `kits.yml` | Kits mit Startausrüstung und Perks |
| `messages.yml` | Alle Texte, englisch, MiniMessage |
| `maps/<name>.yml` | Alles, was zu einer Map gehört |

Eine Map sieht so aus:

```yaml
name: aquarium
display-name: '<aqua>Aquarium'
world: aquarium              # Ordner unter maps/
lobby: {x: 0, y: 100, z: 0, yaw: 0, pitch: 0}
spectator: {x: 0, y: 120, z: 0}
build:
  max-y: 90
  void-y: 0
modes:                       # welche Teams welcher Modus benutzt
  solo:    [RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY]
  doubles: [RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY]
  trio:    [RED, BLUE, GREEN, YELLOW]
  quad:    [RED, BLUE, GREEN, YELLOW]
teams:
  RED:
    spawn: {x: 50, y: 70, z: 0, yaw: 90}
    bed:   {x: 55, y: 70, z: 0}
    shop:  {x: 48, y: 70, z: 3}
    upgrade: {x: 48, y: 70, z: -3}
    generator: {x: 50, y: 70, z: 0}
    protection: 8            # Radius, in dem Gegner nicht bauen dürfen
generators:
  - {type: DIAMOND, x: 0, y: 75, z: 30}
  - {type: EMERALD, x: 0, y: 75, z: 0}
```

Was der Standard vorgibt, steht in `generators.yml`; was hier steht, gewinnt. So laufen alle
Maps ohne eigene Timings, und eine besonders große Map darf trotzdem langsamere Diamanten haben.

## Maps und Welten

Heruntergeladene Maps liegen als Weltordner unter `maps/<name>/`, daneben die `maps/<name>.yml`.

- **Spielbetrieb:** beim Start wird der gewählte Ordner nach `arena_<name>` kopiert und geladen.
  Die Runde spielt in der Kopie, das Original bleibt unberührt. Da der Server danach weggeworfen
  wird, braucht es keinen Reset.
- **Setup:** `/bw setup <map>` lädt dieselbe Kopie, `/bw setup save` schreibt die Punkte in die
  `maps/<name>.yml` **und** die Welt zurück nach `maps/<name>/`. Die Map ändert sich also, wenn du
  es sagst - nicht weil jemand ein Loch in die Arena gesprengt hat.

**Wo eine Welt liegt, hat sich mit 26.2 geändert** (auf dem Server nachgemessen, nicht vermutet):
eine Zusatzwelt ist keine Ordner mehr neben der Hauptwelt, sondern eine Dimension *in* ihr:

```
world/dimensions/minecraft/arena_<name>/region
```

Kopiert man eine Map im alten Layout hinein - und genau so sieht jede heruntergeladene Map aus -,
importiert der Server sie und **verschiebt** sie dabei dorthin. Laden funktioniert deshalb weiter,
aber nichts darf annehmen, die Welt später da wiederzufinden, wo es sie hingelegt hat.
`World.getWorldFolder()` ist die einzige ehrliche Antwort, und daraus liest das Zurückschreiben.

Die Map-Auswahl kommt in dieser Reihenfolge: `--map` beim Serverstart (später vom Event-System),
sonst `map:` aus der `game.yml`, sonst per `/bw map <name>` in der Warte-Lobby, sonst zufällig
aus allen Maps, die zum Modus passen und die Validierung bestehen.

---

# Reihenfolge der Umsetzung

Jede Phase endet mit etwas, das man anfassen kann. Die Reihenfolge ist so gewählt, dass nichts
später umgebaut werden muss - das Gerüst steht zuerst, und jede weitere Phase hängt sich nur ein.

## Phase 0 - Fundament

*Nichts davon ist spielbar. Danach hat alles Weitere einen Platz.*

1. **Configs**: `BedwarsConfig` mit typisierten Zugriffen, Standarddateien aus dem Jar,
   `/bw reload`. Der alte `ConfigurationManager` fällt weg.
2. **Messages**: `Messages.get(key, placeholders...)` auf Adventure/MiniMessage, `messages.yml`
   englisch vorbelegt. Kein `ChatColor` mehr im Modul.
3. **Datenmodell**: `Game`, `GameTeam`, `GamePlayer`, `GameMode`, `TeamColor`.
4. **Phasen**: `GamePhase` mit den drei Implementierungen, `GameLoop` als einziger
   wiederholender Task.
5. **Events**: die neun Events oben, mit `HandlerList`, teilweise `Cancellable`.
6. **Addon-Gerüst**: `Addon`, `AddonRegistry`, `addons.yml`, die drei Konfigurationswege
   (Datei → Launcher-Override → GUI) mit klarer Rangfolge.
7. **Aufräumen**: `StartHandler` und `GameSettingsCommand` ersetzt, `plugin.yml` neu
   (`/bw` mit Unterbefehlen statt `/gameSettings`).

**Fertig, wenn** der Server startet, `/bw reload` und `/bw addons` funktionieren und die
Phasenmaschine leer durchläuft.

## Phase 1 - Map und Setup

1. `ArenaMap` + `MapRepository` (lesen/schreiben/auflisten).
2. `/bw setup <map>` - Setup-Modus mit Unterbefehlen:
   `lobby`, `spectator`, `team <color> spawn|bed|shop|upgrade|generator|protection|remove`,
   `gen add <typ>|remove`, `build <max-y> [void-y]`, `mode <name> <teams...>`, `name`,
   `check`, `save`, `exit`.
   **Kein Zauberstab**: jeder Punkt kommt aus deiner Position oder dem Block, den du ansiehst.
   Das machst du beim Ablaufen der Map ohnehin, und ein Stab wäre ein Gegenstand mehr, den man
   verliert, weiterreicht und hinterher wieder einsammelt.
3. `MapValidator` mit klaren Meldungen, welche Punkte fehlen.
4. `MapLoader`: kopieren, laden, zurückschreiben.

**Fertig, wenn** du eine heruntergeladene Map einrichten und `/bw setup check` grün bekommen kannst.

## Phase 2 - Warte-Lobby und Start

1. `LobbyPhase`: Countdown (60/30/20/10/5…1), Start ab Mindestspielerzahl, Abbruch bei Unterschreitung.
2. Teamwahl per Wolle-GUI, Auto-Balance beim Start, volle Teams gesperrt.
3. Addon-Schalter über `/bw addon <id> on|off|default`, gesperrt sobald die Runde läuft.
   Ein Menü für Modus und Map gibt es bewusst **nicht**: beides steht fest, sobald der Server
   seine Map geladen hat, und ein Knopf, der nichts mehr ändern kann, ist schlimmer als keiner.
4. **Debug-Command im LobbyPlugin**: `/bwdebug` erstellt über `ServerApi` einen `BEDWARS_x`,
   wartet bis er **Spieler annimmt** (nicht nur bis er läuft - dazwischen liegen Sekunden, in denen
   ein Verbindungsversuch am Proxy scheitert) und warpt dich hin. Dazu `list` und `stop <name>`.
   Map und Modus kann er nicht mitgeben: ein Startbefehl transportiert keine Parameter, das macht
   später die `MatchData`.
5. Spielstart: Teams bilden, teleportieren, Grundausstattung, Rüstung in Teamfarbe.

**Fertig, wenn** du über `/bwdebug` auf einem frischen Server landest, ein Team wählst und der
Countdown die Runde startet.

## Phase 3 - Kernspiel

1. **Betten**: nur fremde Betten zerstörbar, Ansage mit Titel und Sound für das betroffene Team,
   Bett-Blöcke sonst geschützt.
2. **Respawn**: 5 Sekunden mit Titel-Countdown, kurzer Spawn-Schutz, Grundausstattung zurück.
3. **Elimination**: kein Bett und tot = Zuschauer, Team-Ansage, Sieg wenn nur ein Team übrig ist.
4. **Bauen**: nur selbst gesetzte Blöcke abbaubar (Block-Tracking), Höhenlimit, Schutzradius um
   Spawns und Generatoren, keine Blöcke auf Händlern.
5. **Ressourcen**: Generatoren mit Timings und Stufen, Item-Cap am Boden, schwebende Anzeige mit
   Countdown, Split auf Teamspieler in der Basis (reihum, nicht an den Nächststehenden - sonst
   bekommt der, der auf dem Generator steht, alles). Ressourcen eines Getöteten gehen an den
   **Killer** statt zu droppen: sonst lohnt es sich nicht, jemanden mit acht Diamanten zu jagen,
   und Stillstehen wird zur sichersten Art, eine Ressource zu halten (`death.resources-to-killer`).
7. **Werkzeuge**: `/bw start` und `/bw stop` starten und beenden eine Runde ohne Wartezeit,
   `/bw generators` sagt für jeden Generator Stufe, Ort und Restzeit - beim Map-Bauen die Antwort
   auf "läuft der überhaupt".
6. **Anzeige**: Sidebar (Teams mit Bett-Status und Spieleranzahl, nächstes Ereignis mit
   Countdown), farbiger Tablist, Team-Chat mit `@` für global.

**Fertig, wenn** eine Runde ohne Shop von Anfang bis Sieg durchläuft.

## Phase 4 - Shop, Upgrades, Traps

1. Händler als bewegungsloser, unverwundbarer Villager mit Namensschild; Rechtsklick öffnet das Menü.
2. `shop.yml` mit allen Hypixel-Kategorien und -Preisen; Sonderregeln: Rüstung dauerhaft,
   Werkzeuge behalten ihre Stufe (mit Downgrade beim Tod), Wolle in Teamfarbe, Schwert-Ersatz.
   Alles, was einen Tod überlebt, steht im `Loadout` des Spielers und **nicht** im Inventar - sonst
   ist es genau das, was der Tod löscht. Der Respawn spielt dieselbe Liste wieder ein.
3. Upgrade-Händler: Sharpened Swords, Reinforced Armor I-IV, Maniac Miner I-II,
   Iron Forge I-IV, Heal Pool, Dragon Buff. Ein Upgrade ist eine Zahl am Team, also muss es nach
   jedem Kauf und jedem Respawn neu auf die Items gelegt werden - dafür gibt es genau eine Stelle.
4. Traps: Warteschlange mit drei Plätzen, Preis 1/2/4 Diamanten, It's a Trap, Counter-Offensive,
   Alarm Trap, Miner Fatigue. Dazu eine Abklingzeit pro Team (`traps.cooldown-seconds`): ohne sie
   räumt ein einziger Gegner, der in der Basis stehen bleibt, die ganze Warteschlange in einer
   Sekunde leer. Magic Milk hebt die vanilla-Wirkung von Milch bewusst auf - wer für Trap-Immunität
   bezahlt, soll dafür nicht seinen Speed-Trank verlieren.
5. **Was ein Item tut, hängt am Material, nicht an der Id** aus der Config: wer "fireball" umbenennt
   oder anders bepreist, verkauft immer noch eine Feuerkugel. Die Zahlen (Preis, Lebensdauer eines
   beschworenen Mobs) kommen weiter aus dem Eintrag.
   - TNT zündet **nach** den Bauregeln, nicht davor: TNT, das dort nicht gesetzt werden darf, darf
     dort auch nicht hochgehen.
   - Explosionen nehmen nur mit, was Spieler gesetzt haben, und **nie** ein Bett - sonst kostet das
     eine Ding, um das die Runde geht, acht Gold.
   - Bedbug und Dream Defender greifen ihr eigenes Team nicht an und verschwinden nach der
     Lebensdauer aus ihrem Shop-Eintrag.
   - Das **Brücken-Ei fällt hier heraus**: es braucht eigenes Verhalten und steht in Phase 6 als
     Custom-Item. Ein Item zu verkaufen, das nichts tut, wäre schlimmer, als es später zu haben.
6. Zwei Regeln, die vorher fehlten und ohne die der Shop keinen Sinn ergibt: **kein Schaden im
   eigenen Team**, und ein TNT-Tod gehört dem, der es gesetzt hat (TNT ist kein Projektil, sonst
   gehört der explosivste Kill des Spiels niemandem).
7. **Werkzeuge**: `/bw shop` und `/bw upgrades` öffnen beide Menüs ohne Villager - für eine Map,
   deren Händlerpunkte noch nicht gesetzt sind, und um eine Preisänderung zu sehen, ohne in eine
   Basis zurückzulaufen.

**Fertig, wenn** eine Runde mit Shop und Upgrades vollständig spielbar ist.

## Phase 5 - Endgame

1. `timeline.yml` mit den Hypixel-Zeiten: Diamond II (6:00), Emerald II (12:00), Diamond III,
   Emerald III, **Bed Destruction** (30:00), **Sudden Death** (40:00), **Game End** (50:00).
   Die Uhr läuft ab **Rundenstart**, nicht ab Serverstart - eine Lobby, die zehn Minuten auf Spieler
   gewartet hat, würde die Runde sonst mit Bed Destruction eröffnen. Der Fahrplan wird bei jedem
   Start neu gelesen, damit eine Änderung ein `/bw reload` braucht und keinen Neustart.
2. Was ein Ereignis *tut*, ist eine von fünf bekannten Aktionen (`GENERATOR_TIER`,
   `BED_DESTRUCTION`, `SUDDEN_DEATH`, `GAME_END`, `ANNOUNCE`) - aus demselben Grund wie bei den
   Upgrades: freier Text bräuchte eine eigene Sprache. Frei ist, *wann* und wie es heißt.
   `GENERATOR_TIER` fasst nur die Generatoren in der **Mitte** an; die in den Basen gehören dem
   Iron Forge, und beides an derselben Zahl würde ein gekauftes Upgrade wieder überschreiben.
3. Bed Destruction: alle Betten fallen, **beide Blockhälften** und ohne Drop - ein halbes Bett in
   der Basis sieht aus wie ein Bett, und das ist das Einzige, worin sich niemand irren darf.
4. Sudden Death: ein Enderdrache pro lebendem Team (einer mehr pro Dragon-Buff-Stufe). Zwei Zeilen
   API machen den Unterschied zu einem beschworenen Drachen: ein **Podium** auf der Kartenmitte,
   sonst fliegt er dorthin, wo in einer End-Welt sein Portal stünde, und eine **Zugehörigkeit**,
   damit er das Team, das ihn bezahlt hat, weder angreift noch jagt. Weil Anflug, Feuerball und die
   Wolke danach drei verschiedene Entities sind, werden alle drei auf ihren Drachen zurückgeführt.
   Blöcke reißt er keine heraus - dieselbe Explosionsregel wie beim TNT aus Phase 4.
5. Hartes Zeitlimit: Entscheidung nach Punkten (Betten, Finals, Kills, Gewichte in der Config) mit
   sichtbarer Wertung. Ein **ausgeschiedenes** Team gewinnt nicht auf Punkte - es hat die Runde
   verloren, als sein letzter Spieler fiel, und eine Tabelle, die ihm den Sieg zurückgibt, macht die
   Betten sinnlos. Gleichstand an der Spitze heißt bewusst: niemand gewinnt.
6. Endbildschirm: Sieger-Titel, Top 3 nach Kills, Finals und Betten (nach derselben Wertung, sonst
   widerspricht die Tafel der Entscheidung, die gerade gefallen ist), Feuerwerk in Teamfarbe, danach
   zurück in die Lobby und Server-Stopp über `ServerApi`. Ab hier ist niemand mehr verwundbar: eine
   entschiedene Runde, in der der Sieger noch auf dem Siegerbildschirm stirbt, hat nichts mehr, was
   dieser Tod bedeuten könnte.
7. **Werkzeug**: `/bw timeline` zeigt den Fahrplan mit Restzeit, `/bw timeline skip` zieht das
   nächste Ereignis sofort vor - und die späteren behalten ihren Abstand dazu. Ohne das ist ein
   Endgame nicht testbar: eine halbe Stunde auf Bed Destruction zu warten ist kein Test, den jemand
   zweimal macht.

**Fertig, wenn** keine Runde mehr ewig laufen kann.

## Phase 6 - Addons

Alle über `AddonRegistry`, jedes einzeln abschaltbar, jedes nur an den Events aus Phase 0.
Registriert heißt nicht eingeschaltet: was läuft, entscheiden `addons.yml`, das Event und das
Lobby-Menü - in dieser Reihenfolge. Jedes Addon liest seine eigenen Zahlen unter
`addons.<id>.settings`, und `/bw reload` schaltet es kurz aus und wieder an, damit eine geänderte
Zahl auch bei etwas ankommt, das schon läuft.

**Was der Shop dafür gelernt hat** (beides generisch, nicht für ein einzelnes Addon):

- Ein Preis ist eine **Liste**. Fast alles kostet eine Sorte, das Bett-Item muss in zwei Währungen
  teuer sein - und bezahlt wird alles oder nichts, sonst sind die Diamanten weg und der Smaragd hat
  gefehlt.
- Ein Eintrag kann **`enemy-only`** sein: er taucht nur am Händler eines fremden Teams auf. Nicht
  ausgegraut, sondern gar nicht - die halbe Idee ist, dass ihn im eigenen Base niemand sieht.
- Addons können Seiten und Einträge **zur Laufzeit anmelden**. In `shop.yml` steht davon nichts, ein
  ausgeschaltetes Addon hat also keinen Eintrag statt eines Eintrags, der sich nicht kaufen lässt.

1. **Bed Token** (dein Bett-Respawn-Item)
   - Kaufbar **ausschließlich am Händler eines fremden Teams**, Standard 8 Diamanten + 16 Smaragde.
   - Beim Kauf sehen alle: *"RED bought a Bed Token!"* - angesagt wird aber erst einen Tick später,
     wenn er wirklich im Inventar liegt. Wer ihn nicht bezahlen kann, löst sonst die Jagd auf sich
     selbst aus.
   - Wer ihn trägt, leuchtet; beim Tod nimmt das Addon ihn aus den Drops heraus und legt ihn selbst
     auf den Boden, weil die Runde die Drops gleich danach wegräumt.
   - Benutzt wird er mit **Rechtsklick an der eigenen Bettstelle**, nicht durch Platzieren: ein Bett,
     das irgendwo anders steht, sieht aus wie das eine Ding, um das die Runde geht.
   - Wie das Bett aussah, merkt sich das Addon **im Moment des Zerstörens** - Richtung und Farbe
     kommen aus dem Block, nicht aus einer Annahme.
   - Nur solange das eigene Bett zerstört und das Team nicht eliminiert ist; einmal pro Team, und
     zwei Tokens gleichzeitig kauft niemand aus Versehen.
2. **Kits/Perks**: Auswahl in der Warte-Lobby (`kits.yml`), Startausrüstung plus ein passiver Effekt.
   Bewusst klein gehalten - ein Kit, das eine Runde entscheiden kann, macht die Wahl in der Lobby zum
   ganzen Spiel. Das Kit wird bei **jedem Respawn** neu ausgegeben, sonst ist es für den, der zuerst
   stirbt, nichts wert.
3. **Custom-Items** auf einer eigenen Shop-Seite: Enterhaken (ein Wurf), Rettungsplattform (vergeht
   wieder), Brücken-Ei (Wolle in Teamfarbe), Sprungfeder. Alles, was sie bauen, geht in den
   Block-Tracker - eine Brücke, die niemand mehr abbauen kann, ist eine Wand. Die Sprungfeder ist
   eine **Druckplatte**: draufsteigen feuert von selbst ein Event, das kostet nichts, solange
   niemand draufsteht.
4. **Killstreaks & Kopfgeld** sind ein Addon, nicht zwei: eine Serie, die nur dem hilft, der sie hat,
   macht einen guten Spieler unaufhaltsam - das Kopfgeld ist das, was der Rest fürs Aufhalten bekommt.
5. **Zufalls-Events** (Ressourcenregen, schnellere Generatoren, Loot-Kiste) mit **Vorwarnung**. Die
   Vorwarnung ist keine Höflichkeit: ein Ereignis ohne sie belohnt Stehenbleiben in der Mitte.
   Standardmäßig aus, weil es als einziges Addon den Rhythmus der Runde selbst verändert.
6. **Lobby-Menü**: `/bw addons` öffnet für einen Spieler die Schalter (Konsole bekommt weiter die
   Liste als Text). Umschalten geht nur, solange gewartet wird - ein Addon, das mitten in der Runde
   dazukommt, ist eine Regeländerung im laufenden Spiel.
7. Dazu ein neues Event: `BedwarsTimelineEvent` feuert für **jeden** Eintrag der `timeline.yml`, auch
   für die reinen `ANNOUNCE`-Einträge. Damit kann ein Server ein Ereignis mit Namen und Uhrzeit
   hinschreiben und ein Addon darauf reagieren, ohne dass eines vom anderen weiß.

**Eine Ehrlichkeit zur Regel "Addons hören nur auf die eigenen Events":** für alles, was die Runde
*weiß* - gekauft, getötet, Bett gefallen, Phase gewechselt, respawnt - gilt sie unverändert. Was ein
Item *tut*, wenn jemand rechtsklickt, wirft oder auf etwas tritt, steht in keinem dieser Events und
kommt aus den normalen Bukkit-Events. Kein Addon greift in die Spiellogik, und Abschalten bleibt
`HandlerList.unregisterAll` plus das, was es angemeldet hat.

**Fertig, wenn** jedes Addon in `addons.yml` aus- und im Lobby-Menü wieder eingeschaltet werden
kann, ohne dass die Runde etwas davon merkt.

## Phase 7 - Feinschliff

1. **Statistik**: `RoundStats` ist eine Momentaufnahme, keine zweite Buchführung - gezählt wird
   weiter dort, wo es passiert (am Spieler, am Team), weil eine zweite Stelle, die Kills zählt, eine
   zweite Stelle ist, die sich irren kann. `StatsRepository` hat genau eine Methode; heute schreibt
   sie eine Datei pro Runde, morgen ist der Launcher die zweite Implementierung und sonst ändert sich
   nichts. `/bw stats` zeigt dieselbe Tabelle im laufenden Spiel.
2. **Zuschauer**: das meiste macht der Spectator-Modus schon. Was er nicht abdeckt, ist der Zustand
   *dazwischen* - wer gerade gestorben ist und zurückkommt, ist für Minecraft kein Zuschauer, für die
   Runde aber schon. Die Regeln hängen deshalb an `GamePlayer.isAlive()` und nicht am Spielmodus.
   Dazu `/bw watch`: die Liste aller, die noch stehen, weil ein Toter den, den er sehen will, sonst
   erst suchen müsste.
3. **Kanten**:
   - **Feuer** breitet sich nicht aus und brennt nichts ab. Ein Feuerball über einer Holzbrücke würde
     sich sonst durch eine Basis fressen, die niemand verteidigen kann.
   - **Ender-Pearl-Cooldown** (`ender-pearl-cooldown-seconds`, Standard 5): gestoppt wird der *Wurf*,
     nicht der Flug - eine abgebrochene Perle wäre weg und hätte nichts getan.
   - **Void-Tode** gehören schon seit Phase 3 dem letzten Angreifer.
   - **Abmelden im Kampf** zählt als Tod, mitsamt Kill für den, der gerade zugeschlagen hat. Was es
     nicht ist, ist ein Ausstieg: solange das Bett steht, behält man seinen Platz und kommt beim
     Wiederverbinden dort hinein, wo man war - der häufigste Grund dafür ist eine Leitung und keine
     Entscheidung (`death.keep-place-when-offline`).
4. **Performance**: Generatoren fragen die Welt nur noch **einmal** pro Drop nach ihren Entities
   (Cap und Merge in einem Durchgang) und legen den Drop auf den Stapel, der schon da liegt.
   Minecraft merged zwar von selbst, aber erst nachträglich - ein Generator, den zwei Minuten niemand
   leert, verbringt diese zwei Minuten sonst als ein Dutzend Entities. Hologramme sind seit Phase 3
   Display-Entities, und keine Schleife über `getNearbyEntities` läuft pro Tick.
5. **README**: eigener Abschnitt wie bei den anderen Modulen - Configs, Befehle, Addons, Testen.

**Fertig, wenn** eine Runde von außen aussieht wie ein fertiges Minispiel und nicht wie ein Prototyp.

# Was danach offen bleibt

- **Event-System**: die Schnittstelle, über die eine Runde später bestellt wird (Modus, Map,
  Addons, Spielerliste). Phase 2 baut den Debug-Weg absichtlich so, dass er dieselben Parameter
  benutzt - das Event-System ersetzt später nur den Aufrufer.
- **Stats-Persistenz** beim Launcher (Phase 7 legt die Schnittstelle an).
- **Cosmetics** (Kill-Effekte, Bett-Zerstörungs-Animation, Sieg-Tanz) - reine Kür, hängt an
  denselben Events.
