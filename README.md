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

## Aussehen der Website

Die Oberfläche heißt intern „Kontrollraum“ und folgt einer einzigen Regel:
**Farbe ist für Zustand reserviert.** Alle Bedienelemente — Buttons, Navigation, Auswahl — sind unbunt
(Knochenweiß auf Graphit). Grün, Bernstein und Rot kommen nirgends sonst vor; wer auf der Seite Farbe
sieht, sieht einen Zustand. Vorher war Blau reine Dekoration und hat mit den Statusfarben um
Aufmerksamkeit konkurriert.

Die Neutrals haben einen leichten Grünstich (`#0F1210` … `#2A322C`) — Gerätelack statt des Blaugraus, das
jedes dunkle Interface erbt. Blöcke trennen sich durch Helligkeit, nicht durch Rahmen. Ein einziger
Radius: 2px.

**Schrift, mit einer Regel:** was ein Mensch formuliert hat, steht in Archivo; was eine Maschine gemessen
hat, in IBM Plex Mono. Koordinaten, UUIDs, TPS und Logzeilen sind Messwerte und sehen auch so aus. Archivo
ist ein Variable Font mit Breitenachse — aus einer Datei kommen die weit gesperrten Gerätebeschriftungen
und die stark verdichteten großen Zahlen.

Beide Schriften liegen als WOFF2 unter `web/fonts/` im Jar (~240 KB, SIL OFL, Lizenztexte daneben). Google
Fonts scheiden aus: die CSP erlaubt nichts von außen, sie hat dafür jetzt `font-src 'self'`.

Die Navigation ist eine linke Bank statt einer Kopfzeile — die Modulliste wächst mit jedem Modul, und
waagerecht läuft sie irgendwann aus dem Bild. Darüber steht auf jeder Seite ein Statusstreifen mit
Spielern, laufenden Servern und dem langsamsten Server. Die TPS kommen dabei echt vom jeweiligen
Paper-Server, huckepack auf der Spielerabfrage.

Der Name oben links ist konfigurierbar:

```yaml
web:
  brand: MCServer     # steht in der Bank und auf der Login-Karte
```

Nur ein dunkler Modus, bewusst — ein Werkzeug, das man nachts neben dem Server offen hat.

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

## Bedwars

Ein Bedwars-Server trägt **genau eine Runde** aus und wird danach weggeworfen. Deshalb gibt es keine
Arena-Verwaltung, keinen Map-Reset und keine Zuordnung Spieler → Arena: es gibt genau ein `Game`, und
wenn es vorbei ist, stoppt sich der Server über die `ServerApi` selbst.

Die Runde läuft über eine Phasenmaschine (Warte-Lobby → Spiel → Ende) und **einen** wiederholenden
Task. Alles, was passiert, wird zusätzlich als eigenes Bukkit-Event gefeuert
(`de.schnorrenbergers.bedwars.api`) - das ist die einzige Stelle, an der Addons andocken.

### Configs

Alle unter `configs/bedwars/` auf dem jeweiligen Server. Jede Datei schreibt sich beim ersten Start
selbst, mitsamt Kommentaren zu jedem Wert.

| Datei | Inhalt |
|-------|--------|
| `game.yml` | Modus, Map, Countdowns, Respawn, Bauregeln, Zeitlimits, Statistik |
| `modes.yml` | Solo/Doubles/3v3/4v4 und eigene: Teamzahl × Teamgröße |
| `generators.yml` | Was die Generatoren droppen, wie schnell, mit welchen Stufen |
| `shop.yml` | Kategorien und Einträge mit Preis, Menge und Kaufregeln |
| `upgrades.yml` | Team-Upgrades und die Trap-Warteschlange |
| `timeline.yml` | Wann die Runde was mit sich macht, die Drachen, die Punktwertung |
| `addons.yml` | Jedes Addon an/aus plus seine eigenen Einstellungen |
| `kits.yml` | Die Kits des Kit-Addons |
| `messages.yml` | Alle Texte, englisch, MiniMessage |
| `maps/<name>.yml` | Alles, was zu einer Map gehört |

Maps liegen als Weltordner unter `maps/<name>/`. Beim Start wird eine Kopie geladen, gespielt wird in
der Kopie. **Achtung bei 26.2:** eine Zusatzwelt liegt nicht mehr neben der Hauptwelt, sondern als
Dimension darin (`world/dimensions/minecraft/arena_<name>`) - der Server verschiebt sie beim Import
selbst dorthin.

### Befehle

Alles unter `/bw`, `bedwars.admin` für alles, was etwas verändert.

