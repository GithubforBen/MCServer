# Netzwerk-Abnahme

Testplan für die Änderungen aus fünf Runden: Serverstart und Warp, Shop und ATM,
die mitgelieferte Bedwars-Map, die Speichergrenzen — und die Korrekturen an Map, Kampf,
Werkzeugen und Nachrichten sowie der Parkour, die danach dazugekommen sind.

Stand: 2026-08-30 · 115 Prüfungen in neunzehn Phasen

Die Phasen bauen aufeinander auf — ohne laufenden Launcher lässt sich über RunPlugin nichts
sagen, und ohne aktives RunPlugin nichts über Warp und Fortschritt. Also von oben nach unten.

**Schweregrade**

| Marker | Bedeutung |
|---|---|
| `BLOCKER` | Geht das nicht, ist der Rest der Runde hinfällig |
| `WICHTIG` | Kern der Änderung |
| `NORMAL` | Detail am Rand |
| `REGRESSION` | War vorher da und muss es bleiben |

**Die 34 Blocker vorweg:** 1.1 · 2.1 · 2.2 · 3.1 · 3.3 · 4.1 · 6.1 · 6.2 · 7.2 · 8.4 · 10.3 · 10.4 · 10.5 · 11.1 · 11.3 · 11.4 · 11.7 · 13.1 · 13.2 · 13.3 · 13.5 · 14.1 · 14.4 · 14.6 · 14.7 · 14.8 · 15.1 · 16.1 · 16.3 · 16.4 · 17.2 · 17.5 · 19.1 · 19.4

---

## 01 — Launcher und Arbeitsspeicher

Zuerst muss das Netzwerk überhaupt hochkommen — und mit den Speichergrenzen dieser Maschine.

- [ ] **1.1 Launcher startet durch** · `BLOCKER`
  - Ablauf: `./start.sh`, dann `tmux attach -t server`
  - Erwartet: Velocity, Lobby und Survival kommen hoch. In `main-config.yml` steht jetzt ein
    neuer Schlüssel `idle-shutdown-minutes: 10` mit Kommentar.

- [ ] **1.2 RAM-Deckel greift** · `WICHTIG`
  - Ablauf: Launcher-Konsole beim Start eines Servers beobachten
  - Erwartet: Für jeden Server, der mehr wollte als erlaubt ist, eine Zeile:
    ```
    Memory for SURVIVAL: 4096 MB requested, 1536 MB granted
    (MCSERVER_MAX_MEMORY_MB/MCSERVER_MEMORY_PERCENT)
    ```

- [ ] **1.3 Die Variable kommt wirklich an** · `WICHTIG`
  - Ablauf: In der tmux-Session `echo $MCSERVER_MAX_MEMORY_MB`
  - Erwartet: `1536`. Kommt nichts, hat `start.sh` die `.env.local` nicht gesourced und der
    Deckel ist wirkungslos — dann greift 1.2 auch nicht.

- [ ] **1.4 Ohne Datei bleibt alles wie vorher** · `REGRESSION`
  - Ablauf: `.env.local` kurz umbenennen, Launcher neu starten
  - Erwartet: Survival bekommt wieder die vollen 4096 MB, keine `Memory for …`-Zeilen.
  - Warum: Die Datei ist absichtlich nicht im git. Auf der Produktionsmaschine gibt es sie
    nicht — dort müssen die Template-Werte unverändert gelten.

---

## 02 — RunPlugin ist wieder aktiv

Der Absturz, der die Fehlermeldung erzeugt hat: der Server hat versucht, sich unter dem
Namen `.` zu registrieren.

- [ ] **2.1 Plugin ist enabled** · `BLOCKER`
  - Ablauf: Event- bzw. Run-Server starten, dort `/pl`
  - Erwartet: **RunPlugin steht grün** in der Liste, nicht rot.

- [ ] **2.2 Der alte Fehler ist weg** · `BLOCKER`
  - Ablauf: `grep -r "not a usable server name" servers/*/logs/latest.log`
  - Erwartet: Keine Treffer. Vorher stand dort
    `IllegalArgumentException: '.' is not a usable server name` und das Plugin blieb für die
    Lebensdauer des Servers deaktiviert.

- [ ] **2.3 Server ist unter seinem echten Namen im Netz** · `WICHTIG`
  - Ablauf: Launcher-Konsole bzw. `/servermanger` aus der Lobby
  - Erwartet: Der Run-Server taucht unter seinem Verzeichnisnamen auf (z. B. `RUN_2B413CAB`),
    nicht als `.` oder `EVENT`.

- [ ] **2.4 Befehle des Plugins gehen** · `WICHTIG`
  - Ablauf: Auf dem Run-Server `/events`, danach `/reset`
  - Erwartet: Der Eventkalender öffnet. Vorher kam
    `Cannot execute command 'events' … plugin is disabled`.

---

## 03 — Serverstart mit Fortschritt

Der Kern der ersten Runde: gewarpt wird erst, wenn Paper wirklich fertig ist — und bis dahin
sieht man, was passiert.

- [ ] **3.1 Die Meldungen kommen in dieser Reihenfolge** · `BLOCKER`
  - Ablauf: In der Lobby `/bwdebug`
  - Erwartet: Im Chat, je einmal pro Schritt:
    ```
    In der Startwarteschlange ...
    Server startet ...
    Terrain wird gebaut ...
    Warp wird vorbereitet ...
    Server bereit.
    Du wirst nach BEDWARS gewarpt...
    ```

- [ ] **3.2 Balken über der Hotbar zählt hoch** · `WICHTIG`
  - Ablauf: Während 3.1 auf die Actionbar schauen
  - Erwartet: Ein Balken, der sich füllt, und bei „Terrain wird gebaut" eine Prozentzahl.
    **Eine** Chatzeile pro Schritt, keine pro Prozent.

