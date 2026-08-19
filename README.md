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

## Admin Website

Der Launcher bringt eine Weboberfläche mit, standardmäßig auf `http://<host>:8080/`. Beim ersten Start
legt er einen Account an und schreibt Benutzername, Passwort und den Google-Authenticator-Schlüssel
(inklusive `otpauth://` Link zum Scannen) einmalig in die Konsole.

### Login

Der Login braucht **Passwort und Google Authenticator Code zusammen**. Die Reihenfolge ist festgelegt:

1. Ohne Code wird der Request abgelehnt - das Passwort wird gar nicht erst angefasst.
2. Der Code wird geprüft. Stimmt er nicht, endet der Login hier, wieder ohne Passwortprüfung.
3. Erst danach wird das Passwort geprüft.

Jeder Versuch wird zusätzlich frühestens nach der **Grace Period von 3 Sekunden** beantwortet, und die
Grace Period beginnt mit dieser Antwort von vorne. Zwischen zwei Versuchen liegen also immer mindestens
3 Sekunden, egal ob sie nacheinander oder gleichzeitig kommen. Alle Fehlerfälle brauchen exakt gleich
lange, damit sich aus der Antwortzeit nichts ablesen lässt. Ein einmal benutzter Code gilt nicht noch
einmal.

Passwörter liegen als PBKDF2-Hash in der `main-config.yml`, nie im Klartext. Die Session hängt an einem
`HttpOnly`-Cookie, ändernde Requests brauchen zusätzlich den CSRF-Token aus der Session.

### Panels

| Panel | Was es kann |
|-------|-------------|
| Server | Zeigt welche Server an sind, und schaltet sie an, aus oder neu |
| Paying Player | Trägt zahlende Spieler per Minecraft-Name oder UUID ein und aus |
| Konsole | Zeigt die Ausgabe eines Servers live an und schickt Befehle an ihn |

### Live-Konsole

Das Konsolen-Panel hängt an einem WebSocket (`/api/console/stream?server=<NAME>`) und zeigt die Ausgabe
eines Servers, während sie entsteht. Beim Verbinden kommen erst die letzten 300 Zeilen, danach jede neue
einzeln. Reißt die Verbindung ab, verbindet die Seite sich nach 3 Sekunden neu.

Die Server laufen in tmux, ihre Ausgabe kommt also nie durch den Launcher. `tmux pipe-pane` schreibt sie
deshalb in `servers/<NAME>/console.log`, und der `ConsoleTailer` folgt dieser Datei, entfernt die
Terminal-Steuerzeichen und legt die Zeilen in einen `ConsoleBuffer` pro Server. Der Buffer hält die
Historie und die offenen WebSockets - Abonnieren und Anhängen laufen unter demselben Lock, damit ein
Zuschauer weder eine Zeile verpasst noch eine doppelt sieht.

Der WebSocket ist genauso geschützt wie der Rest: die Anmeldung wird schon beim Upgrade geprüft, und der
`Origin` muss stimmen, weil ein WebSocket-Handshake nicht unter die Same-Origin-Policy fällt und das
Session-Cookie sonst von jeder fremden Seite mitgeschickt würde.

### Erweitern

Ein neues Panel ist eine Klasse, die `WebModule` implementiert, plus eine Zeile in
`WebServer.loadModules()`:

```java
public class MeinModul implements WebModule {
    public String getId()    { return "mein-modul"; }
    public String getTitle() { return "Mein Modul"; }

    public void register(WebServer server) {
        server.route("/api/mein-modul", new MeinHandler(server));
    }
}
```

Die Navigation der Seite wird aus der Modulliste gebaut, die der Server ausliefert - das Modul taucht
also von selbst im Browser auf. Ohne eigene Ansicht bekommt es eine generische Darstellung seiner
API-Antwort; eine eigene Ansicht registriert man in `app.js` mit
`McAdmin.registerPanel("mein-modul", fn)`.

Module lassen sich auch von außen dazustecken, ohne `WebServer` anzufassen:
`new WebServer(configuration, new MeinModul())`.