| Befehl | Was er tut |
|--------|------------|
| `/bw status` | Modus, Phase, Spielerzahl, Map |
| `/bw setup <map>` | Setup-Modus: Punkte setzen, prüfen, speichern (`/bw setup check\|save\|exit`) |
| `/bw start` \| `/bw stop` | Runde sofort starten oder beenden |
| `/bw generators` | Jeder Generator mit Stufe, Ort und Restzeit |
| `/bw timeline [skip]` | Fahrplan der Runde - und das nächste Ereignis sofort auslösen |
| `/bw shop` \| `/bw upgrades` | Die Menüs ohne Villager öffnen |
| `/bw stats` | Wertung der laufenden Runde |
| `/bw watch` | Zuschauer-Menü: zu einem lebenden Spieler springen |
| `/bw addons` \| `/bw addon <id> on\|off\|default` | Addons schalten (nur in der Warte-Lobby) |
| `/bw reload` | Alle Configs neu lesen |

### Addons

Einzeln schaltbar über `addons.yml`, das startende Event oder das Lobby-Menü - in dieser Reihenfolge,
das Spezifischere gewinnt.

| Addon | Was es tut | Standard |
|-------|------------|----------|
| `bed-token` | Sehr teures Item, nur am fremden Händler; bringt das eigene Bett zurück | an |
| `kits` | Kleine Startausrüstung mit einem passiven Effekt, Wahl in der Lobby | an |
| `custom-items` | Enterhaken, Rettungsplattform, Brücken-Ei, Sprungfeder | an |
| `killstreaks` | Buffs für Serien, Kopfgeld auf den, der eine hat | an |
| `random-events` | Alle paar Minuten ein Ereignis in der Mitte, mit Vorwarnung | aus |

### Testen

`/bwdebug` in der Lobby erstellt einen Bedwars-Server und warpt hin. Zum Testen des Endspiels ist
`/bw timeline skip` der wichtigste Befehl - sonst dauert Bed Destruction eine halbe Stunde. Was von
Hand geprüft werden sollte, steht in `Bedwars/TESTS.md`; der Umsetzungsplan mit allen Entscheidungen
in `Bedwars/PLAN.md`.

## Eigene Runden

Spieler können in der Lobby selbst eine Bedwars-Runde aufmachen. `/runde` zeigt, was gerade läuft,
und wer will, stellt sich über `/runde start` eine eigene zusammen: Map, Modus (Solo bis Quad),
Addons und ob sie öffentlich in der Liste steht oder privat bleibt. Wer sie startet, ist
Rundenadmin - in der Wartelobby bekommt er dasselbe Einstellungs-Item wie ein echter Admin, plus
drei Dinge, die nur für eine eigene Runde gelten: sofort starten, privat schalten und Spieler
rauswerfen. Ein Admin ist immer auch Rundenadmin.

Eine private Runde ist wirklich privat: sie steht nicht in der Liste, und wer den Servernamen rät
und hinwarpt, wird auf dem Rundenserver zurückgeschickt. Wer rein darf, entscheidet der Besitzer
über `/runde einladen <spieler>` in der Lobby oder über den Einladen-Knopf im Rundenmenü, der alle
zeigt, die woanders im Netzwerk online sind.

Standardmäßig ist das **aus**. Ein Admin schaltet es über `/runde admin` frei und stellt dort auch
alles andere ein:

| Einstellung | Was sie macht |
|-------------|---------------|
| Selbst starten | Der Hauptschalter. Aus heißt: nur Admins. |
| Runden pro Spieler | Wie viele Runden einer gleichzeitig offen haben darf |
| Runden insgesamt | Obergrenze über das ganze Netzwerk, `0` = kein Limit |
| Wartezeit | Sekunden, bis derselbe Spieler wieder starten darf |
| Während Events sperren | Läuft ein Event, startet niemand privat |
| Vorlauf vor Events | Minuten vor einem Event ist ebenfalls Schluss |
| Speicher pro Runde | Was ein Rundenserver bekommt, `0` = was die Vorlage sagt |

Die Werte liegen beim Launcher in `rounds.yml` unter `policy`, zusammen mit den Runden selbst.
Jede Änderung gilt sofort auf allen Servern.

Dazu kommt eine Bedingung, die nicht einstellbar ist: der Speicher muss da sein. Bevor eine Runde
startet, fragt die Lobby den Launcher, ob noch ein Server dieser Größe auf die Maschine passt.
Gefragt wird dort und nicht in der Lobby, damit zwei Spieler, die im selben Moment klicken, nicht
beide ein Ja bekommen - und damit jede Ablehnung an einer Stelle gezählt wird.

Der Knopf sagt vorher, warum er nicht geht ("Event XY startet in 3 Minuten"), statt es erst nach
dem Klick zu verraten.

### Maps