- [ ] **3.3 Der Warp landet auch wirklich** · `BLOCKER`
  - Ablauf: 3.1 abwarten, ohne selbst etwas zu tun
  - Erwartet: Du stehst auf dem neuen Server. Kein Rauswurf, kein „Server nicht erreichbar"
    vom Proxy — das war der ursprüngliche Fehler.
  - Warum: Paper öffnet seinen Port lange bevor es Spieler annimmt. Der Launcher liest den
    Fortschritt jetzt aus der Serverkonsole statt am Port zu raten.

- [ ] **3.4 `/warp` wartet auf einen startenden Server** · `WICHTIG`
  - Ablauf: Server über `/servermanger` anlegen, sofort danach `/warp <name>`
  - Erwartet: Statt „läuft gerade nicht" die Meldung
    `… ist noch nicht bereit - du wirst verbunden, sobald er es ist.` und danach der Warp.

- [ ] **3.5 Server-Manager macht dasselbe** · `WICHTIG`
  - Ablauf: `/servermanger` → Neuer Server → Vorlage, RAM, Plugins → *Server starten*
  - Erwartet: Gleiche Fortschrittsmeldungen, danach automatischer Warp.

- [ ] **3.6 Die Liste zeigt den Zustand ehrlich** · `NORMAL`
  - Ablauf: Während ein Server hochkommt `/servermanger` öffnen
  - Erwartet: Gelbe Wolle, Status `Terrain wird gebaut 42%`. Fertige Server sind grün,
    gestoppte rot.

- [ ] **3.7 Ausloggen während des Wartens** · `NORMAL`
  - Ablauf: Start anstoßen, sofort ausloggen, Launcher-Log ansehen
  - Erwartet: Keine Exception. Der Server kommt trotzdem hoch, es wird nur niemand mehr
    benachrichtigt.

---

## 04 — Wieder herauskommen

Auf Event- und Bedwars-Servern gab es schlicht kein `/warp` — deshalb kam man da nicht weg.

- [ ] **4.1 `/lobby` auf dem Bedwars-Server** · `BLOCKER`
  - Ablauf: Auf einen Bedwars-Server warpen, `/lobby`
  - Erwartet: Du landest in der Lobby. `/hub` und `/leave` tun dasselbe.

- [ ] **4.2 Die Befehle gibt es auf dem Event-Server** · `WICHTIG`
  - Ablauf: Auf dem Run-Server `/warp`, `/lobby`, als Op `/servermanger`
  - Erwartet: Alle drei existieren und antworten. Vorher standen sie nicht in der `plugin.yml`.

- [ ] **4.3 `/lobby` geht auch ohne Launcher** · `WICHTIG`
  - Ablauf: Launcher stoppen (Velocity und der Spielserver laufen weiter), dann auf dem
    Spielserver `/lobby`
  - Erwartet: Der Warp geht trotzdem — er läuft direkt über den Proxy und braucht den Host nicht.
  - Warum: Genau dann steckt man fest: wenn das kaputt ist, was einem sonst sagt, wohin man darf.

- [ ] **4.4 Nicht-Op bekommt einen Ausweg genannt** · `NORMAL`
  - Ablauf: Ohne Op-Rechte `/warp gibtsnicht`
  - Erwartet: Fehlermeldung plus `Mit /lobby kommst du zurück in die Lobby.`

- [ ] **4.5 Admin kommt trotzdem durch** · `NORMAL`
  - Ablauf: Als Op bei gestopptem Launcher `/warp LOBBY`
  - Erwartet: `Der Host bestätigt 'LOBBY' nicht - der Warp wird trotzdem versucht.` und der
    Proxy entscheidet.
  - Warum: Ein Admin fragt meistens gerade deshalb, weil etwas kaputt ist. Ein „Nein" von der
    kaputten Komponente ist der Weg, wie jemand festsitzt.

---

## 05 — Server-Manager und Spielerübersicht

Neu: wer gerade wo ist. Die Antwort kommt von den Servern selbst, nicht vom Launcher.

- [ ] **5.1 Spielerzahl steht am Item** · `NORMAL`
  - Ablauf: Mit zwei Accounts auf verschiedenen Servern, dann `/servermanger`
  - Erwartet: Stackgröße der Wolle = Spielerzahl, im Lore `Spieler: N` und die ersten fünf Namen.

- [ ] **5.2 Die Übersicht selbst** · `WICHTIG`
  - Ablauf: Im Server-Manager auf *Spieler (N)* klicken
  - Erwartet: Ein Kopf pro Spieler, im Lore Server, Welt, Modus, Leben; Operatoren sind markiert.

- [ ] **5.3 Klick auf einen Kopf warpt hin** · `NORMAL`
  - Ablauf: In der Übersicht einen Spieler auf einem anderen Server anklicken
  - Erwartet: Du landest auf dessen Server.

- [ ] **5.4 Stummer Server wird nicht als leer gezählt** · `NORMAL`
  - Ablauf: Auf einem Spielserver das Plugin deaktivieren (oder einen Server ohne unsere
    Plugins starten), dann die Liste ansehen
  - Erwartet: `Spieler: meldet sich nicht` — nicht `Spieler: 0`.
  - Warum: Der Unterschied entscheidet, ob der Wächter den Server abschalten darf.

---

## 06 — Server schalten sich selbst ab

Der Fall aus dem Bericht: Server lief weiter, nachdem alle weg waren. Zum Testen die Wartezeit
kurz stellen.

- [ ] **6.1 Leerer Event-Server geht aus** · `BLOCKER`
  - Ablauf: In `main-config.yml` `idle-shutdown-minutes: 2` setzen, Launcher neu starten.
    Event- oder Bedwars-Server starten, kurz joinen, wieder gehen.
  - Erwartet: Nach gut zwei Minuten im Launcher-Log:
    ```
    Stopping BEDWARS - nobody has been on it for 2 minutes.
    ```

