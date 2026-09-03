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

Erledigt:
- [x] Ein Lauf, den niemand fortsetzt, wird nach 24 Stunden ohne Anfassen als ABGEBROCHEN
      geschlossen und sein Server weggeräumt (`runs.abandon-after-hours`, `0` = nie)
- [x] Ein abgesagtes Event wird nicht mehr sofort abgewickelt: seine Server gehen aus, die
      Läufe bleiben pausiert liegen, bis die geplante Zeit wirklich vorbei ist. "Wieder
      aktivieren" bringt sie damit zurück

Offen:
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

- [ ] `MoneyHandler.addMoney()` an den Verkäufer kann fehlschlagen. Das Loch ist kleiner
      geworden, seit das Geld beim Launcher liegt: eine Gutschrift ist eine Differenz, die
      dort angewandt wird, und der einzige verbleibende Fehlerfall ist eine Nachricht, die
      das Netz nicht erreicht. Die wird jetzt dreimal versucht und im Fehlerfall mit Konto
      und Betrag als `LOST BALANCE CHANGE` geloggt, damit sie von Hand korrigierbar ist.
      Was fehlt, ist eine dauerhafte Warteschlange, die den Betrag nach einem Absturz noch
      hat. Bewusst nicht gebaut — soll das rein?
- [ ] `/shop debug` als Diagnose: listet Shops ohne Villager und verwaiste Villager mit
      Shop-ID. Nur bauen, falls die Villager-Prüfung oben Probleme zeigt.

---

## 4. Kleinkram im Code

- [x] `VelocityConfigurator.java:25` — Workaround raus. `velocity.toml` gehört dem Launcher
      komplett und wird bei jedem Start neu geschrieben; die Schreibaufrufe sagen das jetzt,
      statt `firstTime` zu belügen
- [x] `LobbyPlugin.java:31` — der Parkour ist längst da, nur der Kommentar stand noch
- [x] `.idea/misc.xml` steht auf JDK 25
- [ ] `Inventorys.java:217` — `//TODO: add option` im Item-Manager-Inventar. Aus dem Code geht
      nicht hervor, welche Option gemeint ist — **was soll da hin?**
- [x] `Main.java:54` — Ops kommen jetzt über Discord: `/op` und `/deop`, nur für den Besitzer,
      schreiben in dieselbe `ops`-Liste und werden auf laufenden Servern sofort angewandt
- [x] `VerifyAccount.java:5` — Account-Verknüpfung ist da: `/verify <name>` im Discord gibt einen
      Code, `/verify <code>` im Spiel verknüpft, `/verify wer <spieler>` schaut nach. Die alte
      leere `VerifyAccount`-Klasse ist raus

---

## 5. Eigene Runden, Speicher, Geld und Cosmetics

Stand: 2026-09-02. Alles gebaut und kompiliert, nichts davon auf einem laufenden Server geprüft.

### 5.1 Geld gehört jetzt dem Launcher — erledigt
- [x] `money.yml` beim Launcher, `MoneyStore` als einziger Schreiber
- [x] Änderungen sind Differenzen unter einem Lock, jede angewandte wird ans Netz gemeldet
- [x] `configs/money-config.yml` von Survival wird beim ersten Start einmal übernommen
- [x] `MoneyHandler` behält seine Signaturen, antwortet aus der lokalen Kopie
- [x] `MoneyService.changeBlocking` als strenger Weg, wo etwas Wertvolles herausgeht

- [x] Eine Änderung, die das Netz nicht erreicht, wird dreimal versucht und sonst als
      `LOST BALANCE CHANGE` mit Konto und Betrag geloggt

Offen:
- [ ] `MoneyHandler.removeMoney` ist eine Schätzung, wenn dasselbe Konto auf zwei Servern in
      derselben Sekunde leergeräumt wird. Der Launcher lehnt die zweite Änderung ab und korrigiert
      die Kopie, aber der zweite Server hat da schon „ja" gesagt. Für Shop-Käufe eines Spielers auf
      einem Server ist das dicht; falls das mal weh tut, müssen die Aufrufer auf `changeBlocking`
      umgestellt werden
- [ ] Keine dauerhafte Warteschlange für Änderungen, die nach drei Versuchen nicht rausgehen.
      Nach einem Absturz in genau diesem Moment ist der Betrag nur noch im Log
- [ ] `AwardService` zahlt Geldpreise weiterhin nur auf Survival aus. Das war nötig, solange nur
      Survival Geld kannte — jetzt könnte jeder Server das

