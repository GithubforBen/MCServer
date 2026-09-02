# TODO

Stand: 2026-09-02. Offene Punkte aus der Shopkeeper-/Marktplatz-Runde, dem Eventsystem und der
Runde um eigene Runden, Speicher, Geld und Cosmetics.

---

## 1. Eventsystem

Grundgerüst steht. Die UHC-Events setzen darauf auf.

### 1.1 Kalender und Anzeige — erledigt
- [x] Eventkalender als Inventar (`/events`, in Lobby und Survival)
- [x] Gleiche Daten auf der Admin-Website, dort als Zeitbalken statt als Inventar
- [x] Admins legen im Kalender-Inventar unten neue Events an
- [x] Events auch über die Website anlegbar, absagbar, löschbar
- [x] Beim Joinen: Chat-Hinweis mit anklickbarem "[Events ansehen]"
- [x] Tabliste: "Nächstes Event: X in Y" bzw. "läuft — noch Y"

Aufbau wie bei den Teams: der Launcher besitzt die Events (`EventStore`, `events.yml`),
alle Server halten eine Kopie (`EventService`) und werden bei jeder Änderung benachrichtigt.

Offen dabei:
- [ ] Uhrzeit im Inventar nur über Presets wählbar (Start/Dauer). Feineres Datum geht
      bisher nur über die Website — reicht das so?

### 1.2 End-Event — erledigt
- [x] `/end-allow` und `AntiEndCommand` entfernt
- [x] `AntiEndHandler.allowEnd()` fragt jetzt `EventService.hasHappened(END)`
- [x] Sobald das End-Event startet, bleibt das End dauerhaft offen
- [x] Nur ein END-Event möglich (`EventType.isOnlyOnce`)
- [x] Alte `allow-end`-Config wird weiter als Override gelesen, damit ein Netzwerk,
      das das End schon offen hatte, es beim Update nicht wieder zumacht

### 1.3 UHC-Speedrun-Events — Gerüst steht
Gemeinsame Mechanik:
- [x] Teamgröße vorab wählbar (1/2/3/4) über die Regeln-Oberfläche
- [x] Queue: draufklicken, Freunde joinen dazu, Start bei voller Queue automatisch
- [x] Start unter Sollstärke erlaubt (togglebar), Lauf wird als unterbesetzt markiert
- [x] Timer läuft, niedrigste Zeit gewinnt (Bestenliste im Event-Panel)
- [x] Mehrere Versuche pro Person, Limit einstellbar (1/3/5/10/unbegrenzt)
- [x] Einstellungen togglebar: Hardcore, Teamgröße, Versuche, Unterbesetzt

Die zwei Varianten:
- [x] **Bosse**: Elder Guardian, Wither, Warden und Enderdrache
- [x] **Speedrun**: nur der Enderdrache

Zeitmodell (nachgezogen):
- [x] Zeit wird in **Ticks** gezählt, nicht nach Wanduhr
- [x] Gehen alle Teilnehmer raus, pausiert die Zeit; sie läuft weiter, sobald einer zurück ist
- [x] Der Run-Server schaltet sich nach 10 Minuten ohne Teilnehmer selbst ab
- [x] Welt und Fortschritt bleiben liegen, dasselbe Team kann den Lauf fortsetzen
      ("Weiterspielen" im Event-Panel startet den Server unter gleichem Namen neu)
- [x] Timer über der Hotbar (Actionbar), inklusive Zielfortschritt
- [x] Ein Launcher-Neustart lässt die Uhr nicht weiterlaufen — Läufe kommen pausiert zurück

Preise und Abschluss (erledigt):
- [x] Plätze 1-3 und Teilnahmepreis, je Geld plus Items
- [x] Preise werden am Event gespeichert (`prize.place.N`, `prize.participation`)
- [x] Konfiguration im Spiel: Geld durchklicken, Item aus der Hand hinzufügen
- [x] Beim Eventende: Preise vergeben → Run-Server stoppen → **alle Läufe löschen**
- [x] Läuft automatisch, einmal pro Minute geprüft (`EventSettlement`)
- [x] Ein abgesagtes Event zahlt niemanden aus, wird aber genauso aufgeräumt
- [x] Wird ein Event gelöscht, verschwinden seine Läufe mit
- [x] Preise warten in `awards.yml`, bis der Spieler joint — ein Event endet fast nie,
      während die Gewinner online sind
