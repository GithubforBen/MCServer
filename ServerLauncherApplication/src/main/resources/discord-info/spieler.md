# Was es auf dem Server gibt

Alles, was du als Spieler auf dem Netzwerk machen kannst, mit den Befehlen dazu. Jeder Abschnitt ist eine eigene Nachricht, damit du direkt zu dem springen kannst, was dich interessiert.

Fragen, Bugs, Ideen: schreib ein **Ticket** (siehe ganz unten).

## Reinkommen und herumkommen

**Adresse:** `{adresse}`

**Whitelist:** Auf der Regeln-Seite liest du die Regeln, setzt den Haken bei „akzeptiere“ und gibst deinen Minecraft-Namen ein. Danach kommst du sofort rein, ohne dass jemand etwas freischalten muss. Wenn du beim Joinen abgewiesen wirst, steht der Link in der Meldung.
**Regeln-Seite:** {regeln}

**Zwischen den Servern wechseln:**
- `/warp`: Menü mit allen Servern, die gerade laufen
- `/warp <server>`: direkt hinspringen
- `/lobby` (oder `/hub`, `/leave`): zurück in die Lobby, auf Bedwars-, Hunger-Games-, Casino- und Speedrun-Servern

**Tabliste:** Oben steht, wie viele im ganzen Netzwerk online sind. Unten stehen das laufende oder nächste Event und die wichtigsten Befehle des Servers, auf dem du gerade bist.

## Discord verknüpfen

Mit der Verknüpfung wissen die Admins, wer du auf Discord bist. Du bekommst Antworten auf Tickets dann im Spiel **und** per Direktnachricht.

1. Hier auf Discord: `/verify <dein Minecraft-Name>`. Du bekommst einen Code mit sechs Zeichen, zehn Minuten gültig.
2. Im Spiel: `/verify <code>`

`/verify` ohne Code zeigt dir, mit welchem Discord-Account du verknüpft bist.

## Lobby

- **NPCs:** Klick einen an. Die Warp-NPCs bringen dich auf ihren Server und zeigen über dem Kopf, ob er läuft und wie viele drauf sind. Der Event-NPC öffnet den Eventkalender.
- **Parkour:** `/parkour list` zeigt alle Strecken mit Bestzeit, `/parkour start <strecke>` startet einen Lauf, `/parkour leave` bricht ab, `/parkour top <strecke>` zeigt die Bestenliste.
- **Lotto-Stand:** Ein Villager, an dem du Lotto spielst (siehe Lotto).
- **Gadgets:** Was du unter Cosmetics als Lobby-Gadget angelegt hast, bekommst du hier ins Inventar.

## Survival: Teams und Grundstücke

Auf Survival spielst du im Team. Mit `/cteam` öffnet sich der Team-Manager, dort geht fast alles per Klick.

- `/cteam create <name> <tag>`: Team gründen
- `/cteam invite <spieler>`, `/cteam invite accept|reject`: Einladungen
- `/cteam join <team>`: einem offenen Team beitreten
- `/cteam leave`, `/cteam info [team]`, `/cteam list`
- `/cteam sethome`, `/cteam home`: Team-Home (kurz stillstehen, Bewegen bricht ab)

**Grundstücke (Claims):** `/cteam claim` kauft den Chunk, in dem du stehst, mit Bits. Jeder weitere Chunk kostet etwas mehr. `/cteam unclaim` gibt ihn wieder frei.
- `/cteam chunks`: Karte mit einer Farbe pro Team
- `/cteam grenze`: zeigt zehn Sekunden lang die Chunk-Grenzen um dich herum
- Beim Betreten eines Grundstücks erscheint der Teamname als Titel. `/cteam titel` schaltet das für dich ab.

Unter *Einstellungen* im Team-Manager legt euer Team selbst fest, wie viele Mitglieder ihr aufnehmt, ob Friendly Fire an ist, ob jeder beitreten darf und wer claimen und einladen darf.

## Geld (Bits)

Bits sind die Währung im ganzen Netzwerk. Damit bezahlst du Grundstücke, Shops, Cosmetics, Lotto und Poker.

**Ein Diamant = 100 Bits.**
- **Geldautomat:** Auf Survival mit **Schleichen + Rechtsklick auf eine Endertruhe**. Dort zahlst du Diamanten ein oder lässt dir Bits als Diamanten auszahlen.
- **Teamkasse:** `/cteam atm` öffnet denselben Automaten für das Konto deines Teams.