- [ ] **6.2 Lobby und Survival bleiben** · `BLOCKER`
  - Ablauf: Beide leer stehen lassen, während 6.1 läuft
  - Erwartet: Beide laufen weiter. Alles aus `autostart` und die Lobby sind geschützt.
  - Warum: Die sollen um vier Uhr morgens leer sein und trotzdem noch da.

- [ ] **6.3 Besetzter Server bleibt oben** · `WICHTIG`
  - Ablauf: Auf dem Event-Server stehen bleiben und warten
  - Erwartet: Nichts passiert. Der Zähler beginnt erst, wenn der Proxy null Spieler meldet.

- [ ] **6.4 Abschaltbar** · `NORMAL`
  - Ablauf: `idle-shutdown-minutes: 0`, Launcher neu starten
  - Erwartet: Kein Server wird mehr automatisch gestoppt.

- [ ] **6.5 Danach wieder auf einen sinnvollen Wert** · `WICHTIG`
  - Ablauf: Wert auf das setzen, was du dauerhaft willst
  - Erwartet: Nicht vergessen — sonst testest du ab hier mit einer Zwei-Minuten-Abschaltung
    im Rücken.

---

## 07 — Inventare aktualisieren statt neu öffnen

Betrifft jedes Menü im Projekt, weil die Änderung im gemeinsamen Klickpfad sitzt.

- [ ] **7.1 Marktplatz flackert nicht mehr** · `WICHTIG`
  - Ablauf: `/shop`, dann Tabs, Sortierung und die beiden Filter durchklicken
  - Erwartet: Der Inhalt wechselt, das Fenster bleibt stehen. Kein kurzes Zugehen und Aufgehen.

- [ ] **7.2 Was auf dem Cursor liegt, bleibt liegen** · `BLOCKER`
  - Ablauf: Im Shopkeeper-Verwaltungsmenü ein Item auf den Mauszeiger nehmen und dann einen
    Knopf klicken, der die Ansicht wechselt
  - Erwartet: Das Item bleibt am Cursor. Vorher schloss das Menü und warf es zu Boden.
  - Warum: Das ist der Grund, warum Updaten sicherer ist als Neuöffnen — nicht nur das Flackern.

- [ ] **7.3 Shop bleibt nach dem Kauf offen** · `WICHTIG`
  - Ablauf: Rechtsklick auf einen Shopkeeper, ein Angebot kaufen
  - Erwartet: Der Shop bleibt offen und der Lagerbestand im Lore ist sofort um die gekaufte
    Menge kleiner.

- [ ] **7.4 Aktualisieren im Server-Manager** · `NORMAL`
  - Ablauf: `/servermanger` → Uhr-Symbol
  - Erwartet: Liste erneuert sich im selben Fenster.

- [ ] **7.5 Zurück-Knöpfe** · `NORMAL`
  - Ablauf: In ATM, Marktplatz und Shopkeeper-Menü jeweils zurück navigieren
  - Erwartet: Kein Fenster geht dabei zu und wieder auf.

---

## 08 — Shopkeeper verschieben

Neuer Knopf im Verwaltungsmenü, neben „Kistenstandort ändern".

- [ ] **8.1 Verschieben klappt** · `WICHTIG`
  - Ablauf: Schleichen + Rechtsklick auf den eigenen Shopkeeper → *Shop verschieben* → einen
    Block im eigenen Chunk anklicken
  - Erwartet: Partikel zeigen währenddessen zum Shop. Der Villager steht danach oben auf dem
    Block und schaut in deine Blickrichtung.

- [ ] **8.2 Fremder Chunk wird abgelehnt** · `WICHTIG`
  - Ablauf: Beim Verschieben einen Block außerhalb eures Claims anklicken
  - Erwartet: `Dieser Chunk gehört deinem Team nicht.` und der Shop bleibt, wo er war.

- [ ] **8.3 Fremder Shop wird abgelehnt** · `WICHTIG`
  - Ablauf: Mit einem zweiten Account versuchen, den Shop eines anderen Teams zu verschieben
  - Erwartet: `Dieser Shop gehört dir nicht.`

- [ ] **8.4 Nach dem Neustart genau ein Villager** · `BLOCKER`
  - Ablauf: Verschieben, Survival neu starten (`/rs`), zur alten *und* zur neuen Stelle gehen
  - Erwartet: Ein Villager am neuen Ort, keiner am alten. Kein Duplikat.
  - Warum: Der Villager wird teleportiert und nicht neu gespawnt — Killen und Neuspawnen ist
    genau der Weg, wie ein Shop zwei oder null Villager bekommt.

- [ ] **8.5 Kistenstandort ändern geht weiterhin** · `REGRESSION`
  - Ablauf: *Kistenstandort ändern* → eine andere Kiste im eigenen Claim anklicken
  - Erwartet: `Kiste gesetzt: (x, y, z)`, und Käufe kommen danach aus der neuen Kiste.

---

## 09 — Preise und Mengen

Schrittweite als Komparator links oben; Rechtsklick mehr, Linksklick weniger.

- [ ] **9.1 Schrittweite lässt sich durchklicken** · `WICHTIG`
  - Ablauf: Im Preis-Editor den Komparator links-, dann rechtsklicken
  - Erwartet: Vorwärts `x1 → x5 → x10 → x50 → x100 → x1000 → x1`, rückwärts genau umgekehrt.
    Die aktive Stufe ist grün hervorgehoben.

- [ ] **9.2 Rechts mehr, links weniger** · `WICHTIG`
  - Ablauf: Auf das Item in der Mitte rechts- und linksklicken
  - Erwartet: Rechtsklick erhöht um die Schrittweite, Linksklick senkt sie. Die −/+ Knöpfe
    nutzen dieselbe Schrittweite.

