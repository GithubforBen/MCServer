# Admin-Handbuch

Alles, was Admins und Ops auf dem Netzwerk steuern können. Was Spieler können, steht im Info-Kanal für Spieler. Die technischen Hintergründe stehen im README des Repositorys.

„Admin“ heißt im Spiel **Op**, solange nichts anderes dabeisteht. Einzelne Rechte lassen sich auch ohne Op vergeben (Permission in Klammern).

## Discord-Befehle

**Kanäle einrichten** (Discord-Administrator, im jeweiligen Kanal ausführen):
- `/setticketchannel`: Hier schreiben Spieler Tickets. Der Bot legt die Nachricht mit dem Knopf an.
- `/setticketstaffchannel`: Hier bekommt jedes Ticket einen eigenen Thread
- `/setloggingchannel`: Hier landen Admin-Aktionen aus dem Spiel
- `/setinfochannel`: Hier landet die Erklärung für Spieler
- `/setadmininfochannel`: Hier landet dieses Handbuch
- `/setruleschannel`: Hier landen die Regeln zum Durchblättern
- Steht im Kanal schon etwas, wird er archiviert (umbenannt in „…-archiv“, versteckt) und ein frischer Kanal kommt an seine Stelle

Beide Info-Texte aktualisiert der Bot bei jedem Start des Launchers selbst.

**Nur der Besitzer** (`discord-owner-id` in der `main-config.yml`):
- `/op <name>`, `/deop <name>`: wirkt sofort auf allen laufenden Servern
- `/unlink <name>`: löst eine Discord-Verknüpfung

**Sonstige:**
- `/payingplayer <name>`: trägt einen Unterstützer ein (volle Sichtweite, größerer Team-Rucksack)
- `/verify <name>`: verknüpft den eigenen Account, wie bei Spielern

## Admin-Website

Erreichbar unter `http://<host>:8080/`. Zum Login braucht man **Passwort und Google-Authenticator-Code**. Zwischen zwei Versuchen liegen immer mindestens 3 Sekunden.

**Panels:**
- **Server:** an, aus, neu starten
- **Paying Player:** Unterstützer ein- und austragen
- **Tickets:** lesen, beantworten, übernehmen, schließen
- **Konsole:** Live-Ausgabe eines Servers, Befehle schicken
- **Spieler:** Inventar und Enderchest bearbeiten, Items per Drag & Drop in die Admin-Ablage ziehen
- **Whitelist:** Regeln für die öffentliche Seite `/regeln`, Whitelist an/aus, Selbst-Eintragen an/aus, Liste mit Entfernen
- **Netzwerk:** Neustart, Update, Herunterfahren, laufender Commit, Speicherbudget mit Vorschlägen
- **Einstellungen:** Passwort, 2FA neu einrichten, Admin-Accounts, Besitzer-ID, Ops, Whitelist-Namen, Autostart

Passwort, 2FA und Accounts ändern geht nur mit dem eigenen Passwort.

## Neustart und Updates

`/neustart` geht auf jedem Server und braucht `network.restart`.
- `/neustart`: zeigt, was geplant ist und wie das letzte Update lief
- `/neustart 10`: in 10 Minuten neu starten
- `/neustart 10 update`: in 10 Minuten `git pull`, neu bauen, neu starten
- `/neustart 10 aus`: in 10 Minuten herunterfahren, danach bleibt alles aus
- `/neustart abbrechen`: den geplanten Neustart absagen

Spieler sehen einen Countdown im Chat, eine Bossbar und einen Titel. Baut ein Update nicht, startet automatisch der vorige Stand. Dasselbe geht im Website-Panel **Netzwerk**.

## Server verwalten

- `/servermanger`: Übersicht aller Server, Einstellungen, neuen Server erstellen (Vorlage, Name, RAM, Plugins)
- `/servermanger create <name> [vorlage] [ram] [plugin,…]`
- `/servermanger stop|restart <name>`
- `/rs [server]`: einen Server neu starten, ohne Angabe den eigenen (Survival)

Vorlagen: `LOBBY`, `SURVIVAL`, `BEDWARS`, `EVENT`. Jeder Name kann gestartet werden und bekommt automatisch einen Port.

**Chunks vorladen:** Chunky ist auf jedem Server. `/chunky world world`, `/chunky radius 3000`, `/chunky start`. Mit `/chunky pause`, `continue` und `cancel` steuerst du den Lauf, mit `/chunky progress` siehst du den Stand. Geht auch über die Website-Konsole.

**Speicher:** Der Launcher lehnt Starts ab, die nicht mehr in den Arbeitsspeicher passen. Im Server Manager und im Panel **Netzwerk** steht, welcher Server mehr Speicher hat, als er braucht, mit einem Vorschlag zum Anklicken.

## Moderation

- `/banane <spieler> <HACKING|GRIEFING|OTHER> <zahl> <m|h|d|w>`: zeitlich begrenzter Bann (Survival)
- `/verify wer <spieler>`: wer das auf Discord ist (`network.verify.lookup`)
- **CoreProtect** (auf jedem Server): `/co inspect` zeigt, wer einen Block gesetzt oder abgebaut hat, `/co rollback` macht Griefing rückgängig. Auch Shop-Käufe aus Lagerkisten und der Erntehelfer stehen dort. Die Abfrage geht auch über die Website.

