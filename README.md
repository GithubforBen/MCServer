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
| Konsole | Schickt einen Befehl an einen laufenden Server |

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
| `VelocityPlugin` | Meldet neue Server am laufenden Proxy an |