Die Teamkasse zieht bei einer Umbenennung mit. Wird ein Team aufgelöst, bekommt der Anführer den Kontostand.

## Shops und Marktplatz (Survival)

- **Shop eröffnen:** Stell dich auf eine Kiste auf dem Grundstück deines Teams und gib `/shop create <name>` ein. Das kostet 2000 Bits. Die Kiste ist das Lager, daneben steht dein Händler.
- **Einstellen:** Schleichen + Rechtsklick auf deinen Händler. Dort stellst du Angebote, Preise und Mengen ein, wechselst die Kiste oder versetzt den Händler.
- **Kaufen:** Rechtsklick auf einen Händler.
- **Marktplatz:** `/shop` zeigt die Angebote aller Shops auf einmal.

## Team-Rucksack

`/backpack` (oder `/bp`) öffnet den Rucksack, den sich dein ganzes Team teilt. Auf jedem Server ist es derselbe.

Normalerweise hat er 27 Plätze (eine Kiste). Sind die **Unterstützer** in deinem Team in der Mehrheit, werden es 54 (eine Doppelkiste). Gleichstand zählt nicht als Mehrheit.

## Unterstützer

Wer den Server finanziell unterstützt, bekommt:
- **volle Sichtweite**, auch wenn Survival laggt. Ohne Unterstützung wird die Sichtweite bei Lag gesenkt, damit der Server spielbar bleibt.
- zusammen mit dem Team einen **größeren Rucksack** (siehe Team-Rucksack)

Eingetragen wird man von den Admins. Frag einfach per Ticket nach.

## Events

`/events` öffnet den Eventkalender. Dort siehst du, was gerade läuft und was als Nächstes kommt, und bei jedem Event, worum gespielt wird (Knopf **Belohnungen**) und mit welchen Regeln (Knopf **Einstellungen**).

**Arten von Events:**
- **Bedwars:** Eine Runde zur festen Zeit. Wer in der Lobby ist, wird mitgenommen.
- **Hunger Games:** Siehe eigener Abschnitt.
- **Pokernacht:** Siehe eigener Abschnitt.
- **UHC: Alle Bosse / Enderdrache:** Ein Speedrun gegen die Uhr. Jeder Versuch läuft auf einem eigenen Server im Hardcore-Modus, die schnellste Zeit gewinnt.
- **Das End öffnet:** Ab diesem Event ist das End auf Survival offen.
- **Einfaches Event / Andere Welt:** Ankündigungen der Admins

**Belohnungen** gibt es für Plätze, ab einer bestimmten Anzahl Kills oder einfach fürs Mitmachen. Wer offline ist, bekommt sie beim nächsten Join. Ein Bedwars- oder Hunger-Games-Event endet erst, wenn das Spiel vorbei ist. Läuft die Zeit vorher ab, geht es in die Verlängerung.

## Bedwars und eigene Runden

Bedwars wie bei Hypixel, mit den Maps **Speedway, Lighthouse, Orbit** (je 8 Teams) und **Aquarium** (4 Teams). Dazu kommen Extras, die pro Runde an- oder abgeschaltet sein können: Bett-Token (holt das eigene Bett zurück), Kits, Spezial-Items (Enterhaken, Rettungsplattform, Brücken-Ei, Sprungfeder), Killstreaks und Zufallsereignisse.

**Eigene Runde (`/runde`)**, wenn die Admins es freigeschaltet haben:
- `/runde` zeigt alle offenen Runden
- `/runde start`: eigene Runde mit Map, Modus (Solo bis 4er-Teams) und Extras, öffentlich oder privat
- `/runde einladen <spieler>`: jemanden in deine private Runde holen

Als Ersteller kannst du die Runde sofort starten, privat schalten und Spieler rauswerfen. Wenn der Knopf nicht geht, steht dabei, warum (zum Beispiel weil gleich ein Event startet).

## Hunger Games

Alle in eine Welt. In der Mitte steht das Füllhorn mit dem besten Loot, über die Karte fallen Supply Drops (Koordinaten im Chat, mit Lichtsäule). Gegen Ende zieht sich die Weltgrenze bis zum Showdown zusammen.

