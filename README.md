# Minecraft Server setup

Ein Minecraft Netzwerk aus einem Velocity Proxy und beliebig vielen Paper Servern, die vom
`ServerLauncherApplication` gestartet, konfiguriert und verwaltet werden.

## Versionen

Alles läuft auf **Minecraft 26.3** (Stand 2026-09-28, jeweils die neueste Version):

| Teil | Version | Status |
|------|---------|--------|
| Paper | 26.3 (build 140) | **BETA** — neuester Build vom 29.09., einen stabilen gibt es für 26.3 noch nicht |
| Velocity | 4.2.0 (build 30) | stabil, neue Hauptversion (Config 2.9, API 4) |
| WorldEdit | 7.4.6-beta-02 | **Beta** — das einzige WorldEdit für 26.3 |
| WorldGuard | 7.0.19 | Release, für 26.3 freigegeben |
| CoreProtect | 25.0 | erster Build für 26.3, **nicht öffentlich** - kommt über einen Dropbox-Link |
| Chunky | 1.5.3 | Release, für 26.3 freigegeben |
| Simple Voicechat | 2.6.24 (Paper) / 2.6.18 (Velocity) | Release / neuestes Proxy-Plugin |

**CoreProtect 25.0** gibt es weder auf Modrinth noch auf `maven.playpro.com` (beide enden bei 24.1, das
sich auf 26.3 selbst abschaltet). Der Launcher lädt es deshalb über einen Dropbox-Link in `FileType`.
**Wird der Link gelöscht oder läuft er ab, scheitert der Download** - dann einen neuen Link eintragen.
Gebaut wird weiter gegen die API von 24.1 aus Maven; alles, was der Code davon aufruft, gibt es in 25.0
unverändert (API-Version 13, mit dem 25.0-Jar kompiliert). Geprüft auf Paper 26.3 Build 134 und 140: startet,
`/co status` antwortet, fährt sauber herunter.

**Unsere Plugins schreiben selbst nach CoreProtect** (`de.hems.paper.CoreProtectLog`), wo sie die
Welt an CoreProtect vorbei ändern: der Shop-Kauf nimmt Items aus der Lagerkiste (steht unter dem Käufer),
der Erntehelfer pflanzt neu (unter dem Spieler, die Ernte selbst loggt CoreProtect über das
`BlockBreakEvent`). Geprüft auf Paper 26.3 mit 25.0: `/co lookup` zeigt beide Einträge. Bedwars-,
Hunger-Games-, Casino- und Speedrun-Server werden nach der Runde gelöscht, ihre Welt ändert unser Code
dort bewusst ohne Log. Ohne CoreProtect tun die Aufrufe nichts.

25.0 speichert standardmäßig in **DuckDB** (`database.duckdb`), nicht mehr in SQLite. Eine alte
`database.db` wird nicht von selbst übernommen; dafür gibt es `/co migrate-db`. Wer lieber bei SQLite
bleibt, setzt `database-type: sqlite` in `plugins/CoreProtect/config.yml`. Beim ersten Start lädt Paper
die DuckDB-Bibliothek von Maven Central nach.

Paper braucht **Java 25** - sowohl zum Bauen als auch zum Starten. Die Downloads stehen alle in
`CommonCode/src/main/java/de/hems/types/FileType.java`, ein Update ist also ein Update dieser einen Datei
(plus `paper-api` in den poms und `api-version` in den `plugin.yml`).

### Backup vor einem Versionswechsel

Eine neue Minecraft-Version wandelt jede Welt beim ersten Laden um, und zurück geht es nicht. Der Launcher
erkennt den Wechsel am alten Paper-Jar im Serververzeichnis (`paper-26.2-…` → `paper-26.3-…`) und kopiert
**vor dem Start** jeden Weltordner dieses Servers nach
`./backups/<SERVER>/<alte Version>-<Zeitstempel>/`. Schlägt das fehl, startet der Server nicht. Ein neuer
Build derselben Version löst kein Backup aus. Die Backups werden nie automatisch gelöscht — bei großen
Welten auf den Plattenplatz achten.

### Velocity 4

Velocity 4 hat das Format der `velocity.toml` geändert (`config-version = "2.9"`, `ping-passthrough`
ist jetzt eine Tabelle). Der Launcher schreibt die Datei in diesem Format; geprüft durch einen Start des
echten Velocity-4.2.0-Jars mit der erzeugten Datei.

## Bauen und starten

Auf einem neuen Rechner reicht:

```bash
./install.sh
```

Das Skript prüft Java 25, tmux und git (und installiert sie auf Wunsch mit apt), baut alles und fragt
dann ab, was der Launcher braucht: Discord-Token und Besitzer-ID, Modus und Adresse des Proxys, die Domain für Spieler, Ops,
Whitelist, Autostart, die Admin-Website mit Account und Google Authenticator (als QR-Code im Terminal
zum Scannen), und den Arbeitsspeicher.
Das Velocity-Secret und das Secret für `/command` werden erzeugt. Alles landet in der `main-config.yml`,
Java und Speicher in der `.env.local`; danach startet es das Netzwerk. Nochmal ausgeführt zeigt jede Frage
den aktuellen Wert, Enter behält ihn - so lässt sich einzeln etwas ändern, etwa ein neuer Token.

Von Hand:

```bash
./mvnw clean install     # baut alle Plugins nach ./builds/
./start.sh               # startet den Launcher in einer tmux Session
```

Der Launcher startet zuerst den Proxy und danach die Server aus `autostart` in der `main-config.yml`
(Standard: `LOBBY` und `SURVIVAL`).

**Testen:** Wie man das Netzwerk lokal startet und Features mit echten, per Skript gesteuerten Clients prüft
(inkl. Rezepten für jedes Feature), steht in [`testing/README.md`](testing/README.md).

## Neustart und Updates

`/neustart` startet das ganze Netzwerk zu einer festen Zeit neu - von jedem Server aus, nur für Ops.

| Befehl | Was passiert |
|--------|--------------|
| `/neustart` | Zeigt, was geplant ist, und wie das letzte Update lief |
| `/neustart 10` | In 10 Minuten neu starten, auf dem Code, der da ist |
| `/neustart 10 update` | In 10 Minuten: `git pull`, bauen, neu starten |
| `/neustart 10 aus` | In 10 Minuten herunterfahren, danach bleibt alles aus |
| `/neustart abbrechen` | Den geplanten Neustart absagen |

Ein neuer `/neustart` ersetzt einen geplanten. Jeder Server zählt selbst herunter: beim Planen und bei 10,
5, 3, 2 und 1 Minute und 30 Sekunden eine Chatzeile und ein Titel („In 10 Minuten wird der Server neu
starten“), eine Bossbar in den letzten 5 Minuten, ein Sekunden-Countdown als Titel in den letzten 10
Sekunden. Wer während eines geplanten Neustarts joint, bekommt Titel und Chatzeile zwei Sekunden nach dem
Join.

**Wenn es so weit ist:** alle Spieler werden mit Hinweis gekickt, Spieler und Welten gespeichert, jeder
Server gestoppt - und der Launcher **wartet, bis jeder Java-Prozess wirklich weg ist**. Ein Server
schließt beim Stoppen zuerst seinen Port und speichert danach; wer auf den Port schaut, startet das
Netzwerk auf halb geschriebene Welten. Nach drei Minuten wird ein hängender Server hart beendet. Dann
beendet sich der Launcher mit einem Code, den `run.sh` liest:

| Code | `run.sh` macht |
|------|----------------|
| 10 | Launcher wieder starten |
| 11 | `git pull --ff-only`, bauen, Launcher starten |
| sonst | nichts - das Netzwerk bleibt aus (0 nach `/neustart … aus`, alles andere ist ein Absturz) |

**Ein Update kann das Netzwerk nicht ausgeschaltet lassen.** Scheitert `git pull`, startet der alte
Stand. Baut der neue Stand nicht, setzt `run.sh` auf den vorigen Commit zurück, baut den und startet ihn.
Liegen im Arbeitsverzeichnis eingecheckte Dateien mit lokalen Änderungen, wird gar nicht gepullt - ein
Zurücksetzen würde sie sonst wegwerfen. Was passiert ist, steht in `update-result.txt` und unter
`/neustart`.

`start.sh` startet `run.sh` in der tmux-Sitzung `server`. **Einmalig:** eine Sitzung, die noch mit dem
alten `start.sh` läuft, muss einmal von Hand beendet und neu gestartet werden, damit `run.sh` läuft.

## Chunks vorladen (Chunky)

Chunky ist auf **jedem** Paper-Server installiert (es gehört zu den Basis-Plugins aller Vorlagen) und
läuft auf 26.3 - geprüft auf Build 134 und 140: 441 Chunks in 14 Sekunden. Vorladen spart Lag, wenn Spieler
zum ersten Mal in neue Gegenden kommen; am sinnvollsten auf Survival, vor einem End-Event auch für das
End.

| Befehl (Op oder Konsole) | Was er macht |
|--------------------------|--------------|
| `/chunky world <welt>` | Welt wählen, z.B. `world`, `world_nether`, `world_the_end` |
| `/chunky center <x> <z>` · `/chunky radius <blöcke>` | Mitte und Radius des Gebiets |
| `/chunky worldborder` | Genau das Gebiet innerhalb der Weltgrenze |
| `/chunky start` | Loslegen. Fortschritt steht in der Konsole |
| `/chunky pause` · `/chunky continue` · `/chunky cancel` | Anhalten, weitermachen, abbrechen |
| `/chunky progress` | Stand abfragen |