- [x] Kein Teilausliefern: passt der Preis nicht ins Inventar, bleibt er ganz liegen
- [x] Preise mit Geld warten auf einen Server mit Wirtschaft (also Survival),
      statt in der Lobby zu verfallen

Lebenszyklus (erledigt):
- [x] Nach einem Lauf werden alle nach 15 Sekunden zurück in die Lobby geschickt
- [x] Bei der Abwicklung werden die Verzeichnisse der Run-Server gelöscht und ihre
      Port-Reservierung freigegeben (30 s Karenz, damit der Prozess die Dateien loslässt)

Offen:
- [ ] Ein Lauf, den niemand je fortsetzt, bleibt bis zum Eventende PAUSED — erst dann
      räumt ihn die Abwicklung weg
- [ ] Ein abgesagtes Event wird sofort abgewickelt. Klickt man danach "Wieder
      aktivieren", ist es zwar wieder aktiv, aber seine Läufe sind weg
- [ ] Spezialitems als Preise (pluginspezifisch) — bisher nur normale Materialien

### 1.4 Event-Server — erledigt
- [x] Pro Lauf eine eigene Instanz über `ServerApi.createEventServer`
- [x] Neues Modul `RunPlugin` mit `/reset` (Welten löschen und neu generieren,
      Bestätigung durch zweimaliges Eingeben)
- [x] `RunPlugin` wertet Bosskills aus, erzwingt Hardcore und zeigt den Timer
      in der Actionbar
- [x] Als `FileType.PLUGIN.RUN` registriert und im `EVENT`-Template installiert

### 1.5 Lobby
- [x] Eventsystem in der Lobby verfügbar (`/events`, Kalender, Join-Hinweis)
- [x] Events lassen sich direkt aus der Lobby heraus anlegen
- [ ] Eigene Lobby-Minispiel-Events mit Teilnehmerzahl und Preisen über einen
      kurzen Zeitraum (ca. 1 Stunde) — die Event-Typen dafür gibt es noch nicht

### 1.6 Lobby-Welt — erledigt
- [x] Genau eine Lobbywelt, beim Start aus einem Template-Verzeichnis geladen
- [x] Platzhalter: liegt kein Template da, wird die vorhandene Welt benutzt —
      ohne dass etwas konfiguriert werden muss
- [x] Spieler landen beim Joinen immer am Spawn dieser Welt
- [ ] **Deine Aufgabe**: die richtige Karte nach `./lobby-world` legen (Ordner mit
      `level.dat` usw.). Sie wird dann bei jedem Start frisch hergestellt.
      Einstellbar über `lobby.world`, `lobby.source`, `lobby.restore-on-start`

---

## 2. Am lebenden Server nachprüfen

Alles gebaut und logisch geprüft, aber nicht auf einem laufenden Server verifiziert.

- [ ] Bleibt nach mehreren Neustarts **genau ein** Villager pro Shop übrig?
      (`ShopkeeperChunkListener`, Spawn über `EntitiesLoadEvent`)
- [ ] Feuert `ChunkUnloadEvent` in Paper 26.2 früh genug, dass `getState()` noch die
      echten Kisteninhalte liefert? (`Shopkeeper.refreshStock()`)
- [ ] Kauf-Transaktion unter echter Last: Rollback bei vollem Inventar, Erstattung,
      Ware landet nicht doppelt (`Shopkeeper.buyItem()`)
- [ ] Marktplatz-Oberfläche: Reiter, Sortierung, Toggles, Rechtsklick-Anbieterliste
- [ ] BUILDING/MISC-Aufteilung — hängt an `Material.isBlock()` und ist deshalb nur auf
      dem Server testbar (die expliziten Regeln sind abgedeckt)

---

## 3. Offene Entscheidungen

- [ ] `MoneyHandler.addMoney()` an den Verkäufer kann fehlschlagen. Dann ist das Geld weg:
      Käufer hat bezahlt und die Ware, Shopbesitzer bekommt nichts. Bewusst kein Rollback.
      Soll das abgesichert werden?
