# Minecraft Server setup

Ein Minecraft Netzwerk aus einem Velocity Proxy und beliebig vielen Paper Servern, die vom
`ServerLauncherApplication` gestartet, konfiguriert und verwaltet werden.

## Versionen

Alles läuft auf **Minecraft 26.2**:

| Teil | Version |
|------|---------|
| Paper | 26.2 (build 112) |
| Velocity | 3.5.1 (build 615) |
| WorldEdit | 7.4.5 |
| WorldGuard | 7.0.18 |
| CoreProtect | 24.0 |
| Chunky | 1.5.3 |
| Simple Voicechat | 2.6.21 (Paper) / 2.6.18 (Velocity) |

Paper 26.2 braucht **Java 25** - sowohl zum Bauen als auch zum Starten. Die Downloads stehen alle in
`CommonCode/src/main/java/de/hems/types/FileType.java`, ein Update ist also ein Update dieser einen Datei
(plus `paper-api` in den poms und `api-version` in den `plugin.yml`).

## Bauen und starten

```bash
./mvnw clean install     # baut alle Plugins nach ./builds/
./start.sh               # startet den Launcher in einer tmux Session
```

Der Launcher startet zuerst den Proxy und danach die Server aus `autostart` in der `main-config.yml`
(Standard: `LOBBY` und `SURVIVAL`).

## Server

Es gibt keine feste Liste von Servern mehr. Jeder Name kann gestartet werden: unbekannte Namen werden
registriert, bekommen einen freien Port aus dem Bereich 3100-3999 und werden dem Netzwerk gemeldet. Der
Proxy trägt sie über das VelocityPlugin sofort ein, so dass man ohne Neustart auf jeden Server warpen kann.
Ports und Einstellungen stehen in der `main-config.yml` unter `servers.<NAME>` und bleiben über Neustarts
erhalten.

### Im Spiel

| Befehl | Was er macht |
|--------|--------------|
| `/servermanger` | Übersicht aller Server, Einstellungen, neuen Server erstellen |
| `/servermanger create <name> [vorlage] [ram] [plugin,plugin,...]` | Server ohne Menü erstellen |
| `/servermanger stop\|restart <name>` | Server stoppen oder neu starten |
| `/warp` | Menü mit allen laufenden Servern |
| `/warp <server>` | Direkt auf einen Server springen |

Beim Erstellen wählt man eine Vorlage (`LOBBY`, `SURVIVAL`, `BEDWARS`, `EVENT`), danach Name, RAM und in
einem eigenen Menü die Plugins. Plugins, die zur Vorlage gehören, sind fest gesetzt, alle anderen
verfügbaren Plugins lassen sich frei an- und abwählen.

## API

Dieselben Aktionen gibt es programmatisch über `de.hems.api.ServerApi`, damit z.B. Events automatisch
erstellt werden können:

```java
// leerer Paper Server für ein Event
ServerApi.createEventServer("SOMMERFEST");

// eine Bedwars Runde mit den Standardplugins der Vorlage
ServerApi.createServer("BEDWARS_1", ServerTemplate.BEDWARS);

// eigener Server mit mehr RAM und zusätzlichen Plugins
ServerApi.createServer("KREATIV", ServerTemplate.EVENT, 4096, List.of(FileType.PLUGIN.WORLDEDIT));

// beliebig viele Server der gleichen Art
ServerApi.createServer(ServerApi.freeName("BEDWARS"), ServerTemplate.BEDWARS);

ServerApi.listServers();          // was läuft gerade
ServerApi.listJoinableServers();  // wohin kann gewarpt werden
ServerApi.stopServer("SOMMERFEST");
ServerApi.restartServer("SURVIVAL");
```

`listServers()`, `isRunning()` und `freeName()` warten auf die Antwort des Hosts, gehören also nicht in den
Main-Thread - dafür gibt es `listServersAsync()` bzw. `PaperContext.async(...)`.

Vorlagen (`de.hems.types.ServerTemplate`) legen Software, Standard-RAM und die Pflichtplugins fest;
`ServerTemplate.resolvePlugins(...)` mischt sie mit der freien Auswahl.

## Event Kalender

`/kalender` (auch `/calendar`, `/events`) öffnet auf **jedem** Server denselben Kalender. Er zeigt die
nächsten **27 Tage** als Raster: jeder Tag ist ein Item mit den Events des Tages, heute ist gold markiert.
Der Host besitzt den Kalender (`events.yml`), jede Änderung wird an alle Server gemeldet - dadurch sind alle
Server sofort synchron, ohne dass jemand etwas neu laden muss.