- [ ] **9.3 Preis bleibt in Grenzen** · `NORMAL`
  - Ablauf: Mit x1000 nach unten und lange nach oben klicken
  - Erwartet: Nicht unter 0, nicht über 1 000 000.

- [ ] **9.4 Menge bleibt in 1…64** · `NORMAL`
  - Ablauf: Im Mengen-Editor mit x64 in beide Richtungen klicken
  - Erwartet: Bleibt zwischen 1 und 64; das angezeigte Item verschwindet nie.

- [ ] **9.5 Bestätigen speichert dauerhaft** · `WICHTIG`
  - Ablauf: Preis und Menge ändern, bestätigen, Survival neu starten, Angebot wieder ansehen
  - Erwartet: Beide Werte sind noch da.

---

## 10 — Geldautomat

Neben der Kontostandsanzeige stecken hier zwei Fehler, die beim Bauen aufgefallen sind — die
drei Blocker unten sind die wichtigsten Tests dieser Runde.

- [ ] **10.1 Kontostand ist sichtbar** · `WICHTIG`
  - Ablauf: Schleichen + Rechtsklick auf eine Enderkiste
  - Erwartet: Goldbarren in der Mitte: `Kontostand: N Bits`, darunter Kontoname und die
    Entsprechung in Diamanten. Auch auf den Ein- und Auszahlen-Seiten.

- [ ] **10.2 Einzahlen aktualisiert sofort** · `WICHTIG`
  - Ablauf: Diamanten einzahlen und auf den Kontostand schauen, ohne das Menü zu schließen
  - Erwartet: Die Zahl springt direkt hoch.

- [ ] **10.3 Auszahlen am Spielerkonto** · `BLOCKER`
  - Ablauf: Am persönlichen ATM auf *Auszahlen* und einen Betrag klicken
  - Erwartet: Du bekommst Diamanten, der Kontostand sinkt.
  - Warum: Das war komplett kaputt — bei einem Nicht-Team-Konto kam sofort ein Abbruch zurück,
    der Knopf jedes persönlichen ATM tat gar nichts.

- [ ] **10.4 Volles Inventar frisst kein Geld** · `BLOCKER`
  - Ablauf: Inventar komplett vollmachen, dann auszahlen
  - Erwartet: `In deinem Inventar war nicht genug Platz - N Bits sind auf dem Konto geblieben.`
    Der Kontostand stimmt danach auf den Bit genau.
  - Warum: Vorher wurden die Items ohne Platzprüfung eingefügt — was nicht passte, war weg.

- [ ] **10.5 Team-Geld geht aufs Teamkonto zurück** · `BLOCKER`
  - Ablauf: 10.4 am Team-ATM wiederholen (`/cteam` → Team-ATM)
  - Erwartet: Der nicht zustellbare Betrag landet wieder auf dem **Team**konto, nicht auf
    deinem privaten.
  - Warum: Andersherum wäre es eine Möglichkeit, die Teamkasse in die eigene Tasche umzubuchen.

- [ ] **10.6 Beschriftung stimmt** · `NORMAL`
  - Ablauf: Die Auszahlen-Seite ansehen
  - Erwartet: Die Knöpfe heißen `N Bits auszahlen`. Vorher stand auf beiden Seiten „einzahlen".

---

## 11 — Speedway-Map

Wird als Asset mitgeliefert und einmalig ausgepackt. Alle Koordinaten sind gegen die Blöcke
der Welt geprüft — was fehlt, ist der Spieltest.

- [ ] **11.1 Map wird installiert** · `BLOCKER`
  - Ablauf: Frischen Bedwars-Server anlegen, dann `ls servers/BEDWARS/maps/`
  - Erwartet: `speedway/` und `speedway.yml`. Im Launcher-Log:
    ```
    Installing Bedwars Map: Speedway on BEDWARS
    ```

- [ ] **11.2 Versionsmarker liegt da** · `NORMAL`
  - Ablauf: `cat servers/BEDWARS/.assets/BEDWARS_SPEEDWAY`
  - Erwartet: `1`. Daran erkennt der Launcher, dass er nicht noch einmal auspacken muss.

- [ ] **11.3 Runde startet auf der Map** · `BLOCKER`
  - Ablauf: Auf dem Bedwars-Server `/bw status`, dann `/bw start`
  - Erwartet: Als Map steht `Speedway` da und die Runde beginnt.

- [ ] **11.4 Alle acht Basen sind vollständig** · `BLOCKER`
  - Ablauf: Jede Basis ablaufen. Sie liegen bei `(±71, 66, ±32)` und `(±32, 66, ±71)`.
  - Erwartet: Pro Basis: ein Bett, ein Spawn auf der Eisenplattform, ein Eisen-/Gold-Generator
    davor und zwei Villager (Shop und Upgrades) in den Nischen hinten.
  - Farbzuordnung aus der Wolle der Basis:

    | Bett bei | Team |
    |---|---|
    | `(-71, 66, -32)` | Grau |
    | `(-71, 66, 32)` | Pink |
    | `(-32, 66, -71)` | Rot |
    | `(-32, 66, 71)` | Weiß |
    | `(32, 66, -71)` | Blau |
    | `(32, 66, 71)` | Aqua |
    | `(71, 66, -32)` | Grün |
    | `(71, 66, 32)` | Gelb |

- [ ] **11.5 Generatoren in der Mitte** · `WICHTIG`
  - Ablauf: Zu `(±32, 65, ±32)` und `(-11.5, 66, 15.5)` bzw. `(12.5, 66, -14.5)` laufen
  - Erwartet: Vier Diamant- und zwei Emerald-Generatoren, jeweils auf ihrem Markierungsblock.

- [ ] **11.6 Bau- und Todesgrenze** · `NORMAL`
  - Ablauf: Über y 92 bauen versuchen, dann von der Insel springen
  - Erwartet: Bauen wird oberhalb 92 verweigert (die Warte-Lobby schwebt bei y 95–104),
    unter y 40 stirbt man.