- [ ] `/shop debug` als Diagnose: listet Shops ohne Villager und verwaiste Villager mit
      Shop-ID. Nur bauen, falls die Villager-Prüfung oben Probleme zeigt.

---

## 4. Kleinkram im Code

- [ ] `Inventorys.java:217` — `//TODO: add option` im Item-Manager-Inventar
- [ ] `Main.java:54` — Ops automatisch hinzufügen
- [ ] `VelocityConfigurator.java:25` — Workaround ersetzen
- [ ] `VerifyAccount.java:5` — Account-Verknüpfung fehlt komplett
- [ ] `LobbyPlugin.java:31` — Parkour
- [ ] `.idea/misc.xml` stand auf `openjdk-23`; falls IntelliJ wieder zickt, auf 25 prüfen

---

## 5. Eigene Runden, Speicher, Geld und Cosmetics

Stand: 2026-09-02. Alles gebaut und kompiliert, nichts davon auf einem laufenden Server geprüft.

### 5.1 Geld gehört jetzt dem Launcher — erledigt
- [x] `money.yml` beim Launcher, `MoneyStore` als einziger Schreiber
- [x] Änderungen sind Differenzen unter einem Lock, jede angewandte wird ans Netz gemeldet
- [x] `configs/money-config.yml` von Survival wird beim ersten Start einmal übernommen
- [x] `MoneyHandler` behält seine Signaturen, antwortet aus der lokalen Kopie
- [x] `MoneyService.changeBlocking` als strenger Weg, wo etwas Wertvolles herausgeht

Offen:
- [ ] `MoneyHandler.removeMoney` ist eine Schätzung, wenn dasselbe Konto auf zwei Servern in
      derselben Sekunde leergeräumt wird. Der Launcher lehnt die zweite Änderung ab und korrigiert
      die Kopie, aber der zweite Server hat da schon „ja" gesagt. Für Shop-Käufe eines Spielers auf
      einem Server ist das dicht; falls das mal weh tut, müssen die Aufrufer auf `changeBlocking`
      umgestellt werden
- [ ] `AwardService` zahlt Geldpreise weiterhin nur auf Survival aus. Das war nötig, solange nur
      Survival Geld kannte — jetzt könnte jeder Server das

### 5.2 Speicherbudget und Empfehlung — erledigt
- [x] Der Launcher kennt die Größe der Maschine und hält eine Reserve frei
- [x] Starts, die nicht mehr ins Budget passen, werden abgelehnt und gezählt
- [x] Ein bewilligter Start hält seinen Speicher, bis sein Server wirklich läuft
- [x] Alle 30 Sekunden wird gemessen, was jeder Server hält (RSS aus `/proc`), Spitze bleibt stehen
- [x] Panel im Server Manager: Budget, abgelehnte Starts, Vorschläge mit Zahlen dahinter
- [x] Umsetzbar im Spiel: RAM pro Server setzen, gilt beim nächsten Start dieses Servers

Offen:
- [ ] Gemessen wird nur unter Linux. Auf Windows zeigt das Panel das Budget, aber keine Vorschläge
- [ ] `ServerHandler.startNewInstance` lehnt selbst nichts ab. Das Budget greift über die
      Slot-Anfrage, die die Lobby stellt — ein Admin, der im Server Manager einen Server erstellt,
      kann das Budget bewusst überziehen. Absicht; falls das doch verhindert werden soll, gehört
      die Prüfung zusätzlich in `startNewInstance`
- [ ] Die Empfehlung schlägt Werte vor, führt sie aber nicht aus. Ein „Übernehmen"-Knopf direkt am
      Vorschlag wäre der nächste Schritt

### 5.3 Selbst gestartete Runden — erledigt
- [x] `/runde` in der Lobby: laufende Runden sehen, beitreten, eigene aufmachen
- [x] Map, Modus, Addons und öffentlich/privat vor dem Start wählbar
- [x] Vom Admin freischaltbar, dazu Limits pro Spieler, insgesamt, Wartezeit und Event-Sperren
- [x] Rundenadmin ist, wer startet: Wartelobby steuern, kicken, privat schalten
- [x] Der Rundenserver liest seine Runde beim Start über den eigenen Namen, wie die Events auch
- [x] Der Launcher räumt Runden auf, deren Server weg ist