Zur Auswahl stehen die Maps, die mit der `BEDWARS`-Vorlage ausgeliefert werden, plus alles, was in
`./bedwars-maps` neben dem Launcher liegt. Ein Weltordner dort (mit `level.dat`) landet auf jedem
neu erstellten Rundenserver und taucht im Menü auf — ohne Release, ohne Code. Liegt die zugehörige
`<name>.yml` mit den Setup-Punkten daneben, wird sie mitkopiert; sonst muss die Map auf dem Server
einmal mit `/bw setup <name>` eingerichtet werden. Kopiert wird nur, was noch nicht da ist, damit
eine auf dem Server bearbeitete Kopie erhalten bleibt.

## Arbeitsspeicher

Der Launcher kennt jetzt die Größe seiner Maschine, hält eine Reserve fürs System frei und lehnt
Starts ab, die nicht mehr ins Budget passen. Ohne das fällt ein volles Netzwerk nicht auf: es
fängt an zu swappen, und dann ruckelt alles gleichzeitig.

| Einstellung | Umgebungsvariable | Standard |
|-------------|-------------------|----------|
| `memory.budget-mb` | `MCSERVER_MEMORY_BUDGET_MB` | Maschine minus Reserve |
| `memory.reserve-mb` | `MCSERVER_MEMORY_RESERVE_MB` | 2048 |
| `memory.max-memory-mb` | `MCSERVER_MAX_MEMORY_MB` | aus (Deckel pro Server) |
| `memory.memory-percent` | `MCSERVER_MEMORY_PERCENT` | 100 |

Ablehnen allein verschiebt das Problem nur, deshalb misst derselbe Wächter alle 30 Sekunden, wie
viel jeder Server tatsächlich hält (Resident Size aus `/proc`, also nur unter Linux), und merkt
sich pro Server den höchsten Wert. Im Server Manager gibt es dafür ein eigenes Panel:

