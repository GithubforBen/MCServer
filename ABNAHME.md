# Netzwerk-Abnahme

Testplan für die Änderungen aus drei Runden: Serverstart und Warp, Shop und ATM,
die mitgelieferte Bedwars-Map und die Speichergrenzen.

Stand: 2026-08-27 · 65 Prüfungen in zwölf Phasen

Die Phasen bauen aufeinander auf — ohne laufenden Launcher lässt sich über RunPlugin nichts
sagen, und ohne aktives RunPlugin nichts über Warp und Fortschritt. Also von oben nach unten.

**Schweregrade**

| Marker | Bedeutung |
|---|---|
| `BLOCKER` | Geht das nicht, ist der Rest der Runde hinfällig |
| `WICHTIG` | Kern der Änderung |
| `NORMAL` | Detail am Rand |
| `REGRESSION` | War vorher da und muss es bleiben |

**Die 17 Blocker vorweg:** 1.1 · 2.1 · 2.2 · 3.1 · 3.3 · 4.1 · 6.1 · 6.2 · 7.2 · 8.4 · 10.3 · 10.4 · 10.5 · 11.1 · 11.3 · 11.4 · 11.7

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

## Wenn etwas schiefgeht

Server, Befehl und die passende Zeile aus `servers/<name>/logs/latest.log` mitschicken —
damit lässt sich der Fehler am schnellsten finden.

Dieselbe Liste zum Abhaken im Browser:
<https://claude.ai/code/artifact/68c35d29-b071-48a2-9d95-b0b61a0a13bf>