- [ ] **11.7 Bett zerstören und respawnen** · `BLOCKER`
  - Ablauf: Mit zwei Accounts in zwei Teams: erst mit Bett sterben, dann das Bett zerstören
    und wieder sterben
  - Erwartet: Mit Bett Respawn in der Basis, ohne Bett Ausscheiden. Beide Betthälften
    verschwinden.

- [ ] **11.8 Eigene Änderungen überleben** · `WICHTIG`
  - Ablauf: `/bw setup`, etwas verschieben, speichern, Server neu starten
  - Erwartet: Deine Änderung ist noch da. Assets werden nur bei einer neuen Version wieder
    ausgepackt, nicht bei jedem Start.

- [ ] **11.9 Acht Teams zu zweit** · `WICHTIG`
  - Ablauf: In `servers/BEDWARS/plugins/Bedwars/game.yml` `mode: doubles` setzen, Server neu
    starten
  - Erwartet: `/bw status` meldet 8 Teams à 2 Spieler. Standard ist sonst `quad` mit vier Teams.

- [ ] **11.10 Wie lange der Rundenstart dauert** · `WICHTIG`
  - Ablauf: Beim ersten `/bw start` auf die Uhr sehen und die Serverkonsole beobachten
  - Erwartet: Die Welt ist von Minecraft 1.16.5 und wird beim Laden konvertiert. Geprüft ist,
    dass Paper 26.2 sie fehlerfrei lädt — **nicht**, wie lange die Arena-Kopie im laufenden
    Betrieb braucht.
  - Warum: Falls dabei eine 30-Sekunden-Warnung „World storage migration" auftaucht, sag
    Bescheid — dann wird die Map einmalig vor dem Ausliefern konvertiert.

---

## 12 — Befehle und Altbestand

Zum Schluss die Dinge, die vorher schon funktioniert haben und es geblieben sein müssen.

- [ ] **12.1 Jeder Befehl hat eine Beschreibung** · `NORMAL`
  - Ablauf: `/help` und die Plugin-Seiten durchblättern
  - Erwartet: Auch `/debug`, `/admin`, `/cteam`, `/rs`, `/shopkeeper`, `/legitimize` und
    `/banane` haben Text und Benutzung.

- [ ] **12.2 Survival funktioniert wie vorher** · `REGRESSION`
  - Ablauf: Team anlegen, Chunk claimen, Geld verdienen, Shop erstellen und kaufen
  - Erwartet: Alles wie gehabt.

- [ ] **12.3 Rucksack** · `REGRESSION`
  - Ablauf: `/backpack` auf Survival, Item ablegen, mit zweitem Teammitglied prüfen
  - Erwartet: Geteilter Inhalt, keine Duplikate.

- [ ] **12.4 Lobby** · `REGRESSION`
  - Ablauf: Parkour laufen, `/events` öffnen
  - Erwartet: Checkpoints und Kalender unverändert.

- [ ] **12.5 Adminseite** · `REGRESSION`
  - Ablauf: Website öffnen, Spielerliste und Serverkonsole ansehen
  - Erwartet: Spieler werden gefunden, die Konsole zeigt neue Zeilen — Run- und Bedwars-Server
    melden jetzt zusätzlich ihre Spieler mit.

---

## 13 — Die Speedway-Map, korrigiert

Die Punkte der Map wurden neu aus den Blöcken der Welt gelesen. Jeder einzelne liegt jetzt auf
dem Block, der ihn markiert — 16 von 16 Keeper-Plätzen, 8 von 8 Generator-Podesten.

- [ ] **13.1 Acht Teams statt vier** · `BLOCKER`
  - Ablauf: Bedwars-Server starten, Konsole lesen, dann `/bw status`
  - Erwartet: `Hosting doubles (8x2) on speedway`. Im Warteraum stehen acht Wollblöcke.

- [ ] **13.2 Der Spawner steht hinten auf den Stone Brick Slabs** · `BLOCKER`
  - Ablauf: Runde starten, in die eigene Basis, nach hinten laufen (weg von der Mitte)
  - Erwartet: Eisen und Gold fallen in der Nische auf die sechs Bruchsteinziegel-Stufen,
    nicht mehr vorne auf dem freien Boden. Für GRAU ist das `-86 / 66 / -31.5`.

- [ ] **13.3 Beide Shopkeeper stehen in ihrer Nische auf der Sea Lantern** · `BLOCKER`
  - Ablauf: Runde mit acht Spielern (oder `/bw start` und durchlaufen), jede Basis ansehen
  - Erwartet: In jeder Basis stehen zwei Villager, jeder in einer der beiden beleuchteten
    Nischen — zwei Sea Lanterns als Boden, ein Barrier als Dach. Für Grau `-82 / 66 / -37.5`
    (Items) und `-82 / 66 / -25.5` (Upgrades). Keiner steht auf dem Podest davor, keiner
    schwebt, keiner steckt in einer Wand.
  - Konsole sagt beim Start: `16 shop keepers are standing in 8 bases.`

- [ ] **13.4 Vier Smaragd-Generatoren** · `WICHTIG`
  - Ablauf: `/bw generators`
  - Erwartet: Vier Diamant- und vier Smaragd-Generatoren. Zwei Smaragde liegen oben
    (`-11.5/66/15.5` und `12.5/66/-14.5`), zwei auf der unteren Ebene
    (`-2.5/56/8.5` und `3.5/56/-7.5`).

- [ ] **13.5 Die Startplattform verschwindet** · `BLOCKER`
  - Ablauf: Runde starten und nach oben schauen
  - Erwartet: Das Holzhaus über der Mitte ist weg. Konsole:
    `The waiting platform is gone (877 blocks).`