### 5.2 Speicherbudget und Empfehlung — erledigt
- [x] Der Launcher kennt die Größe der Maschine und hält eine Reserve frei
- [x] Starts, die nicht mehr ins Budget passen, werden abgelehnt und gezählt
- [x] Ein bewilligter Start hält seinen Speicher, bis sein Server wirklich läuft
- [x] Alle 30 Sekunden wird gemessen, was jeder Server hält (RSS aus `/proc`), Spitze bleibt stehen
- [x] Panel im Server Manager: Budget, abgelehnte Starts, Vorschläge mit Zahlen dahinter
- [x] Umsetzbar im Spiel: RAM pro Server setzen, gilt beim nächsten Start dieses Servers

- [x] Der Vorschlag lässt sich anklicken: ein Klick schreibt den vorgeschlagenen Wert weg

Offen:
- [ ] Gemessen wird nur unter Linux. Auf Windows zeigt das Panel das Budget, aber keine Vorschläge
- [ ] `ServerHandler.startNewInstance` lehnt selbst nichts ab. Das Budget greift über die
      Slot-Anfrage, die die Lobby stellt — ein Admin, der im Server Manager einen Server erstellt,
      kann das Budget bewusst überziehen. Absicht; falls das doch verhindert werden soll, gehört
      die Prüfung zusätzlich in `startNewInstance`
- [ ] Ein übernommener Vorschlag gilt erst beim nächsten Start dieses Servers. Ein Knopf, der den
      Server gleich mit durchstartet, fehlt — bei SURVIVAL wäre das aber nichts, was man aus
      Versehen anklicken will

### 5.3 Selbst gestartete Runden — erledigt
- [x] `/runde` in der Lobby: laufende Runden sehen, beitreten, eigene aufmachen
- [x] Map, Modus, Addons und öffentlich/privat vor dem Start wählbar
- [x] Vom Admin freischaltbar, dazu Limits pro Spieler, insgesamt, Wartezeit und Event-Sperren
- [x] Rundenadmin ist, wer startet: Wartelobby steuern, kicken, privat schalten
- [x] Der Rundenserver liest seine Runde beim Start über den eigenen Namen, wie die Events auch
- [x] Der Launcher räumt Runden auf, deren Server weg ist

- [x] „Privat" ist echt: eine geschlossene Runde hat eine Gästeliste, `/runde einladen <spieler>`
      in der Lobby oder der Einladen-Knopf im Rundenmenü füllt sie, und wer ohne Einladung
      hinwarpt, wird auf dem Rundenserver zurückgeschickt
- [x] Maps, die von Hand dazukommen: Weltordner nach `./bedwars-maps` beim Launcher, landen auf
      jedem neuen Rundenserver und stehen im Lobby-Menü

Offen:
- [ ] Der Rundenadmin kann die Runde nicht selbst beenden. Bewusst so — der Idle-Watchdog macht den
      Server zu, sobald der letzte raus ist
- [ ] Rausgeworfene Spieler bleiben nur für die Lebensdauer des Rundenservers draußen. Da der
      Server mit der Runde endet, reicht das — es ist trotzdem keine Sperre
- [ ] Eingeladen werden kann nur, wer gerade online ist. Eine Einladung an jemanden, der später
      kommt, gibt es nicht
- [ ] Eine Map ohne `<name>.yml` kommt unfertig auf dem Server an und muss dort einmal mit
      `/bw setup` eingerichtet werden. Das ist richtig so, heißt aber: einfach eine Welt
      hinlegen reicht noch nicht zum Spielen

### 5.4 Cosmetics und Gadgets — erledigt
- [x] Katalog und Besitz beim Launcher (`cosmetics.yml`), Effekt-Code auf dem Spielserver
- [x] Kauf komplett im Launcher: Preis lesen, Bits abbuchen, gutschreiben, in einem Schritt
- [x] Shop als Knopf im Marktplatz auf Survival, ein Klick kauft/legt an/legt ab
- [x] Adminmenü: freischalten, verkäuflich, Preis, für alle gratis
- [x] Sieges-Effekt „Raketen" (für alle gratis, ersetzt das alte Feuerwerk am Rundenende)
- [x] Sieges-Effekt „Tinte": Explosionen von der Bauhöhe über die Map, ohne Schaden und Rückstoß
- [x] Gadget „Endlos-Perle": kommt nach dem Cooldown zurück, Cooldown in den Cosmetic-Settings

Nachgezogen (2026-09-03), alles gebaut und nichts davon auf einem laufenden Server geprüft:
- [x] `/cosmetics` auf Lobby, Bedwars und Survival — der Marktplatz-Knopf bleibt zusätzlich
- [x] Zwei neue Arten: Kill-Effekte und Partikelspuren, mit eigener Registry je Art
      (`KillEffects`, `Trails`) und einem gemeinsamen `CosmeticEffects.init` statt drei
      Einzelaufrufen pro Plugin
