# Testen: lokal, mit echten Clients

Wie man das Netzwerk auf einem Entwickler-Rechner startet und Features **im Spiel** prüft, mit zwei echten
Minecraft-Clients, die ein Agent (oder ein Mensch) per Skript steuert. Geschrieben für Agenten, die das
nachmachen sollen. Alles hier wurde so am 2026-10-01 benutzt.

## Grundregeln (zuerst lesen)

- **Nie Befehle in ein `server-*`-tmux-Fenster tippen, dessen Java-Prozess nicht mehr läuft.** Dann landet
  der Text in einer normalen Shell; `shutdown` hat so zweimal den ganzen Rechner ausgeschaltet. Konsolen-
  befehle immer über `clients/con` schicken (prüft vorher, ob Java läuft). Das Netzwerk mit
  `clients/netstop` stoppen (Strg-C am Launcher), nie mit `stop`/`shutdown` in einem Fenster.
- **Discord aus:** Ein Testrechner darf sich nicht mit dem Bot des echten Servers anmelden (beide würden auf
  jeden Befehl antworten, `/verify`-Codes wären auf dem echten Server unbekannt). In die `.env.local`:
  `MCSERVER_DISCORD=off`.
- **Arbeitsspeicher:** Clients mit 2 GB, Server über `MCSERVER_MAX_MEMORY_MB=1024` in der `.env.local`
  gedeckelt, höchstens ein zusätzlicher Runden-/Event-Server gleichzeitig.
- **Was nach `master` geht, kann sofort auf dem echten Server landen** (`/neustart … update`). Nur pushen,
  was getestet ist.

## Einrichten

1. Java 25 für Build und Launcher: in die `.env.local` `JAVA_HOME=<pfad zum jdk 25>` und
   `PATH="$JAVA_HOME/bin:$PATH"` (macht `install.sh`). `run.sh` liest die Datei.
2. xdotool ohne sudo: `testing/clients/setup.sh` (entpackt nach `$MCTEST_DIR`, Standard `~/.cache/mctest`).
   Alle Skripte nehmen `MCTEST_DIR` von dort; Screenshots landen in `MCTEST_SHOTS` (Standard `/tmp`).
3. Die Clients müssen unter **XWayland** laufen, sonst sieht xdotool sie nicht (GNOME Wayland):
   - Vanilla: den Minecraft-Launcher mit `env -u WAYLAND_DISPLAY SDL_VIDEO_DRIVER=x11 SDL_VIDEODRIVER=x11
     minecraft-launcher` starten; das Spiel erbt das.
   - Lunar: zusätzlich `--ozone-platform=x11`.
   - Prüfen: `xwininfo -root -tree | grep com.mojang.minecraft` muss das Spielfenster zeigen.
4. Konten: **a** = LongRangeMissile (Vanilla, normaler Spieler), **b** = for_sale (Lunar, Op).
   Die Koordinaten in den Skripten gelten für das Vanilla-Fenster in 854×480 (Standardgröße) bzw. Lunar
   in 1280×720. Ist ein Fenster maximiert, Koordinaten aus dem Screenshot umrechnen.

## Netzwerk starten und stoppen

```bash
testing/clients/netstart     # startet run.sh in tmux "server", wartet bis LOBBY und SURVIVAL "Done" melden
testing/clients/netstop      # Strg-C am Launcher, wartet bis kein Server-Java mehr läuft
./mvnw -B -q install -DskipTests   # vorher bauen; ein Neustart nimmt die neuen Jars
```

Ports: Proxy 25565 (Client verbindet auf `localhost`), LOBBY 3001, SURVIVAL 3000 (lokal war 3000 von
Docker belegt → in `main-config.yml` auf 3010 gestellt). Server-Konsolen: tmux-Sessions `server-<NAME>`.

**Cluster prüfen** (alle müssen sich sehen, sonst antwortet der Launcher auf nichts):
```bash
grep -h "JGroups\] Connected" servers/*/logs/latest.log   # in jeder Zeile muss HOST stehen
ss -ltnp | grep -E ':78[0-9][0-9] '                        # Launcher + jeder Server ein Port ab 7800
```