- [ ] **13.6 In fremden Basen darf gebaut werden** · `WICHTIG`
  - Ablauf: Als Rot in die blaue Basis laufen, dort Wolle setzen — erst neben dem Bett,
    dann direkt auf dem gegnerischen Spawn
  - Erwartet: Neben dem Bett geht es. Nur im Umkreis von vier Blöcken um den Spawn kommt
    `build.protected`. Auf dem eigenen Spawn darf man selbst bauen.

- [ ] **13.7 Generatoren lassen sich nicht zumauern** · `NORMAL`
  - Ablauf: Direkt auf ein Generator-Podest bauen, eigenes wie fremdes
  - Erwartet: Geht bei keinem.

- [ ] **13.8 Es bleibt Tag** · `NORMAL`
  - Ablauf: Zehn Minuten spielen
  - Erwartet: Die Sonne steht still. In `maps/speedway.yml` steht `time.daylight-cycle: false`.

---

## 14 — Kampf, Tod und Werkzeuge

- [ ] **14.1 Kein Respawn-Knopf mehr** · `BLOCKER`
  - Ablauf: Sich in die Leere fallen lassen oder totschlagen lassen
  - Erwartet: Kein Todesbildschirm, kein Knopf. Man ist sofort Zuschauer über der Mitte und
    sieht den Countdown.

- [ ] **14.2 Der Countdown zählt** · `WICHTIG`
  - Ablauf: Sterben und auf den Titel schauen
  - Erwartet: `Back in 5s`, `4s`, `3s` … — nicht mehr das Wort `seconds`.

- [ ] **14.3 Die Leere tötet oben** · `WICHTIG`
  - Ablauf: Von einer Brücke fallen
  - Erwartet: Der Tod kommt bei y 40, nicht erst hundert Blöcke tiefer.

- [ ] **14.4 Werkzeuge werden aufgewertet, nicht ausgesucht** · `BLOCKER`
  - Ablauf: Shop → Werkzeuge
  - Erwartet: Drei Knöpfe: Schere, Spitzhacke, Axt. Die Spitzhacke zeigt Holz; nach dem Kauf
    zeigt derselbe Knopf Eisen, dann Gold, dann Diamant. Nie vier Spitzhacken nebeneinander.

- [ ] **14.5 Dasselbe für Schwert und Rüstung** · `WICHTIG`
  - Ablauf: Shop → Nahkampf und Rüstung
  - Erwartet: Je ein Knopf, der die nächste Stufe zeigt. Ganz oben steht `Fully upgraded`.

- [ ] **14.6 Ein Tod kostet eine Stufe, Holz bleibt** · `BLOCKER`
  - Ablauf: Diamantspitzhacke kaufen, sterben, Inventar ansehen; dann noch dreimal sterben
  - Erwartet: Nach dem ersten Tod Gold, dann Eisen, dann Holz — und Holz bleibt für immer.
    Beim Schwert dasselbe: unten steht das Holzschwert, das nie verloren geht.

- [ ] **14.7 Fireballs fliegen** · `BLOCKER`
  - Ablauf: Fireball kaufen, Rechtsklick in die Luft und Rechtsklick auf einen Block
  - Erwartet: Er verlässt sofort und schnell die Hand, fliegt geradeaus, explodiert beim
    Aufprall mit Zischen. Er zündet nichts an und reißt nur Blöcke weg, die jemand in dieser
    Runde gesetzt hat — die Karte selbst bleibt heil.

- [ ] **14.8 Der Dream Defender kämpft** · `BLOCKER`
  - Ablauf: Eisengolem kaufen und auf den Boden der eigenen Basis setzen, dann einen Gegner
    hereinlassen
  - Erwartet: Der Golem greift den Gegner an und bleibt an ihm dran. Teammitglieder rührt er
    nicht an. Nach vier Minuten verschwindet er.

- [ ] **14.9 Der Golem erstickt nicht** · `WICHTIG`
  - Ablauf: Das Ei gegen eine Wand klicken statt auf den Boden, und einmal unter einen
    einen Block hohen Überhang
  - Erwartet: Er erscheint vor der Wand, nicht in ihr. Wo kein Platz ist, kommt
    `There is no room for that here.` und das Ei bleibt im Inventar.

- [ ] **14.10 1.8-PvP lässt sich einschalten** · `WICHTIG`
  - Ablauf: `/bw admin`, `1.8 PvP` anklicken, dann zuschlagen
  - Erwartet: Kein Ladebalken am Schwert, kein Sweep-Effekt. Ohne den Schalter wie vorher.

---

## 15 — Admin-Inventar und Locator Bar

- [ ] **15.1 Die Locator Bar ist aus** · `BLOCKER`
  - Ablauf: Mit zwei Spielern auf den Bedwars-Server, über der Hotbar schauen
  - Erwartet: Keine Leiste, die verrät, wo die anderen stehen.

- [ ] **15.2 `/bw admin` öffnet die Schalter** · `WICHTIG`
  - Ablauf: `/bw admin`
  - Erwartet: Sieben Schalter — 1.8 PvP, Locator Bar, Tracker-Kompass, Tag und Nacht,
    Rohstoffe an den Killer, Hunger, Shop. Klicken schaltet um, das Menü bleibt offen und
    zeigt sofort den neuen Stand.

- [ ] **15.3 Der Stand überlebt den Neustart** · `NORMAL`
  - Ablauf: Etwas umschalten, `configs/bedwars/features.yml` ansehen, Server neu starten
  - Erwartet: Die Datei hat den Wert, und nach dem Neustart steht der Schalter noch so.

- [ ] **15.4 Die Einstellungen sind in der Rundenlobby erreichbar** · `WICHTIG`
  - Ablauf: Als OP auf den Bedwars-Server, im Warteraum in den letzten Hotbar-Platz greifen
  - Erwartet: Ein Comparator "Settings". Rechtsklick öffnet dasselbe Menü wie `/bw admin`.
    Wer kein OP ist und kein `bedwars.admin` hat, bekommt das Item gar nicht erst.

