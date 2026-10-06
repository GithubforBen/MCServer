# Was es auf dem Server gibt

Alles, was du als Spieler auf dem Netzwerk machen kannst, mit den Befehlen dazu. Jeder Abschnitt ist eine eigene Nachricht, damit du direkt zu dem springen kannst, was dich interessiert.

Fragen, Bugs, Ideen: schreib ein **Ticket** (siehe ganz unten).

## Reinkommen und herumkommen

**Adresse:** `{adresse}`

**Whitelist:** Auf der Regeln-Seite liest du die Regeln, setzt den Haken bei „akzeptiere“ und gibst deinen Minecraft-Namen ein. Danach kommst du sofort rein, ohne dass jemand etwas freischalten muss. Wenn du beim Joinen abgewiesen wirst, steht der Link in der Meldung.
**Regeln-Seite:** {regeln}
**Regeln auf Discord:** `/regeln` zeigt sie dir zum Durchblättern.

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
- `/cteam sethome`, `/cteam home`: Team-Home. Jeder Teleport kostet **500 Bits** (5 Diamanten) von deinem eigenen Konto. Du musst kurz stillstehen; wenn du dich bewegst, bricht der Teleport ab und du zahlst nichts.

**Grundstücke (Claims):** `/cteam claim` kauft den Chunk, in dem du stehst, mit Bits von deinem eigenen Konto. Das darf **jedes Mitglied** des Teams. Jeder weitere Chunk kostet etwas mehr. `/cteam unclaim` gibt ihn wieder frei; das darf der Anführer, und die Mitglieder nur, wenn er es in den Einstellungen erlaubt.
- `/cteam chunks`: Karte mit einer Farbe pro Team
- `/cteam grenze`: zeigt zehn Sekunden lang die Chunk-Grenzen um dich herum
- Beim Betreten eines Grundstücks erscheint der Teamname als Titel. `/cteam titel` schaltet das für dich ab.

Unter *Einstellungen* im Team-Manager legt euer Team selbst fest, wie viele Mitglieder ihr aufnehmt, ob Friendly Fire an ist, ob jeder beitreten darf, wer einladen darf und ob Mitglieder Chunks freigeben dürfen.

## Survival: Sichtweite und Kampf

**Sichtweite:** Wenn Survival laggt, senkt der Server deine Sichtweite automatisch und hebt sie wieder an, sobald es besser läuft. Wem das zu unruhig ist, der setzt ein **eigenes Limit**. Über diesen Wert geht deine Sichtweite dann nie, und du merkst nur noch etwas, wenn der Lag sie noch weiter drückt.
- `/sichtweite`: zeigt deine Sichtweite, dein Limit und was der Server dir ohne Limit gerade gäbe
- `/sichtweite <2-32>`: setzt dein Limit in Chunks, zum Beispiel `/sichtweite 8`
- `/sichtweite aus`: nimmt das Limit wieder weg

Das Limit bleibt gespeichert, auch nach dem Ausloggen und nach Neustarts. Ein Limit, das so hoch ist wie das, was du bei Lag noch bekommst, ändert sich praktisch nie. Ein niedrigeres Limit kann außerdem auf schwachen Rechnern die FPS verbessern.

**Attribute Swapping ist erlaubt:** Wie in Vanilla kann ein Schlag die Werte von zwei Items verbinden, wenn du im richtigen Moment in der Hotbar wechselst. Ein Beispiel ist der **Speer** zusammen mit Schwert, Axt oder Streitkolben. Das ist kein Bug, sondern bewusst freigeschaltet, und es gilt nur auf Survival.

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
- **volle Sichtweite**, auch wenn Survival laggt. Ohne Unterstützung wird die Sichtweite bei Lag gesenkt, damit der Server spielbar bleibt. Ein eigenes Limit mit `/sichtweite` kann trotzdem jeder setzen.
- zusammen mit dem Team einen **größeren Rucksack** (siehe Team-Rucksack)

Eingetragen wird man von den Admins. Frag einfach per Ticket nach.

## Events

`/events` öffnet den Eventkalender. Dort siehst du, was gerade läuft und was als Nächstes kommt, und bei jedem Event, worum gespielt wird (Knopf **Belohnungen**) und mit welchen Regeln (Knopf **Einstellungen**).

**Arten von Events:**
- **Bedwars:** Eine Runde zur festen Zeit. Wer in der Lobby ist, wird mitgenommen.
- **Hunger Games:** Siehe eigener Abschnitt.
- **Pokernacht:** Siehe eigener Abschnitt.
- **UHC: Alle Bosse / Enderdrache:** Ein Speedrun gegen die Uhr. Jeder Versuch läuft auf einem eigenen Server im Hardcore-Modus, die schnellste Zeit gewinnt. Ein Lauf, der nichts mehr wird: `/reset` auf dem Run-Server bricht ihn ab und startet sofort den nächsten Versuch. Ihr wartet kurz in der Lobby und werdet verbunden, sobald der neue Server bereit ist – sind gerade mehrere Spieler im Event, oft auf einem schon vorbereiteten Server fast ohne Wartezeit. Nur abbrechen: `/abbrechen` oder „Lauf abbrechen“ im Event-Menü. Ein abgebrochener Lauf zählt trotzdem als Versuch.
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

## Lotto: so wird gezogen

**Wann:** Zum festen Termin zieht der Server von selbst, niemand muss dafür online sein. War der Server zu dem Zeitpunkt aus, wird gezogen, sobald er wieder läuft. Bis zur Ziehung kannst du tippen. Was danach kommt, zählt für die nächste Runde.

**Wie:** Aus den Zahlen 1 bis 15 werden vier verschiedene gezogen, eine nach der anderen und ohne Zurücklegen. Jede Zahl ist gleich wahrscheinlich. Der Zufall kommt aus einem sicheren Zufallsgenerator, den niemand vorhersagen oder beeinflussen kann, auch die Admins nicht. Die Reihenfolge ist egal, es zählt nur, welche vier Zahlen es sind.

**Wer gewinnt:** Nur ein Tipp mit allen vier Zahlen gewinnt. Für drei Richtige gibt es nichts.
- **Ein Gewinner:** Er bekommt den ganzen Topf.
- **Mehrere Gewinner:** Der Topf wird gleichmäßig auf die Gewinner-Tipps verteilt. Wer zweimal dieselben richtigen Zahlen getippt hat, bekommt zwei Anteile. Was sich nicht glatt teilen lässt, bleibt im Topf.
- **Kein Gewinner:** Der ganze Topf geht in die nächste Runde.

Hat in einer Runde niemand getippt, wird nicht gezogen und der Topf wartet auf den nächsten Termin. Nach jeder Ziehung stehen die Zahlen und die Gewinner im Chat, später zeigt `/lotto info` sie noch einmal.

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