Ohne Spiel geht es über das Konsolen-Panel der Website. Ein Lauf überlebt einen Neustart: nach dem
Start mit `/chunky continue` weitermachen. Vorladen kostet CPU - während viel los ist, lieber
pausieren.

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

**Passwort vergessen:** `./admin-passwort.sh` zeigt die Accounts und setzt für einen davon ein neues
Passwort (eingetippt oder zufällig erzeugt), auf Wunsch auch einen neuen 2FA-Schlüssel mit QR-Code. Das alte
Passwort lässt sich nicht anzeigen, gespeichert ist nur der Hash. Das geht auch, während das Netzwerk läuft:
der Launcher liest die Accounts beim nächsten Login neu aus der Datei.

### Panels

| Panel | Was es kann |
|-------|-------------|
| Server | Zeigt welche Server an sind, und schaltet sie an, aus oder neu |
| Paying Player | Trägt zahlende Spieler per Minecraft-Name oder UUID ein und aus |
| Tickets | Liest und beantwortet Tickets, übernimmt, schließt und öffnet sie wieder (siehe [Tickets](#tickets)) |
| Konsole | Zeigt die Ausgabe eines Servers live an und schickt Befehle an ihn |
| Whitelist | Regeln für die öffentliche Seite `/regeln`, Whitelist an/aus, wer sich selbst eingetragen hat (siehe [Whitelist und Regeln](#whitelist-und-regeln)) |
| Netzwerk | `/neustart` aus dem Browser (Neustart, Update, Herunterfahren, Absagen), der laufende Commit, das letzte Update und das Speicherbudget mit Vorschlägen |
| Einstellungen | Eigenes Passwort ändern, Google Authenticator neu einrichten (QR-Code), Admin-Accounts anlegen und löschen, Besitzer-ID, Name der Seite, Ops, Whitelist und Autostart |

Alles in den Einstellungen, was jemanden aussperren oder hereinlassen kann - Passwort, 2FA, Accounts -
braucht zusätzlich das eigene Passwort. Die Prüfung läuft mit derselben Grace Period wie der Login und pro
Account nacheinander, damit ein gestohlenes Session-Cookie kein Weg zum Passwort-Raten wird. Ein neuer
2FA-Schlüssel gilt erst, wenn ein Code daraus eingegeben wurde; bis dahin bleibt der alte. Nach einem
Passwortwechsel werden alle anderen Sitzungen des Accounts beendet, ein gelöschter Account wird überall
abgemeldet.

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
`67`, sondern das aus `web.command-secret`, das beim ersten Start (oder von `./install.sh`) erzeugt wird.

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
| `/cteam grenze` | Zeichnet die Chunk-Grenzen um dich herum zehn Sekunden lang mit Partikeln |
| `/cteam titel` | Schaltet den Titel beim Betreten eines Claims für dich an und aus |

### Wem gehört der Boden hier

Claims sind unterwegs sichtbar, nicht nur auf Befehl:

- **Titel beim Betreten.** Wer die Grenze überschreitet, bekommt den Teamnamen in Teamfarbe
  eingeblendet, darunter „Betreten" - beim Schritt zurück ins Freie steht dort „Wildnis" und
  „Verlassen". Ausgelöst wird das vom Wechsel des **Besitzers**, nicht vom Chunkwechsel: quer über ein
  Teamgebiet zu laufen überquert alle sechzehn Blöcke eine Chunkgrenze, und ein Titel bei jeder davon
  wäre der Grund, das Ganze wieder abzuschalten. Wer ihn trotzdem nicht will, schaltet ihn mit
  `/cteam titel` ab - oder über den Knopf im Team-Manager neben der Team-Übersicht. Die Entscheidung
  hängt am Spieler, nicht am Server: Geschmackssache, und Geschmack ist pro Spieler verschieden. Der
  Name über der Hotbar bleibt dabei stehen.
- **Actionbar währenddessen.** Solange jemand auf fremdem oder eigenem Boden steht, steht der
  Teamname über der Hotbar, beim eigenen Team mit dem Zusatz „dein Team". In der Wildnis steht dort
  nichts - eine leere Zeile ist dort die richtige Antwort und hält die Anzeige aus dem Weg.
- **`/cteam grenze`.** Zeichnet die Kanten des eigenen Chunks und der angrenzenden Claims in
  Teamfarbe, zehn Sekunden lang, und nur für den, der gefragt hat. Der Chunk, in dem man selbst
  steht, ist immer dabei - weiß, wenn er noch frei ist.
- **`/cteam chunks`.** Die Karte zeigt eine Farbe **pro Team** statt nur grün und rot, in der Mitte
  einen Pfeil in Blickrichtung, und darunter eine Legende mit den Teams, die gerade zu sehen sind.
  Norden ist oben. Der Mauszeiger über einem Feld nennt Team und Koordinaten.

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

Das Plugin **verbindet sich selbst** mit dem Netzwerk, auch wenn auf demselben Server schon ein anderes
Plugin verbunden ist. Jedes Plugin trägt seine eigene Kopie des gemeinsamen Codes im Jar, und die
Verbindung des Nachbarn liegt damit in einem Klassenraum, den es gar nicht sehen kann. Ohne eigene
Verbindung kämen die Teams nie an, und `/backpack` würde bis zum Serverneustart „die Teams sind noch nicht
geladen" antworten - lautlos, denn die Aktualisierung bricht in dem Fall ohne Meldung ab. Der Server steht
dann zweimal im JGroups-Cluster; das kostet einen Teilnehmer mehr und sonst nichts, weil Events ohnehin an
alle gehen und jede Verbindung nur mit ihren eigenen Handlern antwortet.

Der Cluster läuft über **TCP auf 127.0.0.1** (`CommonCode/src/main/resources/mcserver-jgroups.xml`, Ports ab
7800), nicht über das Multicast, das JGroups von Haus aus nimmt: das verwerfen viele Hoster, und dann steht
jeder Server allein in seinem Cluster - jede Frage an den Launcher endet mit „Der Host antwortet nicht“
(`/verify`, `/servermanager`, Geld, Lotto, …). Ob alle sich sehen, steht beim Start im Log:
`[JGroups] Connected as 'LOBBY' … View=… [HOST, VELOCITY, LOBBY]`. Die `jgroups.*`-System-Properties
überschreiben die Datei, etwa für ein Netzwerk über mehrere Rechner.

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

### Die Ablage tragen: `/admin join`

`/admin join` auf Survival macht aus einem Admin den **Admin**: der Chat meldet, dass der Spieler
gegangen ist, gleich darauf, dass „Admin" gekommen ist, und ab da heißt er überall so - über dem
Kopf, in der Tabliste, im Chat - mit dem Skin dazu. Nochmal getippt geht es andersherum zurück. Wie
er heißt und welchen Skin er trägt, steht in `./configs/admin-join.yml`: `display-name`, und der Skin als
signierte Textur in `skin-value`/`skin-signature` (Standard ist der mitgelieferte Admin-Skin; eine eigene
Textur bekommt man z.B. über mineskin.org). Ist `skin-value` leer, wird der Skin von `skin-account` beim
Serverstart einmal bei Mojang geholt. Ohne geladenen Skin
passiert gar nichts - der richtige Name über dem falschen Gesicht wäre genau das Merkmal, an dem
man ihn erkennt.

Sein Inventar in dieser Gestalt **ist die Ablage**. Sie wird dabei nicht kopiert, sondern
ausgeliehen: beim Umschalten wird sie beim Launcher geleert und liegt in seinen Taschen, beim
Zurückschalten wird sie von dort zurückgeschrieben. Anders ginge es nicht - zwei Admins mit
derselben Ablage in der Tasche wären zwei Mal dieselben Diamanten. Solange jemand sie trägt, sagt
`/admin` das mit Namen, und ein zweiter Admin kann sich nicht verkleiden. Passen die Sachen nicht in
36 Stapel, lehnt der Befehl ab, statt den Rest fallenzulassen.

Auch der Ort gehört zur Tarnung. Beide Gestalten haben ihre eigene letzte Position: der Admin
taucht dort auf, wo der Admin zuletzt stand, der Spieler dort, wo der Spieler zuletzt stand, und
keiner von beiden erscheint da, wo der andere gerade verschwunden ist. Ohne das verrät sich die
Verkleidung in einer Zeile Chat - einer geht, einer kommt, beide am selben Zaunpfahl. Gibt es für
die Admin-Gestalt noch keinen Ort - beim ersten Mal -, ist es der Spawn, also genau das, was ein
echter Join tut.

Sein eigenes Inventar liegt so lange in `plugins/survival/admin-join.yml`, nicht im Speicher: ein
Neustart mitten in der Verkleidung darf niemandem seine Sachen kosten. Nach dem Neustart ist er
weiterhin der Admin und bekommt die Gestalt beim Join wieder aufgesetzt. Stirbt er in der
Verkleidung, behält er das Inventar - es gehört ihm nicht, es ist die Ablage.

Die uuid bleibt die eigene. Geld, Teams, Cosmetics und Bans wissen also weiterhin, wer da steht,
und das Adminabuse-Log bekommt bewusst den echten Namen: ein Log, in dem alles „Admin" getan hat,
beantwortet die einzige Frage nicht, für die es existiert.

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
der Kopie. **Achtung seit 26.1:** eine Zusatzwelt liegt nicht mehr neben der Hauptwelt, sondern als
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

Mit der `BEDWARS`-Vorlage kommen vier Hypixel-Maps:

| Map | Teams | Modi | Generatoren in der Mitte |
|-----|-------|------|--------------------------|
| Speedway | 8 | Solo, Doppel, 3er, 4er | 4 Diamant, 4 Smaragd |
| Lighthouse | 8 | Solo, Doppel, 3er, 4er | 4 Diamant, 4 Smaragd |
| Orbit | 8 | Solo, Doppel, 3er, 4er | 4 Diamant, 4 Smaragd |
| Aquarium | 4 | 3er, 4er | 4 Diamant, 2 Smaragd |

Lighthouse, Orbit und Aquarium stammen aus der Sammlung
[Odsodium/Hypixel-Bedwars-Maps](https://github.com/Odsodium/Hypixel-Bedwars-Maps) (Mitschnitte vom
Hypixel-Server). Die Punkte stehen nicht von Hand in den `.yml`, sondern sind aus der Welt gelesen:
Bett und Teamfarbe (Wolle um das Bett), Spawn hinter Kiste und Endertruhe, Team-Generator auf den sechs
Steinziegelstufen bzw. im Gitterkäfig am Ende der Basis, Händler in den Seelaternen-Nischen unter einer
Barriere (Orbit: links und rechts vom Spawn), und jeder Diamant-/Smaragd-Generator ist der einzelne
Edelsteinblock unter einem Ring mit Loch. Die Warteplattform ist Hypixels eigene über der Mitte.

Geprüft auf Paper 26.3 mit dem Bedwars-Plugin selbst: jede Map lädt, besteht den Map-Validator für
jeden ihrer Modi, an jeder Bett-Stelle liegt ein Bett, jeder Spawn und Händler hat Boden unter und
zwei freie Blöcke über sich, jeder Generator ist frei und hat Boden. **Nicht** geprüft ist, wie sich
die Maps spielen - ein Händler, der ungünstig steht, lässt sich mit `/bw setup` versetzen.

Bekannte Eigenheit: bei Aquarium war das Loch über einem Diamant-Generator im Mitschnitt mit einem
Diamantblock zugesetzt; der Generator steht dort einen Block höher.

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

## Tabliste

Jeder Server hat dieselbe Tabliste (`de.hems.paper.tablist.TabList`) mit dem Inhalt seines Spielmodus:

- **Kopf:** Netzwerkname, Spielmodus in seiner Farbe, wie viele im ganzen Netzwerk online sind (alle
  10 Sekunden vom Proxy geholt) und wie viele davon hier.
- **Fuß:** was der Modus zu sagen hat, darunter das laufende oder nächste Event, die wichtigsten Befehle
  und für alle Servername, TPS und Tickzeit.

| Modus | Eigene Zeilen | Namen in der Liste |
|-------|---------------|--------------------|
| Lobby | Bits | unverändert |
| Survival | Bits, Team mit Tag, Mitgliedern und Chunks | unverändert (Team-Tag kommt vom Scoreboard) |
| Bedwars | Map; vor dem Start Wartende; danach eigenes Team und Bett, Teams im Spiel, Kills/Finals/Betten | in Teamfarbe, Zuschauer grau |
| Hunger Games | Phase, wie viele noch leben | Ausgeschiedene grau |
| Casino | Bits | unverändert |
| Speedrun | - | unverändert |

Ein neuer Modus ist eine Klasse, die `TabContent` implementiert (oder ein `SimpleTab`), und eine Zeile
`TabList.init(this, …)` im `onEnable`. Der Netzwerkname steht in `TabList.NETWORK_NAME`.

## Shops (Survival)

Ein Team stellt einen Händler (Villager) auf eigenen Boden, eine Kiste daneben ist sein Lager.
`/shop create <name>` (oder `/shopkeeper <name>`) auf der Kiste stehend legt ihn an und kostet den
Spieler 2000 Bits (`create-cost` in `configs/shop.yml`, 0 = kostenlos). Abgebucht wird erst, wenn Kiste,
Team und Chunk passen. Rechtsklick auf den Händler öffnet den Laden,
Schleich-Rechtsklick für das Team die Einstellungen: Kiste wechseln, Händler versetzen, Angebote, Preise
und Mengen. `/shop` öffnet den Marktplatz über alle Shops.

| Klasse | Aufgabe |
|--------|---------|
| `Shopkeeper` | Ein Shop: Villager, Lager, Verkauf - sonst nichts |
| `ShopkeeperManager` | Alle Shops: finden (nach Id, nach Kiste), anlegen, Autosave, folgt Team-Umbenennungen |
| `ShopkeeperStore` | Liest und schreibt `configs/shop-config.yml` (Format unverändert) |
| `ShopSettings` | `configs/shop.yml`: was ein Shop kostet |
| `ShopOwnership` | Wem ein Shop gehört und wo er stehen darf - über den `TeamService`, nicht über das Scoreboard |
| `ShopUi` · `ShopEditorUi` | Laden für Kunden, Einstellungen für das Team |
| `ShopkeeperListener` · `ShopChestListener` · `ShopkeeperChunkListener` | Klicks, Schutz der Lagerkiste, Villager beim Laden des Chunks |

Wird ein Team umbenannt, meldet der Launcher den alten Namen mit (`TeamUpdatedEvent.getPreviousName()`),
und die Shops ziehen mit. Vorher hat eine Umbenennung jeden Shop des Teams verwaist.

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

**Teamkassen gehen nicht verloren.** Ein Teamkonto hängt am Teamnamen. Früher blieb das Geld bei
einer Umbenennung unter dem alten Namen liegen, und bei einer Auflösung lag es herrenlos herum - ein
neues Team mit demselben Namen hat es dann geerbt. Jetzt zieht der Launcher das Konto bei einer
Umbenennung mit auf den neuen Namen (wie den Rucksack), und bei einer Auflösung bekommt der Anführer
den ganzen Kontostand; das Teamkonto wird danach gelöscht. Beides meldet er ins Netzwerk, die Kopien
auf den Servern stimmen sofort. Geprüft durch `MoneyMoveCheck`.

Geld, das vor dieser Änderung schon unter einem alten Teamnamen liegen geblieben ist, wird nicht
automatisch zugeordnet - welcher Name zu welchem Team gehörte, weiß der Launcher nicht mehr. Es steht
in `money.yml` unter `balances.<alter Name>` und lässt sich dort von Hand umtragen.

## Cosmetics

Cosmetics sind netzwerkweit und werden mit Bits bezahlt. Zu kaufen gibt es sie mit `/cosmetics` auf
jedem Server - Lobby, Bedwars, Survival - und zusätzlich über den Knopf im Marktplatz auf Survival
(`/shop`). Dieselbe Oberfläche kauft, legt an und legt ab, je nachdem, wie man zu dem Cosmetic
gerade steht.

| Cosmetic | Art | Was es macht |
|----------|-----|--------------|
| Raketen | Sieges-Effekt | Feuerwerk über den Gewinnern. Für alle gratis, das ist der Standard. |
| Tinte | Sieges-Effekt | Von der Bauhöhe regnen Explosionen über die ganze Map - nur Optik, kein Schaden, kein Rückstoß |
| Gewitter | Sieges-Effekt | Blitze um die Gewinner herum, ohne Feuer und ohne Schaden |
| Lichtsäule | Sieges-Effekt | Eine Säule aus Licht aus jedem Gewinner heraus bis über die Map |
| Blitzschlag | Kill-Effekt | Ein Blitz da, wo der Gegner gefallen ist |
| Seelen | Kill-Effekt | Die Seele des Gegners steigt langsam auf |
| Stichflamme | Kill-Effekt | Ein Ring aus Feuer um den Gefallenen |
| Flammenspur | Partikelspur | Flammen hinter dem Träger, solange er läuft |
| Sternenstaub | Partikelspur | Helle Funken, die langsam absinken |
| Noten | Partikelspur | Bunte Noten über dem Kopf |

Dazu die Gadgets. Sie stehen in derselben Liste, haben aber eine Spalte mehr: wo sie wirken.

| Gadget | Wirkt in | Was es macht |
|--------|----------|--------------|
| Endlos-Perle | Lobby, Survival | Enderperle, die nach dem Cooldown zurückkommt, statt verbraucht zu werden. Im Lobby-Parkour bricht ein Wurf den Lauf ab |
| Enterhaken | Lobby, Survival | Angel, die den Träger dorthin zieht, wo der Haken gelandet ist |
| Doppelsprung | Lobby | Zweiter Sprung in der Luft, weiche Landung |
| Raketenstiefel | Lobby | Rechtsklick wirft nach oben, runter geht es langsam |
| Schneeball-Kanone | Lobby | Schneebälle, die wegschubsen und niemandem wehtun |
| Disco-Boden | Lobby | Der Boden leuchtet - als Paket an die Umstehenden, die Welt bleibt, wie sie ist |
| Fußspuren | Lobby | Abdrücke links und rechts, wo der Träger langgeht |
| Sprungpad | Lobby | Ein Pad zum Hinlegen, das jeden hochwirft und nach zwei Minuten weg ist |
| Reittier | Lobby | Ein Pferd auf Zuruf, gesattelt und gezähmt |
| Erntehelfer | Survival | Rechtsklick erntet reif und pflanzt neu, abzüglich des Saatguts |
| Sitzen | Survival | Hinsetzen auf Treppen und Stufen |
| Werkbank | Survival | Eine Werkbank überall, ohne Amboss und ohne Ofen |
| Haustier | Lobby, Survival | Ein kleines Tier, das hinterherläuft |
| Ballon | Lobby, Survival | Ein Ballon an einer Schnur über dem Kopf |
| Konfetti-Kanone | Lobby, Survival | Rechtsklick, und es regnet Farbe |
| Eigenes Wetter | Lobby, Survival | Eigene Tageszeit und eigenes Wetter, nur im eigenen Client |
| Chat-Blase | Lobby, Survival | Die eigene Chatnachricht steht kurz über dem Kopf |
| Emotes | Lobby, Survival | Menü mit Gesten, die alle in der Nähe sehen |

Es gibt vier Arten: Sieges-Effekt, Kill-Effekt, Partikelspur, Gadget. Von den ersten dreien trägt
man höchstens eine, und sie sind Bilder - sie laufen auf jedem Server. Gadgets nicht: sie greifen
ins Spiel ein, also schaltet jeder Spielmodus sie einzeln frei (`Gadgets.setGuard`) und sagt dabei,
welcher Slot er ist.

Slots sind der Grund, warum ein Spieler mehr als ein Gadget tragen kann: einen für Lobby, einen für
Survival, einen für Bedwars. Ohne das würde der Doppelsprung in der Lobby den Erntehelfer auf
Survival ablegen. Ein Gadget sagt selbst, in welche Slots es gehört (`Gadget.slots()`), und ein
Klick im Menü legt es in alle davon gleichzeitig an - wer den Enterhaken kauft, will den Enterhaken
und keine Entscheidung über Server. Gespeichert wird es beim Launcher unter `GADGET_<SLOT>`; eine
Auswahl aus der Zeit vor den Slots gilt weiter, bis sie einmal geändert wird.

Was ein Gadget in der Welt hinterlässt, nimmt es auch wieder mit. Wer es ablegt, wer die Lobbywelt
verlässt und wer sich ausloggt, verliert dabei sein Gadget-Item und alles, was es gespawnt hat -
Ballon, Haustier, Pferd, Sitz. Ohne das füllt sich eine Lobby über ein Wochenende mit Tieren, deren
Besitzer seit drei Neustarts weg sind.

Partikelspuren zeichnen nichts für Zuschauer und nichts für unsichtbare Spieler. Das Zweite ist
kein Detail: eine gekaufte Spur, die einen unsichtbaren Bedwars-Spieler verrät, wäre eine, die
niemand anlegt.

Admins verwalten sie im selben Menü über "Verwalten": Linksklick schaltet ein Cosmetic von
verkäuflich über nur besitzbar auf aus, Rechtsklick erhöht den Preis, Shift macht es für alle
gratis. Alles landet beim Launcher in `cosmetics.yml`, zusammen mit dem Besitz.

Der Kauf selbst passiert komplett im Launcher: Preis lesen, Bits abbuchen, Cosmetic gutschreiben -
in einem Schritt unter einem Lock. Auf zwei Server verteilt wären das drei Runden mit zwei Stellen,
an denen es auf halbem Weg schiefgehen kann, und auf halbem Weg heißt: bezahlt und nichts bekommen.

Ein Cosmetic ist zwei Hälften, die sich über eine id treffen. Dem Launcher gehört die Hälfte, die
eine Entscheidung ist (gibt es das, verkauft es sich, für wie viel), dem Spielserver die Hälfte, die
Code ist. Ein neuer Effekt ist deshalb ein Eintrag in `de.hems.types.cosmetic.Cosmetics`, eine
Klasse, die `WinEffect`, `KillEffect`, `TrailEffect` oder `Gadget` implementiert, und eine Zeile in
`CosmeticEffects.init` - dazwischen darf jede Seite der anderen voraus sein. Im Verwalten-Menü steht
bei jedem Eintrag, ob dieser Server Code dafür hat.

Den Katalog holt sich jeder Server alle fünf Minuten. Wem was gehört, holt er sich dagegen einzeln:
beim Join des Spielers, und eine Minute nach dessen Quit wirft er es wieder weg. Vorher reiste der
Besitz **aller** Spieler mit dem Katalog mit, was mit jedem Spieler wächst, der jemals etwas gekauft
hat - für die zwanzig Leute, die gerade auf dem Server stehen.

## Pokernacht

Ein Event-Typ, bei dem um die Bits des Netzwerks gepokert wird. Ein Admin legt ihn im Kalender an
(`/events` → Neues Event → Typ `Pokernacht`), stellt Einsätze und Hausanteil ein, und ein paar Minuten
vor der Zeit fährt die Lobby einen Casino-Server hoch und macht ihn auf. Niemand wird hinübergezogen:
das Casino wird angekündigt, wer spielen will geht selbst hin. Eine Bedwars-Runde *startet*, eine
Pokernacht *öffnet*.

| Einstellung | Was sie macht |
|-------------|---------------|
| Format | Cash Game oder Turnier |
| Buy-in | Was ein Einkauf kostet — **ein Bit ist ein Chip**, es gibt keinen Kurs |
| Blinds | Small Blind, der Big Blind ist doppelt so groß |
| Hausanteil | Was das Haus pro Pot behält, mit Deckel in Big Blinds |
| Tische / Plätze | Wie viele Tische das Casino hat und wie viele Stühle pro Tisch |
| Bots | Ob Spieler welche setzen dürfen, und was die Gebühr kostet |
| Wertung | Wie viele Hände und wie viel Einsatz es für die Rangliste braucht |

### Gespielt wird um echtes Geld

Ein Chip ist ein Bit, eins zu eins, und zwar bewusst: bei einem Kurs muss jeder am Tisch rechnen, was
eine Erhöhung ihn kostet, und beim ersten Rechenfehler ist echtes Geld weg.

Der Weg zwischen Bits und Chips ist genau eine Klasse (`Bank`). Der Einkauf **wartet** auf die
Bestätigung des Launchers, bevor Chips existieren — andersherum wäre ein Platz mit Chips, für die nie
bezahlt wurde, und ein Absturz im falschen Moment macht daraus Geld aus dem Nichts. Die Auszahlung
wartet nicht: eine Gutschrift kann nicht an fehlender Deckung scheitern.

**Was ein Absturz kostet.** Den Abend, nie das Geld. Nach jeder Hand meldet der Casino-Server dem
Launcher, wie viele Chips vor wem liegen. Stirbt er mitten im Spiel, zahlt der Launcher genau daraus
zurück, wenn die Nacht abgerechnet wird. Dasselbe gilt, wenn ein Admin ein laufendes Event löscht: die
Chips gehen zuerst zurück, die Zeilen erst danach.

### Rangliste

Gewertet wird, **was unterm Strich gewonnen wurde**: alles was vom Tisch kam, minus alles was drauf
ging. Chips, die noch vor jemandem liegen, zählen mit — wer vorn liegt und weiterspielt, liegt vorn.

Vorher war es das Verhältnis raus/rein, und das hatte ein Loch, das sich mit Schwellen nicht wirklich
zumachen lässt: ein Verhältnis belohnt den kleinsten Einsatz, also gewinnt den Abend, wer möglichst
wenig riskiert — das Gegenteil einer Pokernacht. Beim Gewinn gibt es das Loch nicht: oben steht, wer
anderen Geld abgenommen hat, und das ist das Spiel.

Der Preis dafür ist echt und soll hier stehen: es belohnt, größer zu spielen. Wer 5000 aus einem
50000er Buy-in gewinnt, steht vor dem, der 4000 aus 1000 gewinnt, obwohl der Zweite den besseren Abend
hatte. So rechnet ein Casino, und das Verhältnis steht auf dem Board daneben für den, der die andere
Geschichte sehen will.

Eine Schwelle bleibt: **Mindesthände**. Ein großer Pot ist ein echter Gewinn und trotzdem noch keine
Pokernacht. Wer sie nicht hat, steht unter der Wertung mit der Angabe, was fehlt, statt einfach zu
fehlen. Den Mindesteinsatz gibt es weiterhin als Einstellung, steht aber standardmäßig auf aus — er war
nur für die Verhältnis-Wertung nötig. Belohnungen werden wie bei jedem anderen Event über den Knopf
**Belohnungen** hinterlegt (siehe [Belohnungen](#belohnungen)). Platz 1 ist der größte Gewinn; wer unter
den Mindesthänden bleibt, hat weder einen Platz noch zählt er als Teilnehmer.

### Am Tisch

Setzen tut man sich mit Rechtsklick auf einen Stuhl. Entschieden wird über die Hotbar und nicht über
ein Menü, damit man den Tisch dabei sieht — die Knöpfe tragen den Preis drauf ("Mitgehen 240"), weil
"Call" etwas ist, das man drückt und hinterher erfährt.

| Befehl | Was er macht |
|--------|--------------|
| `/poker` | Einsätze, Regeln, dein Konto, bei einem Turnier auch Level und Topf |
| `/poker nachkaufen` | Noch ein Buy-in (im Turnier nicht) |
| `/poker aufstehen` | Chips werden wieder Bits |
| `/poker bot` · `/poker bot weg` | Einen Bot setzen oder die eigenen abräumen |
| `/poker setup` | Tische und Eingang neu festlegen, nachdem umgebaut wurde (Op) |
| `/poker karte speichern` | Die Welt für die nächste Nacht sichern (Op) |

Auf dem Filz liegt alles, was passiert: die Gemeinschaftskarten in der Mitte, die eigenen zwei vor dem
Stuhl — verdeckt für den Raum, offen für den Besitzer — ein Haufen Chips für jeden Einsatz, ein
größerer in der Mitte für den Pot, und über jedem Stuhl ein Kopf mit Name und Stack. Der Kopf von dem,
der dran ist, dreht sich schneller und schwebt höher; damit sieht man quer durch den Raum, wer
überlegt, ohne eine Zeile Chat zu lesen.

### Die Welt

Das Casino wird **einmal** im Code gebaut und gehört danach dem, der darauf baut. Generierte
Architektur sieht generiert aus, und ein Raum, der bei jedem Start neu entsteht, ist ein Raum, den
niemand verbessern kann.

Damit ein Umbau den Abend überlebt, liegt die Welt in `./poker-world` neben dem Launcher. Jeder neu
erstellte Casino-Server bekommt sie kopiert; `/poker karte speichern` schreibt den aktuellen Stand
zurück. Wo die Tische stehen, sagt `configs/poker/layout.yml` und nicht das Gebäude — man kann also
einen Tisch versetzen, `/poker setup tisch 2` sagen, und Stühle, Karten, Chips und Köpfe ziehen mit.

### Bots

Bots gibt es, damit ein Tisch auch dann spielbar ist, wenn nur zwei Leute da sind — und für später,
wenn Spieler sich eigene Runden aufmachen können.

**Sie kosten Geld, und das ist keine Bremse, sondern Notwendigkeit.** Ein Bot hat kein Konto. Seine
Chips müssen von jemandem kommen, und der einzige ehrliche Jemand ist der, der ihn gesetzt hat: er
zahlt den Stack, er bekommt zurück was übrig ist, und wenn der Tisch den Bot auseinandernimmt, ist das
Geld wirklich weg — aus seiner Tasche in die der Gewinner. Ohne das wäre ein Bot eine Maschine, die
Bits herstellt. Dazu kommt eine Gebühr, die nicht zurückkommt; die macht aus "Tisch voll Bots" eine
Entscheidung statt eines Hebels. Bots zählen aufs Konto ihres Besitzers, aber ihre **Hände** zählen
nicht — sonst erspielt man sich die Wertungsschwelle, indem man drei Bots den Abend spielen lässt.

**Wie sie spielen.** Nicht nach einer Tabelle, sondern nach denselben zwei Zahlen wie ein Mensch: wie
oft die Hand von hier aus gewinnt (ein paar hundert Mal ausgespielt), und was der Pot dafür bietet.

**Charakter statt Typen.** Jeder Bot bekommt beim Setzen ein Temperament, und es gibt keine Stufen und
keine festen Werte — zwei fließende Achsen werden gezogen, alles andere folgt daraus:

- **heat**: passiv bis aggressiv. Wie gern er der ist, der Geld reinlegt, statt der, der zahlt um zu sehen.
- **care**: unbekümmert bis vorsichtig. Wie viel besser als der Preis die Hand sein muss. Das ist die
  risikoscheue Achse.

Wenig heat und viel care ist der Stille, der den Abend passt und mit den Nuts auftaucht. Viel heat und
wenig care ist der Fröhliche, der jede Hand spielt und jedes Board setzt. Viel von beidem ist der, der
selten drin ist und dich überrollt, wenn er es ist. Niemand hat entschieden, dass diese Typen
existieren — sie fallen aus zwei Zahlen heraus. Bei 100 Ziehungen sind 82 verschiedene Werte dabei;
feste Typen wären eine Handvoll.

Ein neuer Bot wird gegen die gezogen, die schon am Tisch sitzen, und zwar absichtlich unähnlich: sechs
unabhängige Ziehungen landen öfter dicht beieinander, als man denkt, und sechs Bots die gleich spielen
sind ein Bot.

**Warum man sie nicht lesen kann.** Drei Sachen, jede gegen eine andere Art von Muster:

1. *Mischzone.* Eine harte Schwelle verrät sich — "callt über 34 %" findet man in zwei Händen. Knapp am
   Preis entscheidet deshalb eine gewichtete Münze statt einer Linie, und wie breit die Zone ist, gehört
   selbst zum Charakter. Bei Asse in derselben Situation 200 mal gefragt: 74 % Call, 26 % Erhöhen — und
   bei jedem Bot anders. Eine Erhöhung sagt also nicht, was er hält.
2. *Semi-Bluff.* Ein Bot, der nur erhöht wenn er vorn ist, ist in einem Satz lesbar: erhöht er, passt du.
   Also erhöht er auch mal mit einem Draw. Das ist gleichzeitig besseres Spiel.
3. *Drift.* Ein festes Temperament ist immer noch lernbar, es dauert nur länger. Also wandert es: ein
   Bad Beat schiebt heat hoch und care runter, wie Tilt bei Menschen, und darunter wandern beide Achsen
   jede Hand ein wenig. Nach 200 Händen verlangt derselbe Bot 6–8 % weniger Vorsprung als vorher — ein
   Read von vor einer Stunde beschreibt einen anderen Gegner.

Dazu die Denkzeit: sie hängt am Charakter und daran, ob die Entscheidung knapp ist. Ein Tisch, an dem
jede schwierige Entscheidung nach genau einer Sekunde zurückkommt, ist ein Tisch, an dem man schwierige
von einfachen Entscheidungen unterscheiden kann.

Der Charakter wird **nie angezeigt**. Er steht beim Setzen einmal im Serverlog und sonst nirgends.

Der Fehler, an dem naive Pokerbots sterben, ist die Annahme, der Gegner halte Zufallskarten. Wer
dreimal erhöht hat, hält keine Zufallskarten, und gegen die Hand, die er wirklich hat, ist Top Pair
nicht die 72 %, die die Simulation ausrechnet. Unkorrigiert redet ein Bot sich so jeden verlorenen Call
ein. Hier wird deshalb die **Gegnerhand aus einer Range gezogen, die mit der Größe des Einsatzes enger
wird**. Der Unterschied ist gemessen und steht in `BotBalanceCheck`:

| Gegner | vor der Korrektur | danach |
|--------|-------------------|--------|
| Nit (nur Premiumhände) | −11,1 BB/Hand | **+0,7 BB/Hand** |
| Maniac (immer All-In) | +75,2 BB/Hand | +52,1 BB/Hand |
| Calling Station | +3,2 BB/Hand | +4,3 BB/Hand |

Darüber stehen zwei harte Regeln, weil sie die zwei Ausfälle sind, die einen Bot-Tisch wertlos machen:
eine führende Hand wird **nie** gepasst, und ein ganzer Stack geht **nie** auf eine hinterherlaufende
Hand — All-In braucht entweder die Hand dafür oder einen Stack, der so kurz ist, dass es keinen
Unterschied mehr macht.

Absichtlich spielen sie etwas loser, als die Mathematik verlangt. Das Haus nimmt von jedem
ausgespielten Pot seinen Anteil, also würde ein Bot, der gegen Menschen genau null spielt, den Tisch
allein über den Rake leerlaufen lassen.

### Turnier

Ein Turnier ist kein Cash Game mit anderen Zahlen, sondern ein anderer Abend. Ein Buy-in pro Person,
steigende Blinds (halb mehr pro Level), raus ist raus, am Ende teilen sich die vorderen Plätze den
Topf (50/30/20 ab sieben Spielern, 70/30 ab vier, sonst nimmt der Sieger alles). Der Hausanteil wird
einmal von den Buy-ins genommen statt von jedem Pot — bei Chips, die kein Geld sind, geht es gar nicht
anders.

**Chips sind hier kein Geld**, und das ändert die ganze Buchführung. Wer mit einem großen Stack
aufsteht, hat nichts gewonnen; ausgezahlt wird erst am Ende aus dem Topf. Dem Launcher wird deshalb
auch nicht der Stack als offener Betrag gemeldet, sondern das **Buy-in** — sonst würde ein Absturz
allen ihren Chipstand als Bits auszahlen. Ein Turnier, das nie zu Ende gespielt wird, kostet also den
Abend und niemanden Geld.

Ein Turnier läuft auf **einem** Tisch. Spieler zwischen Tischen umzusetzen, wenn Plätze leer werden,
ist ein eigenes System — Tische brechen, Plätze ausgleichen, eine Bubble über vier Räume — und die
Hälfte davon wäre schlechter als nichts. Ein Cash Game benutzt alle Tische.

### Geprüft

Die Regeln (`de.schnorrenbergers.poker.game`) enthalten nichts aus Bukkit. Sie zeichnen nichts und
verschicken nichts, sie melden über `TableEvents`, was passiert ist. Das ist kein Ordnungssinn: hier
hängt echtes Geld dran, und Regeln, die man ohne Server laufen lassen kann, sind Regeln, die man
nachprüfen kann. Vier Klassen mit `main` tun genau das:

```bash
./mvnw -q -pl PokerPlugin -am install -DskipTests
CP=PokerPlugin/target/classes:PokerPlugin/target/test-classes
java -cp $CP de.schnorrenbergers.poker.game.HandCheck        # 47 Prüfungen
java -cp $CP de.schnorrenbergers.poker.game.TableCheck       # 38 Prüfungen
java -cp $CP de.schnorrenbergers.poker.bot.BotBalanceCheck   # Bot gegen Maniac, Nit, Station
java -cp $CP de.schnorrenbergers.poker.bot.BotTableCheck     # sechs Bots gegeneinander
java -cp $CP de.schnorrenbergers.poker.bot.BotCharacterCheck # Spread, Mischung, Drift
```

`HandCheck` prüft den Hand-Evaluator gegen die Fälle, die man normalerweise falsch macht: das Wheel
(A-2-3-4-5), drei Paare bei sieben Karten, Kicker, geteilte Pötte, dazu 5000 Zufallshände.
`BotCharacterCheck` misst nicht das Spiel, sondern die Lesbarkeit: wie weit die gezogenen Charaktere
auseinanderliegen, ob derselbe Bot in derselben Situation dieselbe Antwort gibt, und wie weit sein
Temperament über einen Abend wandert.

`TableCheck` fährt die Setzlogik durch die Regeln, die normalerweise falsch sind — heads-up ist der
Button der Small Blind und zieht vor dem Flop zuerst, der Big Blind wird auch dann gefragt, wenn alle
nur mitgegangen sind, ein All-In das kleiner als eine volle Erhöhung ist öffnet die Runde **nicht**
neu, und wer all-in für weniger drin ist, kann nur gewinnen, was er verlieren konnte. Am Ende 400
Zufallshände, in denen Chips nur wandern und nie entstehen.

## Events anlegen

`/events` → **Neues Event**. Name, Beschreibung, Typ, Start und Dauer stehen oben, darunter zwei Knöpfe,
die bei **jedem** Eventtyp an derselben Stelle sitzen:

| Knopf | Was dahinter ist |
|-------|------------------|
| Einstellungen (Komparator, links) | Alle Regeln des Typs — Schalter und Zahlen zum Durchklicken |
| Belohnungen (Goldbarren, rechts) | Wer was bekommt |

Das Event-Panel eines bestehenden Events hat dieselben zwei Knöpfe an denselben Plätzen. Dort wird jede
Änderung sofort gespeichert, beim Anlegen erst mit **Anlegen**. Spieler sehen beide Knöpfe auch, können
sie aber nicht ändern — so sieht jeder, worum gespielt wird.

### Auf der Website

Modul **Events** → **Bearbeiten** an einem Event öffnet ein Formular mit allem auf einmal: Name,
Beschreibung, Anfang, Ende, die Einstellungen des Typs (dieselben Stufen wie im Spiel, als Auswahlliste)
und die Belohnungen (wer: Platzierung von/bis, ab X Kills oder Teilnahme; was: Bits und beliebig viele
Items mit Anzahl, Itemnamen werden aus der Liste eines laufenden Spielservers vorgeschlagen und geprüft).
Gespeichert wird alles mit einem Klick auf **Speichern**. Ein neu angelegtes Event öffnet direkt dieses
Formular.

- Wurde das Event inzwischen im Spiel oder in einem anderen Tab geändert, wird das Speichern abgelehnt
  („bitte nochmal öffnen“) statt die andere Änderung zu überschreiben.
- Hat das Event schon angefangen, lässt sich der Anfang nicht mehr verschieben; das Ende schon.
- Ein schon abgerechnetes Event kann nicht mehr bearbeitet werden, nur noch gelöscht.
- Der Typ eines Events lässt sich nicht ändern — dafür ein neues anlegen.
- Belohnungen gibt es nur bei Typen, die jemanden werten (Tabelle unten).

### Belohnungen

Eine Belohnung ist: **was** (Bits und Items) und **wer** sie bekommt. Angelegt wird sie in dieser
Reihenfolge — Neue Belohnung, Geld durchklicken und Items aus der Hand dazulegen, dann wählen, wer:

| Auswahl | Wer sie bekommt |
|---------|-----------------|
| `#1`, `#2`, `#3` | Genau dieser Platz |
| Top 10 | Platz 1 bis 10 |
| ab Platz 10 | Platz 10 bis zum letzten |
| Von/Bis von Hand | Jeder beliebige Bereich, z.B. Platz 4–10 |
| ab X Kills | Wer mindestens so viele Kills hat — nur bei Events, die Kills zählen |
| Teilnahme | Jeder, der mitgemacht hat |

**Jede Belohnung, die passt, wird ausgezahlt.** Wer Platz 1 und 5 Kills hat, bekommt die Belohnung für
`#1` und die für „ab 5 Kills“. Ausgezahlt wird beim Abrechnen des Events; wer offline ist, bekommt sie
beim nächsten Join (Geld nur auf einem Server mit Wirtschaft, wie bisher).

Welche Events überhaupt jemanden werten:

| Typ | Platz | Kills |
|-----|-------|-------|
| UHC (Bosse / Drache) | nach Zeit, das Team teilt sich den Platz; läuft dieselbe Gruppe mehrmals, zählt nur ihr bester Lauf | – |
| Pokernacht | nach Gewinn, nur wer die Mindesthände hat | – |
| Hunger Games | in der Reihenfolge, in der die Leute rausfliegen | ja |
| Bedwars | nach Team, in der Reihenfolge, in der die Teams ausscheiden; das Team teilt sich den Platz | ja, pro Spieler, Final Kills zählen mit |
| Einfach, Andere Welt, End | – | – |

**Ein Event endet mit seinem Spiel, nicht mit seiner Uhr.** Bei Bedwars und Hunger Games geht ein
Event, dessen Zeit abläuft, während noch gespielt wird, in die Verlängerung („Verlängerung“ im
Kalender). Abgerechnet wird, sobald der Spielserver meldet, dass die Runde vorbei ist. Ohne diese Meldung
rechnet der Launcher ab, wenn der Server nicht mehr läuft (Absturz, niemand kam), und spätestens nach
sechs Stunden Verlängerung.

**Bedwars im Detail:** Das zuerst ausgeschiedene Team bekommt den schlechtesten Platz, das letzte
stehende Platz 1. Endet die Runde über das Zeitlimit, werden die noch stehenden Teams nach derselben
Punktetabelle geordnet, mit der das Zeitlimit den Sieger bestimmt; bei einem Unentschieden an der Spitze
entscheidet die Reihenfolge der Tabelle. Beendet ein Op die Runde (`STOPPED`) oder läuft sie leer,
bleiben die stehenden Teams ohne Platz: Kills und Teilnahme zählen, ein Platz nicht. Private Runden über
`/runde` melden nichts.

Gespeichert werden Belohnungen als `reward.<n>` in den Einstellungen des Events, z.B.
`who=place:4:10;money=100;items=DIAMOND:2`. Events von vorher mit `prize.place.1..3` und
`prize.participation` werden als die Regeln gelesen, die sie meinten, und beim ersten Speichern
umgeschrieben.

**Behoben dabei:** Der Launcher hat Einstellungen mit Punkt im Schlüssel (`poker.buy-in`,
`prize.place.1`) als verschachteltes YAML geschrieben, aber flach zurückgelesen. Nach einem Neustart des
Launchers waren dadurch alle Preise und alle Poker-Einstellungen eines Events kaputt. `EventStore` liest
sie jetzt vollständig zurück (geprüft in `RewardCheck`).

### Ein neues Event bauen (Vorlage)

Was ein neuer Eventtyp braucht, von oben nach unten — die meisten brauchen nur die ersten drei Schritte:

1. **Typ**: Eintrag in `EventType` mit Titel und ob er Plätze (`ranked`) und Kills (`kills`) kennt.
2. **Einstellungen**: eine Klasse wie `HungerGamesSettings` mit Schlüsseln, Gettern und einer Liste
   `SETTINGS` aus `EventSetting.toggle(...)` / `EventSetting.choice(...)`. Das Einstellungs-Menü wird
   daraus gebaut, niemand muss ein Inventar schreiben.
3. **Definition**: eine Zeile in `EventDefinitions`:
   `register(EventDefinition.of(EventType.X, Material.BOW).settings(XSettings.SETTINGS));`
4. **Eigener Server** (falls das Event auf einem läuft): eine `ServerTemplate`, ein
   `FileType.PLUGIN`, der Einstellungs-Schlüssel des Servers in `EventType`, und eine
   `ServerEventStarter.define(...)`-Kette — der fährt den Server fünf Minuten vorher hoch, schreibt
   seinen Namen ins Event und holt die Lobby rüber (`warpFor`) oder lädt nur ein (`walkIn`, wie die
   Pokernacht). Bedwars, Pokernacht und Hunger Games laufen alle darüber. Der Server findet sein Event über den
   eigenen Namen (siehe `ArenaContext`).
5. **Wertung**: der Spielserver meldet `EventResultData` (Platz, Kills) über
   `EventResultService.report(...)`. Der Launcher hebt sie in `results.yml` auf und zahlt beim Abrechnen
   über `RewardPayout` aus — die Belohnungsregeln gelten dann ohne weiteren Code. Für die Anzeige gibt es
   `EventResultUi`.

## Hunger Games

Alle in eine Welt, in der Mitte das Füllhorn mit gutem Loot, über die Karte fallen Supply Drops, am Ende
zieht sich die Weltgrenze zusammen bis zum Showdown. Nether und End sind aus.

Ablauf: Fünf Minuten vor dem Event fährt die Lobby die Arena hoch (`HUNGER_GAMES_<id>`) und lädt ein;
zur Eventzeit schickt sie alle rüber, zwei Minuten lang auch Nachzügler. In der Arena wird gewartet, bis
die Eventzeit da ist **und** genug Spieler da sind, dann 30 Sekunden Countdown — die letzten zehn steht
jeder eingefroren auf seinem Startplatz im Ring ums Füllhorn. Danach Schutzzeit, freies Spiel, Grenze
schrumpft, Showdown. Wer stirbt oder den Server verlässt, ist raus und schaut zu; wer nach dem Start
kommt, schaut zu. Der Letzte gewinnt, 20 Sekunden später geht es zurück in die Lobby.

| Einstellung | Standard | Was sie macht |
|-------------|----------|---------------|
| Mindestens Spieler | 2 | Vorher startet nichts, auch wenn die Zeit da ist |
| Schutzzeit | 1 Min | Niemand kann einem anderen Spieler schaden |
| Grenze am Anfang | 500 | Durchmesser der Welt beim Start |
| Grenze beim Showdown | 20 | So klein wird sie am Ende |
| Grenze schrumpft ab | 10 Min | Nach dem Start |
| Schrumpfdauer | 10 Min | Bis zur Showdown-Größe |
| Supply Drops | 4 Min | Abstand zwischen zwei Paketen, `aus` möglich |
| Leuchten im Showdown | an | Wer im Showdown lebt, leuchtet |

Die Teamgröße ist als Schlüssel (`hg.team-size`) vorbereitet, gespielt wird aber immer solo: Teams
brauchen eigene Plätze und Friendly-Fire-Regeln, die gibt es noch nicht.

**Plätze** ergeben sich aus der Reihenfolge des Rausfliegens. **Das Spiel endet, wenn einer übrig ist —
nicht, wenn die Eventzeit abläuft.** Läuft die Zeit ab, geht das Event in die Verlängerung (so steht es
auch im Kalender) und wird erst abgerechnet, wenn die Arena meldet, dass das Spiel vorbei ist.
`/hg stop` beendet ohne Wertung der Lebenden.

**Nether und End:** `allow-end` in der `bukkit.yml` wirkt, `allow-nether=false` hält den Nether auf 26.3
aber nicht mehr vom Laden ab. Die Arena entlädt ihn deshalb nach dem Start selbst, und Portale werden
ohnehin abgelehnt.

**Die Karte.** Liegt eine Welt in `./hungergames-world` beim Launcher (Ordner mit `level.dat`), wird sie
auf jede neue Arena kopiert. Ohne Karte gibt es eine frisch generierte Welt. Optional liegt in der Karte
eine `hungergames.yml`:

```yaml
center: {x: 0, z: 0}   # Standard: der Weltspawn
cornucopia-radius: 12  # Kisten so nah an der Mitte sind das Füllhorn
spawn-radius: 22       # Radius des Startrings
spawns: ["10,70,-4"]   # feste Startplätze statt des Rings, optional
```

`/hg mitte` schreibt die Mitte von dort, wo man steht — auch in die Karte beim Launcher, gilt also ab
der nächsten Arena. Steht keine Kiste in der Nähe der Mitte, baut die Arena ein kleines Füllhorn
(Steinplatte, goldenes Horn, zwölf Kisten). Kisten auf der Karte werden beim ersten Öffnen gefüllt, wenn
sie leer sind; was der Kartenbauer hineingelegt hat, bleibt.

**Loot** steht in `./hungergames-loot.yml` beim Launcher (wird beim ersten Start mit den Standards
geschrieben): drei Tabellen `normal`, `cornucopia`, `supply` mit Material, Anzahl, Gewicht und optional
Verzauberungen.

**Supply Drops** fallen als Kiste aus 40 Blöcken Höhe irgendwo in die inneren 80 % der aktuellen Grenze,
mit Koordinaten im Chat und einer Lichtsäule. Landet eine auf einer Fackel oder in einem nicht geladenen
Chunk, wird sie nach 20 Sekunden einfach hingestellt.

| Befehl | Was er macht |
|--------|--------------|
| `/hg` | Stand des Spiels |
| `/hg start` | Sofort starten (Op). Alleine läuft es als Test, bis `/hg stop` |
| `/hg stop` | Ohne Sieger beenden (Op) |
| `/hg mitte` | Mitte der Karte setzen (Op) |

Beim Abrechnen des Events zahlt der Launcher die Belohnungen aus `results.yml` aus, stoppt die Arena und
löscht ihr Verzeichnis.

## Whitelist und Regeln

Spieler setzen sich selbst auf die Whitelist: auf `http://<host>:8080/regeln` lesen sie die Regeln,
setzen den Haken bei „akzeptiere“ und geben ihren Minecraft-Namen ein. Der Name wird bei Mojang
nachgeschlagen (Groß-/Kleinschreibung kommt von dort), und der Spieler landet sofort auf allen laufenden
Servern - ohne Neustart. Wer nicht auf der Whitelist steht, bekommt beim Joinen genau diesen Link genannt.

| Wo | Was |
|----|-----|
| `/regeln` (ohne Login) | Regeln lesen, akzeptieren, Namen eintragen. Ein Versuch alle 10 Sekunden pro Adresse |
| Discord `/regeln` | Zeigt die Regeln (nur dem, der fragt) zum Durchblättern, mit einem Knopf zu dieser Seite |
| Admin-Website → Whitelist | Regeln schreiben, Whitelist an/aus, Selbst-Eintragen an/aus, öffentlicher Link, Liste mit Entfernen |
| Admin-Website → Einstellungen | Namen, die Admins von Hand eintragen (wie bisher `whitelist` in der `main-config.yml`) |

**Die Whitelist ist standardmäßig aus** (`enforced: false` in der `whitelist.yml`). Vorher war sie das auch
- `whitelist.json` wurde geschrieben, `white-list` aber nie eingeschaltet, also kam jeder rein. Wer sie
einschaltet, sperrt jeden aus, der noch nicht darauf steht; deshalb geht das nur bewusst im Panel (mit
zweitem Klick), und Eintragen funktioniert schon vorher, damit die Liste voll ist. Ops kommen immer rein.

Wie es gebaut ist:

- `whitelist.yml` hält die Regeln, die Schalter und jeden, der sie akzeptiert hat - nach UUID, mit Datum und
  einem Hash der Regeln, die er gesehen hat. Nach einer Regeländerung bleiben alle auf der Liste und sind im
  Panel als „alte Regeln“ markiert.
- Beim Start eines Servers schreibt der Launcher `whitelist.json` **jedes Mal neu** aus den Admin-Namen und
  der `whitelist.yml`, dazu `white-list`/`enforce-whitelist` und die Kick-Nachricht (`spigot.yml`,
  `messages.whitelist`). Vorher geschah das nur beim allerersten Start eines Servers, spätere Änderungen
  kamen nie an. Ein `/whitelist add` im Spiel überlebt deshalb keinen Neustart - dafür ist die Website da.
- Ein Name, den Mojang nicht kennt (Tippfehler, umbenannt), wird ausgelassen statt den Start mit einer
  NullPointerException abzubrechen.
- Laufende Server bekommen neue Spieler über ihre `whitelist.json` plus `whitelist reload`, nicht über
  `whitelist add`: die Server laufen hinter dem Proxy im Offline-Modus und müssten die UUID sonst selbst
  raten. Entfernen wirkt genauso sofort und schickt den Spieler vom Server.
- Der Link in der Kick-Nachricht ist `http://<Adresse des Netzwerks>:<web.port>/regeln`; läuft die
  Website hinter einer Domain, trägt man die Adresse im Panel ein.

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

## Tickets

Ein Ticket ist ein Gespräch zwischen einem Spieler und den Admins. Es läuft so lange hin und her, bis
jemand es schließt, und jeder kann es danach wieder öffnen. Wo geschrieben wird, ist egal: Es gibt einen
Stand, und der landet überall.

| Wo | Spieler | Admins |
|----|---------|--------|
| Discord | Knopf „Ticket schreiben“ im Ticket-Kanal; Antworten und Schließen über die Knöpfe in der DM | Ein Thread pro Ticket im Admin-Kanal: Jede Nachricht dort geht an den Spieler, außer sie beginnt mit `//`. Knöpfe zum Übernehmen, Schließen und Wieder-Öffnen |
| Im Spiel | `/ticket`, `/ticket neu`, `/ticket <nr>`, `/ticket <nr> antworten [text]`, `/ticket <nr> schliessen\|oeffnen` | dazu `/ticket offen` und `/ticket <nr> uebernehmen` (Op oder `network.tickets`) |
| Website | – | Panel „Tickets“ |

Wer antwortet, bekommt das Ticket automatisch zugeteilt, sofern es noch niemand hat. Ein Spieler bekommt
Antworten per DM, wenn sein Discord bekannt ist, und im Spiel, wenn er online ist. War er offline, sieht
er sie beim nächsten Join. Mit einer [Discord-Verknüpfung](#discord-verknüpfung) ist er an beiden Stellen
erreichbar. Ohne Verknüpfung nur dort, wo er das Ticket geschrieben hat. Admins im Spiel erfahren von neuen
Tickets und Antworten. Ist ein Ticket übernommen und der Bearbeiter online, erfährt es nur er.

Unter jeder Admin-Aktion im Logging-Kanal steht ein Knopf, der ein Ticket zu genau dieser Aktion öffnet.
Die Aktion steht dann im Ticket unter „Bezieht sich auf“.

**Einrichten** (Discord, Administrator):
- `/setticketchannel` im Kanal, in dem Spieler Tickets schreiben. Der Bot legt dort seine Nachricht mit
  dem Knopf an, falls sie in den letzten 25 Nachrichten fehlt. Gelöscht wird nichts.
- `/setticketstaffchannel` im Kanal, in dem die Admins arbeiten. Ohne ihn gibt es keine Threads.
  Tickets laufen dann nur über DM, Spiel und Website.

Die Tickets liegen in `tickets.yml` beim Launcher. Die alten Tickets aus der `main-config.yml`
(`tickets`, `ticket-N`) werden beim ersten Start einmal dorthin übernommen, mit derselben Nummer, und
danach aus der `main-config.yml` entfernt. Ist der Discord-Account des alten Autors verknüpft, bekommen
sie auch seinen Minecraft-Namen.

Geprüft mit `ServerLauncherApplication/src/test/java/de/hems/utils/ticket/TicketCheck.java` (Aufruf
steht in der Klasse, aus einem leeren Verzeichnis starten).

**Ohne Discord:** Ein zweiter Rechner mit einer Kopie des Netzwerks (zum Testen) darf sich nicht mit
demselben Bot anmelden - beide würden auf jeden Befehl antworten, und ein `/verify`-Code vom einen wäre
dem anderen unbekannt. `MCSERVER_DISCORD=off` in der `.env.local` dieses Rechners startet den Launcher ohne
Bot.

## Info-Kanäle

Der Bot erklärt das Netzwerk auf Discord: einmal für Spieler (alle Features und ihre Befehle), einmal
für Admins (Admin-Befehle, Website, Neustart, Events, Moderation, bekannte Einschränkungen), und die Regeln.

| Befehl (Discord) | Was er macht |
|------------------|--------------|
| `/setinfochannel` (Administrator) | Die Erklärung für Spieler kommt in diesen Kanal |
| `/setadmininfochannel` (Administrator) | Das Handbuch für Admins kommt in diesen Kanal |
| `/setruleschannel` (Administrator) | Die Regeln kommen in diesen Kanal, mit Knopf zur Regeln-Seite |
| `/regeln` (jeder) | Zeigt die Regeln nur dem, der fragt, zum Durchblättern |

Jeder Text ist **eine Nachricht zum Durchblättern**: sie zeigt den ersten Teil, darunter „◀ Zurück“,
„Weiter ▶“ und ein Menü „Springe zu …“ mit allen Teilen. Wer klickt, bekommt den Text nur für sich
angezeigt (ephemeral) und blättert darin weiter - so dreht niemand jemand anderem die Seite um. Jeder
`##`-Abschnitt ist ein Teil, bei den Regeln jeder Paragraph (`§1 - …`); Unterparagraphen (`§2.1`) stehen
fett darin. Die Texte werden bei jedem Klick frisch gelesen, geänderte Regeln sind also sofort da.

Der Bot merkt sich seine Nachricht (`info-channel-messages`, `admin-info-channel-messages`,
`rules-channel-messages` in der `main-config.yml`) und bringt sie **bei jedem Start** auf den neuen Stand.

**Kanal mit Nachrichten darin:** Wird ein `/set…channel` in einem Kanal benutzt, in dem schon andere
Nachrichten stehen, postet der Bot nicht dazwischen. Der Kanal wird in `<name>-archiv` umbenannt, für
alle versteckt und nach unten geschoben; eine frische Kopie (gleicher Name, gleiche Kategorie, gleiche
Rechte) kommt an seinen Platz und bekommt die Nachricht. Dafür braucht der Bot „Kanäle verwalten“ und
„Berechtigungen verwalten“, sonst sagt er das und nimmt nur leere Kanäle. Ein neuer Kanal räumt die
Nachricht im alten weg.

Die Texte liegen in `ServerLauncherApplication/src/main/resources/discord-info/` (`spieler.md`,
`admins.md`). Wer sie ohne Build ändern will, legt eine Datei gleichen Namens in `./discord-info/`
neben den Launcher, die gewinnt. `{adresse}` und `{regeln}` werden mit der Adresse des Netzwerks und
dem Link zur Regeln-Seite gefüllt - die Adresse aus `public-address` in der `main-config.yml`
(fragt das Setup ab, z.B. `mc.ben-schnorr.com`), sonst die IP, auf die der Proxy hört. Ist eines
davon nur lokal (`localhost`), fällt die Zeile weg.
Discord zeigt keine Tabellen. Deshalb sind die Texte Listen, und pro Teil sind höchstens 4096 Zeichen
möglich (längere werden an einem Absatz geteilt). Das Menü fasst höchstens 25 Teile.

**Wer ein Feature baut, ergänzt den passenden Text.** Geprüft mit
`ServerLauncherApplication/src/test/java/de/hems/utils/bot/info/InfoTextCheck.java` (Aufruf wie bei
`RewardCheck`, aus einem leeren Verzeichnis starten).

## Lobby-Schutz und Portal

In der Lobby verändert niemand die Karte, außer im Kreativmodus: kein Block abgebaut oder gesetzt, kein
Eimer, keine Rahmen, Bilder oder Rüstungsständer, kein zertrampeltes Feld, keine Blumentöpfe oder
Notenblöcke. Von selbst ändert sich auch nichts - Explosionen, Feuer, Laubzerfall und schmelzendes Eis sind
aus, Mobs spawnen nicht und das Wetter bleibt. Das ersetzt die Vanilla-Spawn-Protection, die Ops ausnimmt
und nach 16 Blöcken endet.

Ein **Netherportal** in der Lobby führt auf Survival (wie `/warp SURVIVAL`, wartet also auf einen Server,
der gerade startet).

## Lobby-NPCs

In der Lobby stehen NPCs, die man anklickt (rechts oder links), um woanders hinzukommen:

- **Warp-NPC:** schickt auf einen Server - genau wie `/warp <server>`, wartet also auf einen Server, der
  gerade hochfährt. Über dem Kopf steht der Name, ob der Server läuft und wie viele darauf sind
  (alle 10 Sekunden aus dem Netzwerk geholt).
- **Event-NPC:** öffnet den Eventkalender (`/events`). Über dem Kopf steht live, was gerade läuft oder
  als Nächstes kommt, mit Countdown.

Die NPCs sind `Mannequin`s - die spielerförmige Entity, die Minecraft seit 1.21.9 selbst hat. Sie tragen
echte Skins, brauchen kein zusätzliches Plugin und keine Pakete, und schauen den nächsten Spieler in
acht Blöcken Umkreis an. Wie der Lotto-Stand werden sie nie mit der Welt gespeichert, sondern bei jedem
Start und jedem Nachladen des Chunks neu aufgestellt - es bleibt also nie ein doppelter stehen.

| Befehl | Was er macht |
|--------|--------------|
| `/npc` · `/npc list` | Alle NPCs, anklicken springt hin |
| `/npc warp <server> [name]` | Warp-NPC an die eigene Position, Blickrichtung wird übernommen |
| `/npc events [name]` | Event-NPC an die eigene Position |
| `/npc name <id> <name>` | Umbenennen, `&`-Farbcodes gehen (`&bSurvival`) |
| `/npc skin <id> <spieler\|aus>` | Skin eines Minecraft-Accounts tragen, oder den Standard |
| `/npc ziel <id> <server>` | Ziel eines Warp-NPCs ändern |
| `/npc hier <id>` · `/npc tp <id>` · `/npc weg <id>` | Versetzen, hinspringen, entfernen |

Alles nur mit `network.npc.admin` (Standard: Ops). Gespeichert wird in `plugins/LobbyPlugin/npcs.yml`,
ohne Weltnamen - eine neue Lobby-Map behält ihre NPCs.

## Lotto

4 aus 15, bezahlt mit Bits. Jeder Tipp kostet gleich viel (Standard 100 Bits, also einen Diamanten an
der Bank), und man kann so oft tippen, wie man will. Jede Woche zur selben Zeit (Standard: Sonntag
20:00, Zeitzone Berlin) zieht der Launcher vier Zahlen. Wer alle vier getroffen hat, bekommt den
ganzen Topf.

- **Der ganze Einsatz geht in den Topf.** Das Haus behält nichts.
- **Trifft niemand**, bleibt der Topf und wächst in der nächsten Runde weiter.
- **Treffen mehrere**, wird pro Gewinnertipp geteilt: Wer zwei richtige Tipps hat, bekommt zwei
  Anteile. Was sich nicht glatt teilen lässt, bleibt im Topf.
- **Der Gewinn geht direkt aufs Konto**, auch an Spieler, die gerade offline sind.
- **Chance:** 1 zu 1365 pro Tipp.
- **Eine Runde ohne Tipps** wird nicht gezogen, der Termin rückt einfach eine Woche weiter.
- **War der Launcher zur Ziehung aus**, wird gezogen, sobald er wieder läuft.

| Befehl | Was er macht |
|--------|--------------|
| `/lotto` | Öffnet den Tippschein: Zahlen anklicken, zufällig ausfüllen, 5 Quicktipps, eigene Tipps |
| `/lotto tipp 3 7 11 14` | Ein Tipp direkt |
| `/lotto quick [anzahl]` | 1 bis 20 zufällige Tipps |
| `/lotto info` · `/lotto meine` | Topf, Termin, letzte Zahlen · eigene Tipps dieser Runde |
| `/lotto ziehen` | Admin: jetzt ziehen |
| `/lotto termin sonntag 20:00` | Admin: Wochentag und Uhrzeit der Ziehung |
| `/lotto preis 100` | Admin: Preis pro Tipp, gilt ab dann |
| `/lotto stand` · `/lotto standweg` | Admin, nur Lobby: Lotto-Stand an die eigene Position stellen oder entfernen |

Admin heißt Op oder `network.lotto.admin`. `/lotto` gibt es auf jedem Server. Die Ziehung wird überall
im Chat und als Titel angesagt, und wer gewonnen hat, sieht „Gewonnen!“.

Der Stand in der Lobby ist ein Villager. Ein Klick auf ihn öffnet den Tippschein, und über ihm stehen
Topf, Zeit bis zur Ziehung und die letzten Zahlen. Er wird nicht mit der Welt gespeichert, sondern bei
jedem Start und nach dem Entladen seines Chunks neu hingestellt. Sein Platz steht in
`plugins/LobbyPlugin/lotto-stand.yml`.

Alles andere liegt in `lotto.yml` beim Launcher: Preis, Termin, Zeitzone (`settings.zone`), der Topf,
die Tipps der laufenden Runde und die letzten 20 Ziehungen. Gezogen wird mit `SecureRandom`.

Geprüft mit `ServerLauncherApplication/src/test/java/de/hems/utils/lotto/LottoCheck.java`.

## Module

| Modul | Inhalt |
|-------|--------|
| `ServerLauncherApplication` | Startet und konfiguriert die Server, Discord Bot, Admin Website |
| `CommonCode` | Netzwerk-Events, `ServerApi`, Server Manager UI, Warp System, Geld, Runden, Cosmetics |
| `LobbyPlugin` | Lobby, Parkour, Server Manager, eigene Runden |
| `Survival` | Survival Spielmodus |
| `Bedwars` | Bedwars Minispiel |
| `PokerPlugin` | Casino einer Pokernacht: Regeln, Tisch, Bots |
| `HungerGamesPlugin` | Arena eines Hunger-Games-Events: Füllhorn, Supply Drops, Grenze, Wertung |
| `BackpackPlugin` | Geteilter Team-Rucksack |
| `VelocityPlugin` | Meldet neue Server am laufenden Proxy an |