- [ ] **15.5 Auto Start lässt sich abschalten** · `WICHTIG`
  - Ablauf: Im Menü `Auto Start` ausschalten, dann mit genug Leuten im Warteraum stehen
  - Erwartet: Kein Countdown. Einmalig die Zeile, dass der automatische Start aus ist. Die
    Runde beginnt erst auf `/bw start`. Wieder einschalten lässt den Countdown normal laufen.

- [ ] **15.6 Der Kompass ersetzt die Leiste** · `NORMAL`
  - Ablauf: Im Shop unter Sonstiges den `Tracker Compass` kaufen und in die Hand nehmen
  - Erwartet: Die Nadel zeigt auf den nächsten Gegner. In der Tasche tut sie nichts.

---

## 16 — Was vorher kaputt war

- [ ] **16.1 Keine `MemorySection`-Nachrichten mehr** · `BLOCKER`
  - Ablauf: Jemanden töten, ein Bett zerstören, eine Falle auslösen, eine Runde zu Ende
    spielen
  - Erwartet: Überall echte Sätze. Nirgends
    `MemorySection[path='trap.set-off', root='YamlConfiguration']`.
  - Hinweis: Betraf elf Texte, darunter jede Todesmeldung. Eine alte `messages.yml` wird
    beim Start selbst repariert — vorhandene Blöcke werden verworfen und neu geschrieben.

- [ ] **16.2 Sauberes Herunterfahren** · `WICHTIG`
  - Ablauf: Einen Run- oder Bedwars-Server stoppen und die letzten Zeilen lesen
  - Erwartet: Kein `IllegalStateException: zip file closed` und kein
    `The plugin classloader for RunPlugin has thrown a zip file error` mehr.

- [ ] **16.3 Das Inventar springt nicht mehr** · `BLOCKER`
  - Ablauf: Shop öffnen, mehrfach hintereinander kaufen und auf Reiter klicken
  - Erwartet: Kein Blinken, kein Zurückspringen auf die erste Seite, kein Zurücksetzen unter
    der Hand. Preise und `Fully upgraded` stimmen sofort.

- [ ] **16.4 In der Lobby tut nichts weh** · `BLOCKER`
  - Ablauf: Im Hub vom Parkour fallen, in Lava/Feuer laufen falls vorhanden, warten bis der
    Hunger sinken müsste
  - Erwartet: Keine Herzen weg, keine Nahrungsanzeige, die leerläuft. Wer unter die Welt
    fällt, steht wieder am Spawn statt endlos zu fallen. Im Kreativmodus bleibt alles wie
    vorher — das ist der Bauzustand.

---

## 17 — Bedwars als Event

- [ ] **17.1 Event anlegen** · `WICHTIG`
  - Ablauf: In der Lobby `/events`, neues Event, Typ auf `Bedwars` klicken
  - Erwartet: Ein zusätzlicher Knopf `Teamgröße` erscheint (Bett-Symbol) und zählt 1 bis 8
    durch. Darunter steht der Modus, den das ergibt.

- [ ] **17.2 Das Event startet die Runde** · `BLOCKER`
  - Ablauf: Start auf `sofort` stellen, anlegen und in der Lobby stehen bleiben
  - Erwartet: Binnen 15 Sekunden eine Ansage, ein Bedwars-Server wird gestartet, alle in der
    Lobby werden mitgenommen, sobald er Spieler annimmt.

- [ ] **17.3 Die Teamgröße kommt an** · `WICHTIG`
  - Ablauf: Auf dem gestarteten Server `/bw status`
  - Erwartet: Der Modus passt zur eingestellten Teamgröße (1 → solo, 2 → doubles, 3 → trio,
    ab 4 → quad). Im Log steht `This round belongs to the event '<name>'`.

- [ ] **17.4 Die Bedwars-Lobby ist vor dem Event offen** · `WICHTIG`
  - Ablauf: Event mit Start in ein paar Minuten anlegen und warten, bis fünf Minuten vorher
    erreicht sind
  - Erwartet: In der Lobby erscheint "… startet in Kürze - die Bedwars-Lobby ist offen".
    Unter `/events` → Event steht ein Knopf "Zur Bedwars-Lobby". Wer ihn drückt, landet im
    Warteraum der Runde.

- [ ] **17.5 Die Runde wartet auf die Eventzeit** · `BLOCKER`
  - Ablauf: Früh in die Bedwars-Lobby gehen, dort mit genug Leuten stehen bleiben
  - Erwartet: Kein Countdown, stattdessen "Waiting for <Event>, which begins in …". Bei 60,
    30, 20, 15, 10, 5 … Sekunden wird heruntergezählt. Zur Eventzeit läuft der normale
    Lobby-Countdown los, und die Übriggebliebenen aus dem Hub werden nachgeholt.

- [ ] **17.6 Niemand wird zweimal geschickt** · `NORMAL`
  - Ablauf: Nach dem Warp zurück in die Lobby, dort eine Minute stehen bleiben
  - Erwartet: Man wird nicht alle 15 Sekunden erneut hinübergezogen.

---

## 18 — Erklärungen

Jeder Eintrag im Shop, jedes Upgrade, jede Falle und jeder Schalter sagt jetzt, was er tut.

- [ ] **18.1 Shop-Items erklären sich** · `WICHTIG`
  - Ablauf: Shop öffnen und über jeden Eintrag fahren
  - Erwartet: Über dem Preis stehen zwei bis fünf Zeilen, die sagen wofür das Item da ist.
    Dinge, die man nicht sieht, stehen ausdrücklich drin: Glas ist explosionsfest, End Stone
    fireball-fest, Rüstung und Schere bleiben über den Tod, Schwerter nicht.