| Wer | Was |
|-----|-----|
| Alle | Tag anklicken → Events des Tages → Event anklicken → Teams, Rangliste, zum Event Server warpen |
| Alle | Team anklicken → beitreten oder verlassen |
| Admins | "Neues Event" → Art wählen → Name, Teams, Wertung, RAM, Plugins → "Tage" → Tage anklicken → speichern |
| Admins | Event bearbeiten, absagen oder den Event Server sofort starten |

Das Planen läuft genau so wie gewünscht über Klicks: im Planungsmodus schaltet ein Klick auf einen Tag das
Event für diesen Tag an oder aus, beliebig viele Tage sind möglich. Ist "Server automatisch starten" an,
startet der Host den Event Server am Event Tag von selbst - mit dem Plugin des Events drauf.

Ohne Menü geht es auch:

```
/kalender heute
/kalender liste
/kalender create BEDWARS "Sommer Cup" 2026-09-05,2026-09-06
```

## Event System

Ein Event besteht aus zwei Teilen: der **Definition** (`EventDefinition`, das Verhalten) und dem
**geplanten Event** (`ScheduledEvent`, die Daten). Nur die Daten wandern durchs Netzwerk, das Verhalten
löst jeder Server selbst auf - deshalb zeigt auch ein Server das Event an, auf dem dessen Plugin gar nicht
installiert ist.

Jedes Event hat Teams, und wie diese Teams verglichen werden, entscheidet eine `RankingStrategy`:

| Wertung | Bedeutung |
|---------|-----------|
| `HIGHEST_SCORE` | Meiste Punkte gewinnen |
| `LOWEST_SCORE` | Wenigste Punkte gewinnen |
| `FASTEST_TIME` | Kürzeste Zeit gewinnt |
| `NONE` | Teams ohne Rangliste |

Gleichstände teilen sich einen Platz. Mitgeliefert sind die Event Arten `BEDWARS`, `TURNIER`, `SPEEDRUN`
und `COMMUNITY`.

### Eine neue Event Art

Eine neue Art Event ist eine Klasse plus eine Zeile - Kalender, UI und API kennen sie danach automatisch:

```java
public class SchatzsucheDefinition extends EventDefinition {
    public String getId()            { return "SCHATZSUCHE"; }
    public String getDisplayName()   { return "Schatzsuche"; }
    public String getDescription()   { return "Wer findet den Schatz zuerst"; }
    public FileType.PLUGIN getPlugin() { return FileType.PLUGIN.SCHATZSUCHE; } // das eigene Plugin
    public String getIconMaterial()  { return "CHEST"; }
    public int getMaxTeamSize()      { return 2; }
    public RankingStrategy getDefaultRanking() { return RankingStrategies.FASTEST_TIME; }
}

// im onEnable des Plugins
EventRegistry.register(new SchatzsucheDefinition());
```

Eine eigene Wertung geht genauso über `RankingStrategies.register(...)`. Überschreibbar sind unter anderem
`createTeams(...)`, `getServerTemplate()`, `getAdditionalPlugins()`, `getMaxTeamSize()`,
`allowsPlayerSignup()` sowie die Haken `onEventStart(...)` und `onEventEnd(...)`.

### API

```java
// Event planen
ScheduledEvent event = EventApi.create("BEDWARS", "Sommer Cup");
event.setDays(List.of(LocalDate.now().plusDays(3)));
event.setTeamCount(4);
EventApi.schedule(event);

// oder in einem Aufruf
EventApi.createAndSchedule("TURNIER", "Herbst Cup", LocalDate.now().plusDays(7));

// aus dem Event Plugin heraus werten
EventApi.addScore(event.getId(), team.getId(), 5);
EventApi.setScore(event.getId(), team.getId(), 62);   // z.B. Zeit in Sekunden
Ranking ranking = EventApi.getRanking(event.getId());

// lesen und starten
EventApi.getEventsToday();
EventApi.getEventsOn(LocalDate.now().plusDays(1));
EventApi.startServer(event);   // Event Server mit dem Event Plugin hochfahren
EventApi.cancel(event.getId());
```

`EventApi.refresh()` holt den Kalender frisch vom Host und blockiert dabei - im Main-Thread stattdessen
`refreshAsync()` oder `PaperContext.async(...)` nutzen.

## Module

| Modul | Inhalt |
|-------|--------|
| `ServerLauncherApplication` | Startet und konfiguriert die Server, Event Kalender, Discord Bot, Web Konsole |
| `CommonCode` | Netzwerk-Events, `ServerApi`, `EventApi`, Server Manager UI, Kalender UI, Warp System |
| `LobbyPlugin` | Lobby, Parkour, Server Manager |
| `Survival` | Survival Spielmodus |
| `Bedwars` | Bedwars Minispiel und Event Art `BEDWARS` |
| `VelocityPlugin` | Meldet neue Server am laufenden Proxy an |