- Fünf Minuten vorher wirst du eingeladen, zur Startzeit geht es los
- Am Anfang gibt es eine Schutzzeit, in der niemand Schaden macht
- Wer stirbt oder geht, ist raus und schaut zu. Der Letzte gewinnt.
- Plätze ergeben sich aus der Reihenfolge des Ausscheidens, Kills zählen für Belohnungen
- `/hg` zeigt den Stand des Spiels

## Pokernacht

Texas Hold'em um echte Bits: **1 Bit = 1 Chip.** Ein paar Minuten vorher öffnet das Casino. Hin gehst du selbst, niemand wird hinübergezogen.

- **Hinsetzen:** Rechtsklick auf einen Stuhl. Gespielt wird über die Hotbar, auf den Knöpfen steht der Preis (z. B. „Mitgehen 240“).
- `/poker`: Einsätze, Regeln, dein Konto
- `/poker nachkaufen`: noch ein Buy-in (nicht im Turnier)
- `/poker aufstehen`: deine Chips werden wieder zu Bits
- `/poker bot`, `/poker bot weg`: einen Bot an den Tisch setzen (du zahlst seinen Stack und eine Gebühr) oder deine Bots abräumen

**Turnier:** Ein Buy-in, die Blinds steigen, wer raus ist, ist raus. Die vorderen Plätze teilen sich den Topf.
**Rangliste:** Gewertet wird, was du unterm Strich gewonnen hast. Dafür brauchst du eine Mindestzahl an Händen.
Stürzt der Casino-Server ab, wird zurückgezahlt, was vor dir lag (im Turnier das Buy-in). Du verlierst den Abend, aber nie das Geld.

## Cosmetics und Gadgets

`/cosmetics` auf jedem Server (auf Survival auch über den Marktplatz) ist zum Kaufen, Anlegen und Ablegen da. Bezahlt wird mit Bits, und alles gilt im ganzen Netzwerk.

- **Sieges-Effekte:** Raketen (gratis), Tinte, Gewitter, Lichtsäule
- **Kill-Effekte:** Blitzschlag, Seelen, Stichflamme
- **Partikelspuren:** Flammenspur, Sternenstaub, Noten
- **Gadgets für die Lobby:** Doppelsprung, Raketenstiefel, Schneeball-Kanone, Disco-Boden, Fußspuren, Sprungpad, Reittier
- **Gadgets für Survival:** Erntehelfer, Sitzen, Werkbank für unterwegs
- **Gadgets für beide:** Endlos-Perle, Enterhaken, Haustier, Ballon, Konfetti-Kanone, Eigenes Wetter, Chat-Blase, Emotes

Du kannst in der Lobby und auf Survival je ein Gadget tragen. In Bedwars gibt es keine Gadgets, denn ein Gadget soll nie ein Spielvorteil sein.

## Lotto

4 aus 15, jede Woche (Standard: Sonntag 20:00). Wer alle vier trifft, bekommt den **ganzen Topf**. Der gesamte Einsatz geht in den Topf, trifft niemand, wächst er weiter.

- `/lotto`: Tippschein öffnen (Zahlen anklicken, zufällig ausfüllen, Quicktipps)
- `/lotto tipp 3 7 11 14`: direkt tippen
- `/lotto quick [anzahl]`: 1 bis 20 zufällige Tipps
- `/lotto info`: Topf, nächste Ziehung, letzte Zahlen
- `/lotto meine`: deine Tipps dieser Runde

Ein Tipp kostet standardmäßig 100 Bits, die Chance liegt bei 1 zu 1365. Der Gewinn geht direkt aufs Konto, auch wenn du offline bist.

## Hilfe: Tickets

Bug gefunden, jemanden melden, eine Idee oder eine Frage? Schreib ein Ticket.

- **Auf Discord:** Knopf **„Ticket schreiben“** im Ticket-Kanal. Antworten bekommst du per Direktnachricht und antwortest über die Knöpfe dort.
- **Im Spiel:**
  - `/ticket neu`: neues Ticket
  - `/ticket`: deine Tickets
  - `/ticket <nr>`: ein Ticket lesen
  - `/ticket <nr> antworten [text]`
  - `/ticket <nr> schliessen` / `oeffnen`

Es ist dasselbe Ticket, egal wo du schreibst. Mit verknüpftem Discord (`/verify`) erreichen dich Antworten an beiden Stellen.