- [x] Neue Effekte: Gewitter und Lichtsäule (Sieg), Blitzschlag, Seelen und Stichflamme (Kill),
      Flammenspur, Sternenstaub und Noten (Spur)
- [x] Gadget-Framework in CommonCode (`Gadget`, `Gadgets`); die Endlos-Perle ist von Bedwars
      dorthin gezogen, neu dazu der Enterhaken
- [x] Gadgets sind pro Server abgeschaltet, bis ein Spielmodus sie freischaltet — nur Bedwars tut
      das, und nur für Spieler, die in der Runde sind
- [x] Besitz wird pro Spieler beim Join geladen und eine Minute nach dem Quit vergessen; der
      Katalog reist weiter für alle. Die alte Vollverteilung antwortet der Launcher weiterhin,
      damit ein älterer Server beim Rollout nicht plötzlich ohne Besitz dasteht

Offen:
- [ ] Der Cooldown der Endlos-Perle steht auf 22 Ticks — Vanilla plus die zehn Prozent. Das ist
      fast geschenkt. Wenn sich das im Spiel als zu stark zeigt, ist es eine Zahl in
      `cosmetics.yml` unter `endless-pearl.settings.cooldown-ticks`
- [ ] Der Enterhaken nimmt seinem Träger sechs Sekunden lang den Fallschaden, damit die Landung
      ihn nicht umbringt. In Bedwars ist der Sturz die Map — wenn sich das als zu stark zeigt,
      ist es `grappling-hook.settings.no-fall-millis`
- [ ] Kill-Effekte hängen am vanilla `PlayerDeathEvent`. Ein Bedwars-Tod ohne den — der Sturz ins
      Void — löst deshalb keinen aus. Für einen Effekt über einem leeren Loch ist das richtig,
      für einen Void-Kill mit Verursacher wäre es diskutabel
- [ ] Gadgets gibt es weiterhin nur in Bedwars. Für die Lobby müsste jemand entscheiden, ob ein
      Enterhaken dort ausgegeben wird und was der Lobbyschutz dazu sagt

### 5.5 Am lebenden Server nachprüfen
- [ ] Übernahme der alten `money-config.yml` beim ersten Start des Launchers
- [ ] Zwei Spieler starten gleichzeitig eine Runde, wenn nur noch für eine Platz ist
- [ ] Die gemessenen Spitzen sind plausibel (RSS ist mehr als der Heap — der Vorschlag rechnet mit
      Faktor 1,4 auf die Spitze, das sollte an echten Zahlen geprüft werden)
- [ ] Tinte auf einer vollen Runde: kostet es TPS?
- [ ] Rundenadmin kickt jemanden, der danach wieder joinen will
- [ ] Eine private Runde: jemand ohne Einladung warpt direkt auf den Servernamen
- [ ] Eine Welt nach `./bedwars-maps` legen und prüfen, dass sie auf dem nächsten Rundenserver
      liegt und im Lobby-Menü steht
- [ ] Ein abgesagtes Event wieder aktivieren und prüfen, dass seine Läufe noch da sind
- [ ] `velocity.toml` nach einem zweiten Start des Launchers: stehen alle Server drin?

### 5.6 Discord-Verknüpfung und Ops — erledigt
- [x] `/verify <minecraftname>` im Discord gibt einen Code (6 Zeichen, 10 Minuten, ephemeral)
- [x] `/verify <code>` im Spiel verknüpft, geprüft wird beim Launcher gegen die UUID
- [x] `/verify wer <spieler>` sagt, wer das auf Discord ist (Op oder `network.verify.lookup`)
- [x] `/unlink` im Discord löst eine Verknüpfung, nur der Besitzer
- [x] `/op` und `/deop` im Discord, nur der Besitzer, gelten auf laufenden Servern sofort
- [x] Die Besitzer-ID steht in `discord-owner-id` statt fest im Code von `/payingplayer`

Offen:
- [ ] `/verify` gibt es in Lobby und Survival. Auf einem Bedwars-Rundenserver nicht — dort steht
      man selten, wenn man gerade einen Code eintippt, aber es ist eine Lücke
- [ ] Ein Spieler, der sich umbenennt, behält den alten Namen in `links.yml`. Die UUID stimmt,
      die Anzeige nicht — beim nächsten Verknüpfen wird der Name aktualisiert
- [ ] Die Verknüpfung steht nirgends in der Admin-Website, nur im Spiel
- [ ] Nachprüfen: `/op` auf einem laufenden Server — bekommt der Spieler die Rechte wirklich
      sofort und stehen sie nach einem Neustart noch in der `ops.json`?

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