- das Budget als Balken, mit vergeben und frei
- wie oft ein Start in den letzten 7 Tagen abgelehnt wurde, und insgesamt
- die Server, die dauerhaft deutlich weniger halten als sie bekommen haben, mit einem konkreten
  Vorschlag ("SURVIVAL: 4096 MB zugewiesen, Spitze 1400 MB, 2048 MB würden reichen - macht 2048 MB
  frei, Platz für eine weitere Runde")

Ein Vorschlag entsteht nur zu einem gemessenen Wert. Wo nicht gemessen werden kann, sagt das Panel
das, statt Zahlen zu erfinden. Umsetzen lässt er sich mit einem Klick auf den Vorschlag selbst —
oder von Hand über den Server im Server Manager. Der neue Wert landet in `servers.<NAME>.memory`
und gilt beim nächsten Start dieses Servers, auch nach einem Neustart des ganzen Netzwerks. Der
laufende Server behält seinen Speicher: der Heap einer JVM steht fest, sobald sie läuft.

Gezählt wird auch, was nur reserviert ist: ein bewilligter Start hält seinen Speicher, bis der
Server dazu wirklich läuft.

## Geld

Die Bits gehören seit dieser Runde dem Launcher, nicht mehr dem Survival-Server. Vorher lagen sie in
`configs/money-config.yml` neben Survival: nur dieser eine Server konnte sie lesen, nur er konnte
sie ausgeben, und ein neu aufgesetzter Survival-Server hätte die Wirtschaft mitgenommen. Jetzt
liegen sie in `money.yml` beim Launcher, so wie die Teams und die Events auch. Beim ersten Start
wird die alte Datei einmal übernommen, danach nie wieder - sonst kämen ausgegebene Bits zurück.

Jede Änderung ist eine Differenz, keine neue Summe, und wird beim Launcher unter einem Lock
angewandt. Zwei Server, die im selben Moment auszahlen, addieren sich damit, statt sich zu
überschreiben. Jede angewandte Änderung wird gemeldet, die Kopien auf den Servern ziehen nach.

`MoneyHandler` auf Survival heißt und verspricht dasselbe wie vorher. Es antwortet aus der lokalen
Kopie, also sofort - eine Schätzung ist das in genau einem Fall, nämlich wenn dasselbe Konto auf
zwei Servern in derselben Sekunde leergeräumt wird. Dann lehnt der Launcher die zweite Änderung ab
und schickt den richtigen Stand hinterher. Für alles, wo an der Antwort etwas Wertvolles hängt,
gibt es `MoneyService.changeBlocking` - der Cosmetic-Kauf geht diesen Weg.

## Cosmetics

Cosmetics sind netzwerkweit und werden mit Bits bezahlt. Zu kaufen gibt es sie im Marktplatz auf
Survival (`/shop`, Knopf in der unteren Reihe) - dieselbe Oberfläche kauft, legt an und legt ab, je
nachdem, wie man zu dem Cosmetic gerade steht.

| Cosmetic | Art | Was es macht |
|----------|-----|--------------|
| Raketen | Sieges-Effekt | Feuerwerk über den Gewinnern. Für alle gratis, das ist der Standard. |
| Tinte | Sieges-Effekt | Von der Bauhöhe regnen Explosionen über die ganze Map - nur Optik, kein Schaden, kein Rückstoß |
| Endlos-Perle | Gadget | Enderperle, die nach dem Cooldown zurückkommt, statt verbraucht zu werden |

Admins verwalten sie im selben Menü über "Verwalten": Linksklick schaltet ein Cosmetic von
verkäuflich über nur besitzbar auf aus, Rechtsklick erhöht den Preis, Shift macht es für alle
gratis. Alles landet beim Launcher in `cosmetics.yml`, zusammen mit dem Besitz.

Der Kauf selbst passiert komplett im Launcher: Preis lesen, Bits abbuchen, Cosmetic gutschreiben -
in einem Schritt unter einem Lock. Auf zwei Server verteilt wären das drei Runden mit zwei Stellen,
an denen es auf halbem Weg schiefgehen kann, und auf halbem Weg heißt: bezahlt und nichts bekommen.

Ein Cosmetic ist zwei Hälften, die sich über eine id treffen. Dem Launcher gehört die Hälfte, die
eine Entscheidung ist (gibt es das, verkauft es sich, für wie viel), dem Spielserver die Hälfte, die
Code ist. Ein neuer Effekt ist deshalb ein Eintrag in `de.hems.types.cosmetic.Cosmetics` und eine
Klasse, die `WinEffect` implementiert - dazwischen darf jede Seite der anderen voraus sein.

## Discord-Verknüpfung

Ein Minecraft-Name ist alles, was man von jemandem hat, wenn er auffällt. Die Verknüpfung macht daraus
eine Person, die man auch anschreiben kann.

| Wo | Befehl | Was er macht |
|----|--------|--------------|
| Discord | `/verify <minecraftname>` | Gibt dir einen Code, sechs Zeichen, zehn Minuten gültig |
| Im Spiel | `/verify <code>` | Verknüpft die beiden Accounts |
| Im Spiel | `/verify` | Zeigt, mit welchem Discord du verknüpft bist |
| Im Spiel | `/verify wer <spieler>` | Sagt, wer das auf Discord ist (Op oder `network.verify.lookup`) |
| Discord | `/unlink <minecraftname>` | Löst eine Verknüpfung (nur der Besitzer) |

Zwei Schritte, weil ein Schritt nichts wert wäre: auf Discord kann jeder jeden Namen eingeben, und den
Code zurücktippen kann nur, wer wirklich als dieser Account eingeloggt ist. Eine Liste, in der auch
gelogen sein könnte, ist schlechter als keine — der ganze Zweck ist ja zu wissen, wen man anschreibt.

Der Code lebt nur im Speicher. Ein Neustart des Launchers mitten im Verknüpfen kostet einen Befehl.
Gespeicherte Verknüpfungen liegen in `links.yml` beim Launcher und sind auf jedem Server sofort da.

### Operator-Rechte

| Befehl | Wer darf |
|--------|----------|
| `/op <minecraftname>` | Nur der Besitzer |
| `/deop <minecraftname>` | Nur der Besitzer |

Der Name landet in `ops` in der `main-config.yml` — derselben Liste, aus der jeder neue Server gebaut
wird — und die Änderung wird ans Netzwerk gemeldet, sodass laufende Server sie sofort übernehmen und in
ihre eigene `ops.json` schreiben. Kein Neustart nötig.

Wer der Besitzer ist, steht unter `discord-owner-id` in der `main-config.yml`. Der Wert war vorher fest
im Code von `/payingplayer` verdrahtet; er ist jetzt an einer Stelle und änderbar, ohne neu zu bauen.
Operator ist jedes Recht, das es gibt — deshalb hängt das bewusst am Besitzer und nicht an einer
Discord-Rolle.

## Module

| Modul | Inhalt |
|-------|--------|
| `ServerLauncherApplication` | Startet und konfiguriert die Server, Discord Bot, Admin Website |
| `CommonCode` | Netzwerk-Events, `ServerApi`, Server Manager UI, Warp System, Geld, Runden, Cosmetics |
| `LobbyPlugin` | Lobby, Parkour, Server Manager, eigene Runden |
| `Survival` | Survival Spielmodus |
| `Bedwars` | Bedwars Minispiel |
| `BackpackPlugin` | Geteilter Team-Rucksack |
| `VelocityPlugin` | Meldet neue Server am laufenden Proxy an |