## Die Client-Skripte (`testing/clients/`)

| Skript | Wozu |
|--------|------|
| `mc <a\|b> shot <name>` | Screenshot des Spielfensters nach `$MCTEST_SHOTS/<name>.png` |
| `mc <a\|b> key <tasten>` | Tasten (z.B. `Escape`, `F1`, `ctrl+a`) |
| `mc <a\|b> say <text>` | Chat öffnen, Text tippen, Enter |
| `cmd <a\|b> "/befehl"` | Befehl tippen **und prüfen**, dass ein Server ihn geloggt hat (3 Versuche) |
| `hclk <a\|b> <x> <y>` | Klick in Menüs: **erst hovern, dann klicken** (ein Klick ohne Mausbewegung wird ignoriert) |
| `con <SERVER> <befehl>` | Konsolenbefehl, nur wenn dort Java läuft |
| `place <SERVER> <spieler> <x> <z>` | Teleport auf die Oberfläche mit Resistenz/Slow Falling (Spieler sterben sonst) |
| `joina` / `rejoina` / `joinb` | Vom Titelbild bzw. „Connection Lost“ auf `localhost` verbinden |
| `aim <SERVER> <spieler> <x> <y> <z>` | Blick des Spielers auf einen Punkt drehen |

Typische Stolpersteine:
- **Mausklicks im Spiel** (Block abbauen) kommen per xdotool oft nicht an; Menüklicks mit `hclk` schon.
  Für Spielmechanik lieber per Konsole nachstellen (siehe Rezepte).
- Es poppt manchmal „Friends List“ oder „Third-Party Online Play“ auf: „Turn OFF“ bzw. „Proceed“ klicken.
- Spieler im Survival-Modus sterben an Fall/Zombies, wenn sie herumstehen. Für Tests Zuschauermodus
  (`con SURVIVAL "gamemode spectator <name>"`) oder `place` benutzen. Nie erst teleportieren und dann vom
  Zuschauer- in den Survival-Modus wechseln, ohne Boden unter den Füßen.
- `pgrep -f muster` findet die eigene Shell mit; für den Launcher `pgrep -f '^java -jar ServerLauncher'`.
- Tooltips lesen: mit `xdo mousemove --window <id> x y` hovern, Screenshot, Ausschnitt vergrößern
  (`convert in.png -crop WxH+X+Y +repage -scale 200% out.png`).

## Rezepte: so wurde jedes Feature geprüft

### Anti-X-Ray (Survival)
X-Ray heißt hier: ein **Texturpaket**, das Stein unsichtbar macht und Erze überall zeigt (kein Zuschauermodus).
1. `testing/make-xray-pack.sh` baut so ein Paket nach `~/.minecraft/resourcepacks/xraytest`; im Client unter
   Optionen → Ressourcenpakete aktivieren.
2. Spieler weit weg schicken (Chunks müssen danach **neu** gesendet werden), dann einen massiven Würfel bauen:
   ```
   con SURVIVAL "gamemode spectator LongRangeMissile"
   con SURVIVAL "tp LongRangeMissile 3000 120 3000"
   con SURVIVAL "forceload add 594 594 606 606"
   con SURVIVAL "fill 594 -36 594 606 -24 606 stone"
   con SURVIVAL "setblock 600 -30 600 diamond_ore"
   con SURVIVAL "forceload remove 594 594 606 606"
   con SURVIVAL "tp LongRangeMissile 600.5 -31.62 588.5 0 0"   # Auge auf Höhe des Erzes, 11 Blöcke davor
   ```
3. Screenshot: In der Bildmitte darf **kein** Erz zu sehen sein.
4. Gegenprobe Position: `setblock 600 -30 599 air` → das Erz muss jetzt genau dort erscheinen.
5. Gegenprobe Paket: derselbe Würfel in der Lobby (ohne Anti-X-Ray) → das Erz ist durch den Stein sichtbar.
6. Bestehender Server: Werte in `servers/SURVIVAL/config/*.yml` von Hand auf `false` setzen, neu starten → sie
   müssen wieder `true` sein. Frischer Server: `servers/SURVIVAL/config` wegschieben, starten → Paper legt die
   Dateien vollständig an, unsere Werte stehen drin, keine Fehler im Log.