- [ ] **18.2 Upgrades und Fallen erklären sich** · `WICHTIG`
  - Ablauf: Upgrade-Keeper anklicken
  - Erwartet: Jedes Upgrade sagt, was es dem Team bringt. Jede Falle sagt, wann sie auslöst
    und was sie beim Gegner macht — und dass Magic Milk dagegen hilft.

- [ ] **18.3 Die Einstellungen erklären sich** · `WICHTIG`
  - Ablauf: `/bw admin` bzw. das Comparator-Item, über jeden Schalter fahren
  - Erwartet: Ein Absatz was der Schalter ist, eine Leerzeile, dann was **an** und was **aus**
    jeweils bedeutet. Unten "Now: On/Off" und "Click to switch it off/on" — nicht mehr nur
    "Click to switch".
  - Dieselben Texte stehen als Kommentare in `configs/bedwars/features.yml`.

- [ ] **18.4 Was heißt „Diamond II in 3:20"?** · `WICHTIG`
  - Ablauf: `/bw timeline`, mit der Maus über eine Zeile fahren
  - Erwartet: Ein Tooltip mit Name, Zeitpunkt und einem Satz, was dabei passiert. Unter der
    Liste steht der Hinweis, dass man hovern kann.

- [ ] **18.5 Ereignisse erklären sich beim Auslösen** · `NORMAL`
  - Ablauf: `/bw timeline skip` durchlaufen lassen
  - Erwartet: Nach jeder Ankündigung eine graue Zeile, was das Ereignis bedeutet. Bei Bed
    Destruction also, dass ab jetzt niemand mehr respawnt.

- [ ] **18.6 Die Texte sind änderbar** · `NORMAL`
  - Ablauf: In `configs/bedwars/shop.yml` bei einem Item die `lore` ändern, `/bw reload`
  - Erwartet: Der neue Text steht im Shop. Nichts davon ist im Code festgenagelt.

---

## 19 — Der Parkour in der Lobby

Neu und noch nie gelaufen. Es gibt beim ersten Start keine Strecke — die muss gebaut werden.

- [ ] **19.1 Eine Strecke bauen** · `BLOCKER`
  - Ablauf: Als OP in der Lobby hinstellen und der Reihe nach
    `/parkour setup test start`, ein Stück laufen, `/parkour setup test checkpoint`,
    noch ein Stück, `/parkour setup test finish`
  - Erwartet: Jede Zeile wird bestätigt. Danach liegt `plugins/LobbyPlugin/parkour.yml` mit
    Start, Checkpoint und Ziel, jeweils mit den Koordinaten, an denen Du standest.

- [ ] **19.2 `/parkour` zeigt die Strecken** · `WICHTIG`
  - Ablauf: `/parkour`
  - Erwartet: Die Strecke mit Anzahl Checkpoints, anklickbar zum Starten.

- [ ] **19.3 Der Lauf startet von selbst** · `WICHTIG`
  - Ablauf: Einfach auf den Startpunkt laufen, ohne Befehl
  - Erwartet: Titel mit dem Streckennamen und "Los!", darunter die eigene Bestzeit.

- [ ] **19.4 Checkpoints und Reihenfolge** · `BLOCKER`
  - Ablauf: Über den Checkpoint laufen, dann versuchen, direkt ins Ziel zu springen und dabei
    den zweiten Checkpoint auszulassen
  - Erwartet: Der Checkpoint gibt einen Ton und eine Actionbar mit Zwischenzeit. Das Ziel
    zählt erst, wenn alle Checkpoints hinter Dir liegen — Abkürzen geht nicht.

- [ ] **19.5 Runterfallen kostet nur Zeit** · `WICHTIG`
  - Ablauf: Mitten im Lauf ins Nichts springen
  - Erwartet: Zurück am letzten Checkpoint (bzw. am Start), kein Schaden, die Uhr läuft weiter.

- [ ] **19.6 Bestzeit und Bestenliste** · `WICHTIG`
  - Ablauf: Zweimal ins Ziel, das zweite Mal langsamer; danach `/parkour top test`
  - Erwartet: Beim ersten Mal "Neue Bestzeit!" und eine Ansage an alle. Beim zweiten Mal
    bleibt die alte Zeit stehen und es gibt keine Ansage. Die Bestenliste zeigt die schnellere.

- [ ] **19.7 Die Strecke ist sichtbar** · `WICHTIG`
  - Ablauf: Nach dem Bauen in die Lobby schauen
  - Erwartet: Über dem Start schwebt Text mit Streckenname, Anzahl Checkpoints und dem
    aktuellen Rekord. Eine unfertige Strecke sagt das dort auch.

- [ ] **19.8 Bestenliste als Hologramm** · `NORMAL`
  - Ablauf: `/parkour setup test board`, dann eine Zeit laufen
  - Erwartet: An der Stelle hängt eine Liste mit den fünf besten Zeiten. Nach einer neuen
    Bestzeit steht sie sofort drin, ohne Neustart.

- [ ] **19.9 Kein Text bleibt liegen** · `NORMAL`
  - Ablauf: Lobby-Plugin neu laden bzw. Server neu starten
  - Erwartet: Keine doppelten Hologramme. Text-Displays sind nicht persistent und werden beim
    Abschalten zusätzlich aktiv entfernt.

- [ ] **19.10 Der Lauf endet beim Verlassen** · `NORMAL`
  - Ablauf: Mitten im Lauf `/warp survival`, dann zurück in die Lobby
  - Erwartet: Kein laufender Lauf mehr, keine absurde Zeit. `/parkour leave` bricht ebenfalls
    ab und setzt zurück an den Spawn.

---

## Wenn etwas schiefgeht

Server, Befehl und die passende Zeile aus `servers/<name>/logs/latest.log` mitschicken —
damit lässt sich der Fehler am schnellsten finden.

Dieselbe Liste zum Abhaken im Browser:
<https://claude.ai/code/artifact/68c35d29-b071-48a2-9d95-b0b61a0a13bf>