Ein Modul kann neben `get`/`post`/`delete` auch einen WebSocket anmelden, der die Anmeldung schon beim
Upgrade prüft:

```java
server.authenticatedWs("/api/mein-modul/stream", ws -> {
    ws.onConnect(ctx -> ctx.send("hallo"));
    ws.onClose(ctx -> ...);
});
```

### Einstellungen (`main-config.yml`)

```yaml
web:
  enabled: true
  port: 8080
  bind: 0.0.0.0
  grace-period-seconds: 3
  session-timeout-minutes: 60
  secure-cookie: false      # auf true, sobald die Seite hinter HTTPS läuft
  totp:
    issuer: MCServer
    digits: 6
    period-seconds: 30
    window: 1               # wie viele 30s-Schritte Uhrenabweichung erlaubt sind
```

Der alte `/command` Endpoint gibt es weiterhin, er akzeptiert aber nicht mehr das fest eingebaute Secret
`67`, sondern das aus `web.command-secret`, das beim ersten Start erzeugt wird.

### Technik

Die Seite läuft auf [Javalin](https://javalin.io) (Jetty), weil der eingebaute `com.sun.net.httpserver`
keine WebSockets kann. Die Auth-Schicht (`Totp`, `Passwords`, `AuthService`, `Session`) ist davon
unabhängig und würde einen weiteren Wechsel unverändert überstehen.

Wichtig fürs Packaging: das Fat Jar wird über `src/assembly/jar-with-dependencies.xml` gebaut statt über
den eingebauten `descriptorRef`. Jetty findet Teile von sich per `ServiceLoader`, und der eingebaute
Descriptor überschreibt gleichnamige `META-INF/services`-Dateien, statt sie zusammenzuführen - ohne den
`metaInf-services`-Handler fehlen 17 der 38 Service-Provider im fertigen Jar.

## Teams

Teams gehören dem **Launcher**, nicht mehr einem einzelnen Server. Früher lagen sie in einer
`team-config.yml` neben dem Survival-Server - damit existierten sie nur dort und waren weg, sobald das
Verzeichnis gelöscht wurde. Jetzt speichert der Launcher sie in `teams.yml`, jeder Server hält eine lokale
Kopie, und der Launcher meldet jede Änderung ins Netzwerk. Ein Team, das auf einem Server erstellt wird,
ist einen Moment später überall bekannt.

Schreibzugriffe laufen immer über den Launcher und sind **optimistisch gesperrt**: jedes Team trägt die
Revision, mit der es gelesen wurde. Ändert jemand anderes es zwischendurch, wird der Schreibvorgang
abgelehnt statt eine Änderung stillschweigend zu überschreiben.

Beim ersten Start werden vorhandene Teams automatisch übernommen: Anführer aus der alten Config,
Mitglieder, Tag und Farbe aus Minecrafts eigenem Scoreboard, Claims aus dem alten `claims`-Abschnitt.
Danach wird die alte Datei als migriert markiert (nicht gelöscht).

### Befehle

| Befehl | Was er macht |
|--------|--------------|
| `/cteam` | Öffnet den Team-Manager |
| `/cteam create <name> <tag>` | Team gründen |
| `/cteam invite <spieler>` · `invite accept\|reject` | Einladungen |
| `/cteam join <team>` | Einem offenen Team beitreten |
| `/cteam leave` · `kick <spieler>` · `transfer <spieler>` | Mitgliederverwaltung |
| `/cteam rename <name>` · `tag <tag>` · `disband` | Team umbenennen, Tag ändern, auflösen |
| `/cteam sethome` · `home` | Team-Home setzen und nutzen |
| `/cteam info [team]` · `list` | Team-Infos, alle Teams im Netzwerk |
| `/cteam claim` · `unclaim` · `chunks` | Chunks kaufen, freigeben, Karte anzeigen |

### Einstellbar

Was **das Team** selbst festlegt, steht im Manager unter *Einstellungen* und liegt beim Team:
maximale Mitglieder, Friendly Fire, offener Beitritt, ob Mitglieder claimen oder einladen dürfen, ob der
Rucksack aktiv ist und ob Mitglieder daraus entnehmen dürfen, Team-Home, Beitritts-Ankündigungen.

Was **der Server** vorgibt, steht in `configs/team.yml` auf dem Survival-Server:

```yaml
members:
  maximum: 8            # Obergrenze - ein Team darf sich darunter selbst begrenzen
name:
  minimum-length: 3
  maximum-length: 16
  maximum-tag-length: 5
claims:
  base-cost: 50         # was der erste Chunk kostet
  growth: 1.1           # 10 % mehr pro weiterem Chunk
  maximum-cost: 1000
  maximum-per-team: 0   # 0 = unbegrenzt
permissions:
  allow-rename: true
  allow-disband: true
  allow-public-join: true
home:
  cooldown-seconds: 60
  warmup-seconds: 3     # Bewegen bricht ab
```

Eine neue Einstellung ist ein Eintrag in `TeamSettings.Key` - der Manager baut seine Buttons aus dem Enum,
Speicherung und Netzwerk-Übertragung ändern sich nicht.

## Backpack

Ein eigenes, auswählbares Plugin (`BackpackPlugin`). `/backpack` oder `/bp` öffnet den Rucksack, den sich
ein **Team teilt**. Er liegt beim Launcher neben den Teams, ist also auf jedem Server derselbe.

**Die Größe hängt davon ab, wer zahlt:** sobald die zahlenden Mitglieder eines Teams in der *Mehrheit*
sind, wird aus der Kiste (27 Slots) eine Doppelkiste (54). Gleichstand zählt nicht als Mehrheit - ein
Zweierteam braucht also beide. Das wird bei jedem Öffnen neu berechnet.

Schauen mehrere Mitglieder gleichzeitig hinein, teilen sie sich auf demselben Server ein und dasselbe
Inventar und sehen sich gegenseitig zu. Gespeichert wird, sobald der Letzte es schließt. Wurde der Rucksack
in der Zwischenzeit auf einem anderen Server verändert, wird der Schreibvorgang abgelehnt und der Spieler
darauf hingewiesen - statt die Änderungen des anderen kommentarlos zu überschreiben.

Einstellbar in `configs/backpack.yml`:

```yaml
size:
  default-rows: 3          # normales Team = Kiste
  paying-majority-rows: 6  # zahlende Mehrheit = Doppelkiste
title: '&6Team-Rucksack &7- &f%team%'
announce-size: true        # sagt beim Öffnen, wie viele Unterstützer noch fehlen
```

Das Plugin braucht ein Team-System und damit eine Netzwerkverbindung; es gehört zur Vorlage `SURVIVAL` und
kann bei jedem anderen Paper-Server dazugewählt werden.

## Admin-Ablage

Im Spieler-Panel der Website lassen sich Items **per Drag & Drop** verschieben: innerhalb eines Inventars
umsortieren, zwischen Inventar und Enderchest, und vor allem hinaus in die **Admin-Ablage**. Das ist die
Kiste, die im Spiel mit `/admin` geöffnet wird - nur für Operatoren bzw. mit der Berechtigung
`mcserver.adminstash`.

Damit hat das Herausnehmen aus einem Spielerinventar endlich ein Ziel: Item im Browser rüberziehen,
speichern, im Spiel `/admin` und rausnehmen.

Die Ablage liegt beim Launcher (`stashes.yml`), ist also von jedem Server aus dieselbe. Sie wird slotweise
gespeichert - Material, Anzahl und die Bytes, die Bukkit aus genau diesem Item gemacht hat. Der Launcher
liest nur die ersten beiden Felder (er hat kein Bukkit), der Spielserver baut das Item aus dem dritten
wieder auf, samt Verzauberungen und Namen.

### Was beim Speichern passiert

Ein Spielerinventar lebt in einem Paper-Server, die Ablage beim Launcher - das sind zwangsläufig **zwei
Schreibvorgänge**, eine echte Transaktion gibt es nicht. Die Leiste speichert deshalb in fester
Reihenfolge: **erst das Spielerinventar, dann die Ablage.** Wird das Inventar abgelehnt, wurde nirgends
etwas geschrieben und der Browser hält noch alle Änderungen - einfach nochmal klicken. Andersherum könnte
ein Item in der Ablage *und* beim Spieler landen, und Duplizieren ist das einzige Ergebnis, das wirklich
weh tut.

Schlägt der zweite Schritt fehl, sagt die Leiste genau, was noch offen ist, und behält den Zustand - nichts
verschwindet still.

Beides ist revisionsgesichert: wer die Ablage öffnet, bekommt ihre Revision mit. Hat inzwischen jemand
anderes gespeichert - im Browser oder im Spiel - wird der Schreibvorgang mit 409 abgelehnt statt zu
überschreiben.

## Chunk Limiter

Damit ein ruckelnder Server spielbar bleibt, senkt der Survival-Server bei Lag die Sichtweite - aber nur
bei Spielern, die **nicht** für den Server zahlen. Wer zahlt, behält seine volle Sichtweite.

Gemessen wird nicht der Ein-Minuten-Durchschnitt von Bukkit, sondern wie lange die Ticks seit der letzten
Prüfung wirklich gebraucht haben; das reagiert deutlich schneller. Über mehrere Messungen wird gemittelt,
damit ein einzelner Ruckler nicht sofort allen die Sichtweite zusammenstreicht.

Runter geht es sofort, hoch nur vorsichtig: die TPS müssen erst deutlich über die Schwelle steigen
(`raise-hysteresis-tps`) und das mehrere Prüfungen lang halten (`raise-delay-checks`), und dann wird
immer nur eine Stufe zurückgenommen. So pendelt die Sichtweite nicht um eine Schwelle herum.

Wer zahlt, steht in der `main-config.yml` unter `paying-players` und wird über die Admin-Website oder den
Discord-Befehl `/payingplayer` gepflegt. Der Survival-Server holt die Liste im Hintergrund und arbeitet mit
der zuletzt erfolgreich geholten Fassung weiter, wenn eine Anfrage mal keine Antwort bekommt - eine
langsame Antwort darf keinen zahlenden Spieler herunterstufen. Solange die Liste noch nie angekommen ist,
wird niemand begrenzt.

Eingestellt wird das in `configs/chunklimiter.yml` auf dem Survival-Server:

```yaml
enabled: true
check-interval-ticks: 40      # wie oft gemessen und angepasst wird
smoothing-samples: 5          # über wie viele Messungen gemittelt wird
raise-delay-checks: 3         # so viele gute Messungen, bevor es wieder hochgeht
raise-hysteresis-tps: 1.5     # so weit über die Schwelle, bevor eine Stufe fällt
paying:
  max-view-distance: 12
  min-view-distance: 8
  penalty-factor: 0.0         # 0 = zahlende Spieler werden nie begrenzt
free:
  max-view-distance: 10
  min-view-distance: 4
  penalty-factor: 1.0
tiers:                        # ab welchen TPS wie viele Chunks abgezogen werden
- tps: 18.0
  penalty: 0
- tps: 15.0
  penalty: 2
- tps: 10.0
  penalty: 4
- tps: 5.0
  penalty: 6
- tps: 3.0
  penalty: 8
```

## Module

| Modul | Inhalt |
|-------|--------|
| `ServerLauncherApplication` | Startet und konfiguriert die Server, Discord Bot, Admin Website |
| `CommonCode` | Netzwerk-Events, `ServerApi`, Server Manager UI, Warp System |
| `LobbyPlugin` | Lobby, Parkour, Server Manager |
| `Survival` | Survival Spielmodus |
| `Bedwars` | Bedwars Minispiel |
| `BackpackPlugin` | Geteilter Team-Rucksack |
| `VelocityPlugin` | Meldet neue Server am laufenden Proxy an |