### Dupes (Survival)
Nach dem Start in `servers/SURVIVAL/config/paper-global.yml` unter `unsupported-settings`:
`allow-piston-duplication`, `allow-unsafe-end-portal-teleportation`,
`skip-tripwire-hook-placement-validation` alle `true`.

### Lobby-Schutz und Block-Updates
```
con LOBBY "forceload add 320 320"
con LOBBY "setblock 320 120 320 stone"
con LOBBY "setblock 320 121 320 torch"
con LOBBY "setblock 320 120 320 air"
con LOBBY "execute if block 320 121 320 torch run say FACKEL-BLEIBT"
```
In der Lobby muss die Fackel bleiben; derselbe Ablauf auf SURVIVAL → sie fällt ab (Gegenprobe). Abbauen und
Setzen im Survival-Modus ist verboten, im Kreativmodus erlaubt (am einfachsten von Hand prüfen).

### TPS in der Tabliste
Mit einem Nicht-Op (Konto a) `Tab` gedrückt halten (`xdo keydown Tab`, Screenshot, `xdo keyup Tab`):
unten steht `LOBBY TPS 20.0 3 ms`.

### Join/Leave-Zeilen
In der Server-Konsole erscheint beim Joinen `>> <Name>` (z.B. `tmux capture-pane -p -t server-LOBBY`).

### Paying Player sofort auf allen Servern
Zwei Java-Programme im Cluster (`testing/probes/`): `PayRecv` lädt die echte `PayingPlayers`-Klasse und
wartet auf eine Änderung, `PaySend` schickt die Liste wie der Launcher.
```bash
JAR=ServerLauncherApplication/target/ServerLauncher.jar
java -cp $JAR testing/probes/PayRecv.java <uuid> &   # gibt "nachher: true nach … ms" aus
sleep 8; java -cp $JAR testing/probes/PaySend.java <uuid>
```

### Launcher antwortet (/verify, Servermanager)
`java -cp $JAR testing/probes/ClusterProbe.java` tritt dem Cluster bei, fragt die Serverliste ab und schickt
ein `/verify` mit falschem Code. Erwartet: Serverliste in Millisekunden, `RespondAccountLinkEvent`.

### Bedwars: Teamwahl und Ausgleich
`testing/probes/BalanceProbe.java` ruft den echten `TeamBalancer` per Reflection auf (Classpath: Bedwars-Jar,
paper-api, adventure aus `~/.m2`). Erwartet: zwei Gewählte bleiben zusammen (2/1/0/0), alle im selben Team →
einer wechselt (1/1), nur Zufällige werden ausgeglichen.

### Ender-Drachen-Event (UHC, Hardcore)
1. Konto a vorübergehend Op: `con LOBBY "op LongRangeMissile"` (ops.json wird beim nächsten Start neu
   geschrieben).
2. `/events` → Buch unten rechts „Neues Event“ → Typ (Papier) viermal klicken bis „Enderdrache (UHC)“, Start
   (Uhr) viermal bis „sofort“, Einstellungen zeigen Hardcore an / Teamgröße 1 / Versuche 3 → Anlegen (Slime).
3. Event im Kalender öffnen → Netherstern „Mitmachen & Bestenliste“ → grünes Bett „Mitmachen“. Bei
   Teamgröße 1 startet der Lauf sofort, ein Server `RUN_<event>_<lauf>` fährt hoch (~1 Min), der Spieler wird
   gewarpt.
4. Hardcore: `con RUN_<…> "kill LongRangeMissile"` → „… ist gestorben - der Lauf ist vorbei.“, zurück in die
   Lobby, im Panel „Deine Versuche: 1/3“. Der Server stoppt, sobald er leer ist, und sein Verzeichnis ist
   nach spätestens zwei Minuten weg.