**Admin-Aktionen werden geloggt.** Auf Survival landen `gamemode`, `give`, `tp`, `kill`, `ban`, `kick`, `clear`, `fill`, `setblock` und ähnliche Befehle von Ops im Logging-Kanal, ebenso jeder Spielmoduswechsel. Begründen lässt sich das nachträglich mit `/legitimize <uuid|@all> "<grund>"` (`network.adminabuse.legitimize`). Unter jedem Log-Eintrag auf Discord steht ein Knopf, der ein Ticket zu genau dieser Aktion öffnet.

**Tickets im Spiel** (`network.tickets`): `/ticket offen` zeigt alle offenen, `/ticket <nr> uebernehmen` übernimmt eines. Im Staff-Thread auf Discord geht jede Nachricht an den Spieler, außer sie beginnt mit `//`.

## Admin-Ablage und Geld

- `/admin` (Survival): öffnet die **Admin-Ablage**, eine Kiste, die netzwerkweit dieselbe ist. Items aus Spielerinventaren zieht man auf der Website hinein.
- `/admin join`: Du trittst als „Admin“ auf, mit Name, Skin und der Ablage als Inventar. Nochmal tippen schaltet zurück. Ort und Gamemode merkt sich jede Gestalt selbst: Als Admin bist du wieder im Gamemode vom letzten Mal (z. B. Creative), als Spieler wieder in deinem. Das Log nennt trotzdem deinen echten Namen.
- `/admin money add|remove <spieler> <betrag>` und `/admin money query <spieler>`: Bits verwalten

## Events anlegen

`/events` → **Neues Event**. Dort stellst du Name, Beschreibung, Typ, Start und Dauer ein, darunter liegen die Knöpfe **Einstellungen** (Regeln des Typs) und **Belohnungen**.

**Typen:** Einfaches Event, Andere Welt, Das End öffnet (nur einmal), UHC Alle Bosse, UHC Enderdrache, Bedwars, Pokernacht, Hunger Games

**Belohnungen** bestehen aus Bits und/oder Items aus der Hand und gehen an:
- `#1`, `#2`, `#3`, Top 10, ab Platz X, einen Bereich (z. B. 4–10)
- ab X Kills (nur bei Bedwars und Hunger Games)
- alle Teilnehmer

Jede passende Belohnung wird ausgezahlt. Bedwars-, Pokernacht- und Hunger-Games-Server fährt die Lobby fünf Minuten vorher selbst hoch.

## Spielmodi steuern

**Bedwars** (`bedwars.admin`):
- `/bw status|start|stop`
- `/bw setup <map>`: Map einrichten
- `/bw timeline skip`: nächstes Ereignis sofort auslösen
- `/bw addon <id> on|off`: Extras schalten (nur in der Warte-Lobby)
- `/bw generators`, `/bw stats`, `/bw watch`, `/bw reload`
- `/bwdebug` (Lobby): Testrunde erstellen und hinspringen

**Eigene Runden:** `/runde admin` schaltet frei, dass Spieler eigene Bedwars-Runden starten dürfen (standardmäßig **aus**). Dort stellst du auch Limits, Wartezeit, Sperre bei Events und Speicher pro Runde ein.

**Hunger Games:** `/hg start` startet sofort (allein als Test), `/hg stop` beendet ohne Sieger, `/hg mitte` setzt die Mitte der Karte.

**Pokernacht:** `/poker setup` legt Tische und Eingang nach einem Umbau neu fest, `/poker karte speichern` sichert die Casino-Welt für die nächste Nacht.

**Speedrun-Server (UHC):** `/reset` setzt den Server für den nächsten Versuch zurück.

## Lobby einrichten

**NPCs** (`network.npc.admin`):
- `/npc warp <server> [name]`: Warp-NPC an deine Position
- `/npc events [name]`: Event-NPC
- `/npc list`, `/npc tp <id>`, `/npc hier <id>`, `/npc weg <id>`
- `/npc name <id> <name>` (mit `&`-Farbcodes), `/npc skin <id> <spieler|aus>`, `/npc ziel <id> <server>`

**Parkour:** `/parkour setup <strecke> start`, dann `checkpoint`, `undo`, `finish`, `board`, `name`, `delete`

**Lotto** (`network.lotto.admin`):
- `/lotto ziehen`: sofort ziehen
- `/lotto termin sonntag 20:00`, `/lotto preis 100`
- `/lotto stand`, `/lotto standweg`: Lotto-Stand in der Lobby aufstellen oder entfernen

## Cosmetics und Whitelist

**Cosmetics:** `/cosmetics` → **Verwalten**. Linksklick schaltet zwischen verkäuflich, nur besitzbar und aus. Rechtsklick erhöht den Preis, Shift macht ein Cosmetic für alle gratis.

**Whitelist:** Sie ist standardmäßig **aus**. Spieler können sich trotzdem schon auf `/regeln` eintragen. Erst wenn sie im Website-Panel eingeschaltet wird, kommt niemand mehr ohne Eintrag rein. Ops kommen immer rein. Ein `/whitelist add` im Spiel überlebt keinen Neustart, nimm dafür die Website.

## Bekannte Einschränkungen

- Paper 26.3 und WorldEdit sind noch **Beta**-Versionen.
- Backups vor einem Versionswechsel macht der Launcher selbst. Gelöscht werden sie nie, also auf den Plattenplatz achten.