Offen:
- [ ] Der Rundenadmin kann die Runde nicht selbst beenden. Bewusst so — der Idle-Watchdog macht den
      Server zu, sobald der letzte raus ist
- [ ] Die Map-Liste kommt aus den Assets der Vorlage (`FileType.ASSET.getBedwarsMap`). Eine Welt,
      die ein Admin von Hand nach `maps/` legt, taucht in der Lobby nicht auf
- [ ] Rausgeworfene Spieler bleiben nur für die Lebensdauer des Rundenservers draußen. Da der
      Server mit der Runde endet, reicht das — es ist trotzdem keine Sperre
- [ ] Ein privater Runde fehlt das Einladen. „Privat" heißt derzeit nur: steht nicht in der Liste

### 5.4 Cosmetics und Gadgets — erledigt
- [x] Katalog und Besitz beim Launcher (`cosmetics.yml`), Effekt-Code auf dem Spielserver
- [x] Kauf komplett im Launcher: Preis lesen, Bits abbuchen, gutschreiben, in einem Schritt
- [x] Shop als Knopf im Marktplatz auf Survival, ein Klick kauft/legt an/legt ab
- [x] Adminmenü: freischalten, verkäuflich, Preis, für alle gratis
- [x] Sieges-Effekt „Raketen" (für alle gratis, ersetzt das alte Feuerwerk am Rundenende)
- [x] Sieges-Effekt „Tinte": Explosionen von der Bauhöhe über die Map, ohne Schaden und Rückstoß
- [x] Gadget „Endlos-Perle": kommt nach dem Cooldown zurück, Cooldown in den Cosmetic-Settings

Offen:
- [ ] Der Cooldown der Endlos-Perle steht auf 22 Ticks — Vanilla plus die zehn Prozent. Das ist
      fast geschenkt. Wenn sich das im Spiel als zu stark zeigt, ist es eine Zahl in
      `cosmetics.yml` unter `endless-pearl.settings.cooldown-ticks`
- [ ] Gekauft und angelegt wird nur auf Survival. Wer nur Bedwars spielt, muss dafür einmal
      rüber — die Effekte selbst laufen überall
- [ ] Der Besitz aller Spieler wird komplett an jeden Server verteilt, wie bei den Teams. Bei
      vielen Spielern ist das irgendwann zu viel, dann müsste pro Spieler nachgeladen werden
- [ ] Nur Sieges-Effekte und ein Gadget. Für weitere Arten (Killeffekte, Partikelspuren) gibt es
      noch keinen Typ

### 5.5 Am lebenden Server nachprüfen
- [ ] Übernahme der alten `money-config.yml` beim ersten Start des Launchers
- [ ] Zwei Spieler starten gleichzeitig eine Runde, wenn nur noch für eine Platz ist
- [ ] Die gemessenen Spitzen sind plausibel (RSS ist mehr als der Heap — der Vorschlag rechnet mit
      Faktor 1,4 auf die Spitze, das sollte an echten Zahlen geprüft werden)
- [ ] Tinte auf einer vollen Runde: kostet es TPS?
- [ ] Rundenadmin kickt jemanden, der danach wieder joinen will

---

## Erledigt in dieser Runde

- Launcher startete nicht: `mvnw` und die MC-Server liefen über `java` aus dem PATH (21)
  statt über das JDK des Launchers (25). `FileHandler.build()` und `ServerInstance.start()`
  geben jetzt beide ihre eigene JVM weiter.
- Team umbenennen war generell kaputt (Create+Delete löste die "schon in einem anderen Team"-
  Prüfung aus). Jetzt atomarer Rename, Backpack zieht mit um.
- Shopkeeper: `save()` tötete den Villager, nichts erreichte die Platte, der Ladepfad brauchte
  100 Sekunden und tötete den vorhandenen Villager.
- Marktplatz `/shop` mit Reitern, Sortierung, Ausverkauft-/Nur-Verkauft-Toggles,
  Konkurrenzansicht, Vergleich pro Stück und Rotation bei gleichem Preis.
- Dupe in `removeItem` (hing an Mirror-Semantik), Itemverlust bei vollem Inventar,
  NPE durch leeres Angebot, ungeprüfte Drags.