5. Abbrechen: neuer Versuch, auf dem Run-Server zweimal `/abbrechen` → zurück in die Lobby, Server weg. Oder
   im Panel „Lauf abbrechen“ (zweimal klicken).
5b. Reset: auf dem Run-Server zweimal `/reset` → „… es geht von vorne los“, nach wenigen Sekunden „Ihr wartet in
   der Lobby auf euren neuen Server“ und Warp in die Lobby; der alte Server geht sofort aus (Log „The run is
   over and everybody has left“). In der Lobby „Dein nächster Lauf wird vorbereitet“ mit Fortschrittsbalken,
   Warp auf `RUN_<event>_<lauf>`, sobald er bereit ist. Im Panel ein Versuch mehr verbraucht. Nach einem Tod
   startet `/reset` ohne Bestätigung.
5c. Ghost (braucht beide Konten im Event, Teamgröße 1): a und b starten je einen Lauf. Nach höchstens 30 s
   meldet der Launcher „Building ghost server RUN_…_GHOST_…“. Wer sich dorthin warpt, ist Zuschauer
   („wartet noch auf seinen Lauf“). `/reset` von a → „vorbereiteten Server“, Warp ohne Weltgenerierung,
   Launcher „Ghost server … was claimed“ und gleich danach ein neuer Ghost. b verlässt das Event → drei
   Minuten später „Throwing away ghost server … fewer than 2 players are left“, kurz danach „Discarded run
   server“.
6. Drache: nächster Versuch (jedes Mal ein neuer Server), Resistenz/Slow Falling/Feuerresistenz geben, dann
   `execute in minecraft:the_end run tp LongRangeMissile 0 90 25`, ein paar Sekunden warten, dann
   `execute in minecraft:the_end run kill @e[type=ender_dragon]` → „✔ Enderdrache erledigt“, „Geschafft!
   Zeit: …“, zurück in die Lobby, Bestenliste zeigt Platz 1 mit der Zeit.

Ohne Client (`testing/probes/RunProbe.java`, Aufruf wie `ClusterProbe`): `closed <event-id>` → ein
gescheiterter Lauf bleibt `FAILED`, auch wenn danach noch ein „läuft“ ankommt. `start <event-id>` legt einen
Lauf an und startet seinen Server; nach dem Hochfahren zeigt `state <lauf-id>` `PAUSED`. `abort <lauf-id>` →
im Log des Run-Servers „The run is over and everybody has left - stopping“, im Launcher nach ein bis zwei
Minuten „Discarded run server“. Die Probe-Läufe danach aus `runs.yml` nehmen (Launcher aus).

### Team-Farbe (Survival, /cteam)
`/cteam create Name TAG`, `/cteam` → Farb-Knopf (Wolle) → jede Farbe hat ihre Wolle, die aktuelle trägt
„✔ Aktuelle Farbe“; der Team-Tag im Chat erscheint in der Farbe.

### Discord-Infos (ohne Discord)
`DiscordInfo.view(page, part)` ist package-private und lässt sich offline aufrufen: ein Testprogramm im Paket
`de.hems.utils.bot.info` mit dem Launcher-Jar im Classpath prüft Zahl der Teile, Längen (≤ 4096 pro Text,
≤ 6000 pro Embed, ≤ 25 Menüpunkte) und die Aufteilung der Regeln (`DiscordInfo.rulesMarkdown`).

### Admin-Passwort
`./admin-passwort.sh` in einem Testordner mit einer kopierten `main-config.yml` laufen lassen (Eingaben per
`printf 'admin\n\nn\n' |`), danach muss `password:` ein neuer `pbkdf2-…`-Hash sein und `totp-secret` gleich.

## Aufräumen
- X-Ray-Paket im Client wieder abwählen; Op wieder nehmen (`con LOBBY "deop LongRangeMissile"`).
- Event- und Run-Server stoppt der Idle-Watchdog nach 10 Minuten.
- Die Lobby wird bei jedem Start aus `assets/lobby-spawn.zip` wiederhergestellt; Test-Würfel dort sind weg.
