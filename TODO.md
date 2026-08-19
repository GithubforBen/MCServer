# TODO

Stand: 2026-08-19. Offene Punkte aus der Shopkeeper-/Marktplatz-Runde und die noch nicht
begonnene Event-Arbeit.

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

Offen:
- [ ] Was passiert am Ende eines Laufs mit den Spielern? Sie bleiben als Zuschauer
      auf dem Run-Server, es gibt keinen Rückwarp in die Lobby
- [ ] Ein Lauf, den niemand je fortsetzt, bleibt bis zum Eventende PAUSED — erst dann
      räumt ihn die Abwicklung weg
- [ ] Die Welten gestoppter Run-Server bleiben auf der Platte liegen
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
- [ ] Eventsystem auch in der Lobby verfügbar
- [ ] Lobby-Events mit Teilnehmerzahl und Preisen, kürzerer Zeitraum (ca. 1 Stunde)
- [ ] Später: Events direkt aus der Lobby heraus erstellen

### 1.6 Lobby-Welt
- [ ] Es gibt nur eine Lobbywelt, die automatisch aus einer Datei geladen wird
- [ ] Platzhalter bis dahin: die Welt, die gerade in der Lobby liegt
- [ ] Richtige Welt wird noch geliefert und muss dann eingebunden werden

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
