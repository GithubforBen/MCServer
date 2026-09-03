# Bedwars - was getestet werden sollte

Stand: Phase 7 (Feinschliff) - damit ist der Plan durch. Die Punkte davor sind aus den früheren Phasen und lohnen sich nur noch
als Gegenprobe, wenn etwas davon plötzlich kaputt aussieht.

## Vorbereitung

```bash
JAVA_HOME=~/.jdks/openjdk-25.0.1 ./mvnw -pl Bedwars package   # Jar landet in builds/BEDWARS
```

- Eine eingerichtete Map, `/bw setup check` grün.
- **Neu nötig:** Händlerpunkte pro Team, sonst steht kein Villager da:
  `/bw setup <map>` → hinstellen → `/bw setup team RED shop` und `/bw setup team RED upgrade`
  → `/bw setup save`.
- Ressourcen zum Kaufen: `/give` für Eisen, Gold, Diamant, Smaragd. Kreativmodus hilft **nicht**,
  der Shop schaut ins Inventar.
- Für Traps und Teamschaden ein zweiter Account (oder ein zweiter Client).
- `/bw start` startet die Runde ohne Wartezeit, `/bw shop` und `/bw upgrades` öffnen die Menüs
  auch dann, wenn noch kein Villager steht.
- **Für Phase 6:** `/bw addons` öffnet als Spieler das Schaltmenü (Konsole bekommt die Liste als
  Text), `/bw addon <id> on|off|default` tut dasselbe im Chat. Zum Testen einzelner Addons am besten
  alles andere ausschalten.
- **Für Phase 5 der wichtigste Griff:** `/bw timeline` zeigt den Fahrplan, `/bw timeline skip` zieht
  das nächste Ereignis sofort vor. Ohne das dauert Bed Destruction 30 Minuten und Sudden Death 40.
  Wer lieber echte Zeiten testet, schreibt sie in `configs/bedwars/timeline.yml` klein (z.B. 20/40/60)
  und macht `/bw reload` **vor** dem Rundenstart - die Uhr liest den Fahrplan beim Start.

## 1. Configs

- [ ] Erster Start legt `configs/bedwars/shop.yml` und `upgrades.yml` an, mit Kommentaren,
      6 Kategorien, 6 Upgrades, 4 Traps.
- [ ] Preis in `shop.yml` ändern (z.B. Wolle auf 1 Eisen) → `/bw reload` → Menü zeigt den neuen Preis.
- [ ] Unfug hineinschreiben (`material: NOT_A_BLOCK`) → Warnung in der Konsole, Server läuft weiter,
      nur der eine Eintrag fehlt.
- [ ] Die Dateien bleiben lesbar: keine hundert Zeilen `slot: -1` / `permanent: false`.

## 2. Händler

- [ ] Nach dem Rundenstart stehen in jeder besetzten Basis zwei Villager mit Namensschild.
- [ ] Rechtsklick Item-Händler → Shop. Rechtsklick Upgrade-Händler → Upgrades.
- [ ] Der Villager lässt sich nicht schlagen, nicht schieben, nicht in den Vanilla-Handel bringen,
      und läuft nicht weg.
- [ ] Direkt an ihn bauen geht nicht (`build.shop-radius`).
- [ ] Als Zuschauer: kein Menü, nur der Hinweis.
- [ ] Runde vorbei (`/bw stop`) → Villager sind weg.

## 3. Shop

- [ ] Die Tabs oben wechseln die Seite, die offene Seite ist markiert.
- [ ] Zu wenig Ressourcen: rote Zeile, Klick nimmt nichts, Villager-Nein-Ton.
- [ ] Kauf zieht **genau** den Preis ab. Volles Inventar → der Rest fällt vor die Füße.
- [ ] Wolle, Glas und Hardened Clay kommen in Teamfarbe.
- [ ] Schwertkauf nimmt das Holzschwert weg.
- [ ] Rüstungskauf: Hose und Schuhe wechseln, Helm und Brust bleiben gefärbtes Leder.
- [ ] Etwas Dauerhaftes doppelt kaufen (Schere, Rüstungsstufe, Werkzeugstufe) → "You already own this",
      kein Abzug.
- [ ] Werkzeugkauf ersetzt die schwächere Stufe derselben Kette - danach liegt nur eine Spitzhacke
      im Inventar, nicht zwei.

## 4. Tod und Respawn

- [ ] Nach dem Tod sind Rüstung und Schere wieder da.
- [ ] Spitzhacke und Axt kommen **eine Stufe niedriger** zurück, aber nie unter die erste Stufe.
- [ ] Gekaufte Items droppen nicht; nur Ressourcen gehen an den Killer.
- [ ] Team-Upgrades liegen nach dem Respawn wieder auf den Sachen (Schärfe, Schutz, Eile).
- [ ] Ein Upgrade, das gekauft wurde, während man tot war, ist beim Respawn trotzdem drauf.

## 5. Upgrades

- [ ] Das Menü zeigt Level x/max und den Preis der **nächsten** Stufe.
- [ ] Sharpened Swords: alle Schwerter und Äxte des ganzen Teams - auch beim Teamkollegen, auch bei
      einem Schwert, das erst danach gekauft wird.
- [ ] Reinforced Armor I-IV: Schutz auf allen vier Teilen, Stufe passt zum Level.
- [ ] Maniac Miner II: Eile II.
- [ ] Iron Forge: der Generator in der **eigenen** Basis wird schneller (`/bw generators` zeigt die
      Stufe), die Diamanten in der Mitte bleiben, wie sie waren.
- [ ] Heal Pool: Regeneration nur in der eigenen Basis, hört draußen nach ein paar Sekunden auf.
- [ ] Dragon Buff: kaufbar und gemerkt - wirken tut er erst in Phase 5.
- [ ] Wither Buff: 14 Stufen, Preis 5 → 10 → 15 → ... → 70 Diamanten, wirkt erst in Phase 5.
- [ ] Höchste Stufe erreicht → "Fully upgraded", kein weiterer Abzug.
- [ ] Jeder Kauf wird im ganzen Team angesagt.

## 6. Traps

- [ ] Die Preise steigen 1 → 2 → 4 Diamanten mit der Länge der Warteschlange.
- [ ] Die Warteschlange oben im Menü zeigt die gekauften Traps in der Reihenfolge.
- [ ] Vierter Trap → "queue is full".
- [ ] Ein Gegner betritt die Basis → der erste Trap geht los: Titel und Ton fürs ganze Team.
- [ ] It's a Trap: Blindheit und Langsamkeit beim Eindringling.
- [ ] Miner Fatigue: der Eindringling kann nicht abbauen.
- [ ] Counter-Offensive: Speed und Sprungkraft für die Verteidiger zuhause.
- [ ] Alarm: Unsichtbarkeit weg, Name des Eindringlings in der Nachricht.
- [ ] **Abklingzeit**: der zweite Trap geht nicht sofort hinterher (Standard 5 Sekunden), auch wenn
      der Gegner stehen bleibt.
- [ ] Eigene Teammitglieder lösen nichts aus.
- [ ] Magic Milk trinken → Traps ignorieren einen 30 Sekunden lang, und der Speed-Trank bleibt
      erhalten (Milch darf hier **nicht** die Effekte löschen).

## 7. Spezial-Items

- [ ] TNT zündet von selbst, rund zwei Sekunden nach dem Setzen.
- [ ] Die Explosion nimmt **nur** selbst gebaute Blöcke mit - nicht die Map, und nie ein Bett.
- [ ] Ein TNT-Kill wird dem gutgeschrieben, der es gesetzt hat.
- [ ] Fireball fliegt geradeaus, wirft zurück und setzt nichts in Brand.
- [ ] Bedbug: Silberfisch greift Gegner an, nicht das eigene Team, und verschwindet nach 15 Sekunden.
- [ ] Dream Defender: Eisengolem genauso, nach 240 Sekunden.
- [ ] Goldapfel, Enderperle, Wassereimer, Schwamm und Leiter tun das, was sie in Vanilla tun.

## 8. Timeline

- [ ] Erster Start legt `configs/bedwars/timeline.yml` an: sieben Ereignisse mit Kommentaren,
      dazu die Blöcke `sudden-death` und `points`.
- [ ] `/bw timeline` listet alle Ereignisse: erledigte durchgestrichen, das nächste gelb mit
      Restzeit, der Rest grau.
- [ ] Die Sidebar zeigt eine Zeile "Diamond II in 05:12", und die Zeit läuft wirklich herunter.
- [ ] In der **Warte-Lobby** steht diese Zeile nicht - die Uhr läuft erst ab Rundenstart.
- [ ] Zeiten in `timeline.yml` ändern → `/bw reload` → `/bw start` → der neue Fahrplan gilt.
- [ ] Diamond II: die Generatoren **in der Mitte** werden schneller (`/bw generators`), die in den
      Basen bleiben, wie sie waren.
- [ ] `/bw timeline skip` setzt das nächste Ereignis sofort an - und die danach behalten ihren
      Abstand (nach einem Skip auf Bed Destruction ist Sudden Death 10 Minuten später, nicht sofort).

## 9. Bed Destruction

- [ ] Ansage in Chat und als Titel für alle, mit Ton.
- [ ] **Alle** noch stehenden Betten sind wirklich weg, beide Blockhälften, ohne Item-Drop.
- [ ] Wer sein Bett verliert, bekommt zusätzlich die persönliche Zeile.
- [ ] Danach ist jeder Tod ein Final Kill, und die Sidebar zeigt die Teams ohne Bett-Haken.
- [ ] Ein Team, dessen Bett vorher schon weg war, wird nicht doppelt angesagt.

## 10. Sudden Death

- [ ] Pro lebendem Team ein Drache, mit Namensschild in Teamfarbe, über der Mitte der Map.
- [ ] Der eigene Drache tut dem eigenen Team **nichts**: nicht im Anflug, nicht mit dem Feuerball,
      nicht in der Wolke, die der Feuerball hinterlässt - und er sucht sich kein eigenes Teammitglied
      als Ziel.
- [ ] Der fremde Drache tut sehr wohl weh.
- [ ] Der Drache **jagt**: er kommt zum nächsten gegnerischen Spieler, auch bis in dessen Basis in
      der Ecke der Map, und bleibt dort, solange der Spieler dort ist.
- [ ] Er hängt nicht über der Mitte fest, sobald irgendwo ein Gegner steht - und kehrt dorthin
      zurück, wenn keiner mehr erreichbar ist.
- [ ] Der Drache **landet nicht** und setzt sich nicht auf den Boden.
- [ ] Der Drache reißt die Map auf, wo er durchfliegt: Wolle, Endstein, Sandstein, alles
      (`sudden-death.carve-radius`), es droppt nichts, und was in `sudden-death.indestructible`
      steht - Bedrock, Barrier - bleibt stehen.
- [ ] Nach ein paar Minuten fehlt der Map sichtbar Boden, und Spieler fallen ins Void.
- [ ] Auch die Explosionen des Drachen nehmen Map-Blöcke mit; TNT und Feuerball von Spielern
      **nicht** (Phase 4 gilt unverändert weiter).
- [ ] Dragon Buff gekauft → dieses Team hat zwei Drachen.
- [ ] Drache getötet → eine Zeile im Chat, keine Drops, kein XP, kein Portal.
- [ ] Runde vorbei → alle Drachen sind sofort weg.

## 10a. Wither-Wellen

- [ ] 5 Minuten nach Sudden Death (`sudden-death.wither-delay-seconds`) kommt die erste Welle,
      danach jede Minute (`sudden-death.wither-interval-seconds`) eine weitere.
- [ ] Ohne Upgrade bekommt jedes **lebende** Team pro Welle einen Wither, mit Namensschild in
      Teamfarbe, verteilt über der Mitte - nicht alle auf demselben Block.
- [ ] Wither Buff Stufe n → n+1 Wither pro Welle für dieses Team, gedeckelt bei 15
      (`sudden-death.wither-maximum`).
- [ ] Der eigene Wither tut dem eigenen Team nichts und zielt nicht auf es - auch nicht mit dem
      Schädel und nicht mit dem Wither-Effekt.
- [ ] Kein Wither schießt auf die Shop-Villager.
- [ ] Die Wither reißen ebenfalls Löcher in die Map, Bedrock und Barrier ausgenommen.
- [ ] Keine Bossbar pro Wither (sonst 60 Balken übereinander); der Drache hat weiterhin seine.
- [ ] Wither getötet → eine Zeile im Chat, keine Drops, kein XP.
- [ ] `/bw timeline skip` auf Sudden Death → die erste Welle kommt 5 Minuten **danach**, nicht
      sofort.
- [ ] Runde vorbei → alle Wither sind sofort weg.

## 11. Zeitlimit und Endbildschirm

- [ ] `/bw timeline skip` bis Game End: die Runde endet, auch wenn noch zwei Teams stehen.
- [ ] Die Wertung wird angezeigt: Punkte pro Team mit Betten, Finals und Kills, ausgeschiedene
      Teams grau.
- [ ] Der Sieger ist der mit den meisten Punkten **unter den noch lebenden Teams** - ein
      ausgeschiedenes Team gewinnt nichts, auch wenn es vorher am meisten gerissen hat.
- [ ] Gleichstand an der Spitze → niemand gewinnt ("Nobody won").
- [ ] Gewichte in `timeline.yml` (`points.bed` usw.) ändern → die Entscheidung ändert sich mit.
- [ ] Endbildschirm: Sieger-Titel für die Gewinner, "GAME OVER" für alle anderen.
- [ ] Top 3 der Runde mit Kills, Finals und Betten; eine Runde, in der nichts passiert ist, zeigt
      die Liste gar nicht.
- [ ] Feuerwerk über den Gewinnern, in Teamfarbe, und **niemand nimmt dabei Schaden**.
- [ ] Nach `end.return-seconds` gehen alle zurück in die Lobby und der Server stoppt sich
      (bei `end.stop-server: false` bleibt er stehen - so testet es sich leichter).

## 12. Addons allgemein

- [ ] `addons.yml` listet nach dem ersten Start alle fünf Addons mit Beschreibung, `enabled` und
      einem `settings`-Block mit Kommentaren.
- [ ] `random-events` ist standardmäßig **aus**, die anderen vier an.
- [ ] `/bw addons` als Spieler: Menü mit grünen und grauen Farbstoffen, Klick schaltet um, die
      Quelle ("addons.yml" / "this round") steht in der Lore.
- [ ] Umschalten während der laufenden Runde wird abgelehnt.
- [ ] Ein Addon ausschalten heißt wirklich aus: keine Shop-Einträge, keine Menüs, keine Effekte.
- [ ] Preis in `addons.yml` ändern → `/bw reload` → der neue Preis steht im Shop.

## 13. Bed Token

- [ ] Am **eigenen** Händler ist er nicht zu sehen, auch nicht ausgegraut. `/bw shop` zeigt ihn auch
      nicht.
- [ ] Am Händler eines fremden Teams ist er da und kostet 8 Diamanten **und** 16 Smaragde; beide
      Zeilen stehen in der Lore.
- [ ] Zu wenig von einer der beiden Währungen → gar nichts wird abgezogen.
- [ ] Solange das eigene Bett noch steht: Kauf abgelehnt.
- [ ] Kauf → Ansage an alle, der Träger leuchtet.
- [ ] Träger stirbt → der Token liegt leuchtend am Boden und ist aufsammelbar (er verschwindet
      **nicht** mit dem Rest des Inventars).
- [ ] Rechtsklick irgendwo sonst → Hinweis, kein Bett.
- [ ] Rechtsklick an der eigenen Bettstelle → das Bett steht wieder, in Teamfarbe und in derselben
      Richtung wie vorher, Ansage an alle.
- [ ] Danach funktioniert das Bett normal: Respawn ja, zweites Zerstören möglich.
- [ ] Zweiter Token für dasselbe Team → abgelehnt (`per-team`).
- [ ] Team schon eliminiert → abgelehnt.

## 14. Kits

- [ ] In der Warte-Lobby liegt neben der Wolle eine Kiste, die das Kit-Menü öffnet.
- [ ] Auswahl wird bestätigt und im Menü als gewählt markiert.
- [ ] Rundenstart: die Kit-Items sind zusätzlich zur Grundausstattung da, der Perk läuft
      (unendliche Dauer, kein blinkender Countdown).
- [ ] Nach dem Tod ist das Kit **wieder** da - Items und Perk.
- [ ] Wer nichts wählt, spielt ohne Kit (oder das, was `default` sagt).
- [ ] Ein Kit in `kits.yml` mit Unfug (`NOT_A_MATERIAL`) → Warnung, Rest des Kits funktioniert.

## 15. Custom-Items

- [ ] Eigene Shop-Seite "Specials" mit vier Einträgen.
- [ ] Enterhaken: zieht einen zum Haken, verbraucht sich dabei.
- [ ] Rettungsplattform: 3x3 Slime unter den Füßen, verschwindet nach 10 Sekunden - und nimmt nur
      das mit, was noch Slime ist, nicht was man daraufgebaut hat.
- [ ] Brücken-Ei: legt eine Brücke in Teamfarbe, schlüpft **kein** Huhn.
- [ ] Was Ei und Plattform bauen, lässt sich abbauen und mit TNT sprengen (Block-Tracker).
- [ ] Sprungfeder: platzieren, drauftreten, fliegen. Zweimal schnell hintereinander wirft nicht
      doppelt (Cooldown).
- [ ] Eine Druckplatte, die zur Map gehört, wirft niemanden.

## 16. Killstreaks und Kopfgeld

- [ ] 3 Kills in Folge → Effekt, Titel und Ansage; 5 und 10 ebenso.
- [ ] Sterben setzt die Serie zurück.
- [ ] Ab 3 Kills liegt ein Kopfgeld auf einem, angesagt an alle.
- [ ] Wer ihn tötet, bekommt die Diamanten wirklich ins Inventar, mit Ansage.
- [ ] Das Kopfgeld wächst mit der Serie und ist bei `bounty.maximum` gedeckelt.
- [ ] `bounty.maximum: 0` schaltet nur das Kopfgeld aus, die Serien-Buffs bleiben.

## 17. Zufalls-Events

- [ ] Einschalten (`/bw addon random-events on`) und `interval-seconds` klein setzen.
- [ ] Erst die Vorwarnung mit Titel und Countdown, dann das Ereignis.
- [ ] Ressourcenregen fällt über der Mitte.
- [ ] Schnellere Generatoren: `/bw generators` zeigt die höhere Stufe, nach der Zeit wieder die
      alte - auch dann, wenn zwischendurch ein Timeline-Ereignis die Stufe erhöht hat.
- [ ] Loot-Kiste steht in der Mitte, ist gefüllt und lässt sich abbauen.
- [ ] Rundenende → keine Kiste bleibt stehen, keine Ereignisse mehr.

## 18. Feinschliff

- [ ] **Feuer**: Feuerball auf eine Holzbrücke → die Brücke brennt nicht ab, das Feuer springt nicht
      weiter.
- [ ] **Ender-Perle**: zweite Perle innerhalb von 5 Sekunden → Hinweis, und die Perle bleibt im
      Inventar (sie darf nicht verbraucht werden).
- [ ] **Abmelden im Kampf**: jemanden anschlagen, der sich sofort ausloggt → Tod wird gezählt und der
      Kill dem Angreifer gutgeschrieben, Ansage im Chat.
- [ ] **Wiederverbinden**: mit noch stehendem Bett ausloggen und neu verbinden → man steht wieder im
      eigenen Team, mit Grundausstattung, Rüstung und Upgrades.
- [ ] Ohne Bett ausloggen → man kommt als Zuschauer zurück, das Team kann eliminiert werden.
- [ ] **Zuschauer**: kein Aufsammeln, kein Bauen, kein Schlagen, kein Geschlagenwerden - auch in den
      5 Sekunden zwischen Tod und Respawn.
- [ ] `/bw watch` zeigt alle, die noch stehen, mit Team und Kills; Klick teleportiert hin.
- [ ] `/bw stats` zeigt die Tabelle der laufenden Runde.
- [ ] Rundenende → eine Datei unter `./stats/round_<datum>.yml` mit Map, Modus, Dauer, Sieger und
      allen Spielerzeilen.
- [ ] `stats.enabled: false` → keine Datei, sonst unverändert.
- [ ] **Generatoren**: den Eisengenerator eine Minute laufen lassen → dort liegt ein Stapel und nicht
      ein Dutzend einzelner Items.

## 19. Querschnitt

- [ ] **Kein Schaden im eigenen Team** - Nahkampf, Pfeil, Fireball, TNT.
- [ ] Alles, was im Spiel steht, kommt aus `messages.yml` (englisch, keine deutschen Reste).
- [ ] Eine ganze Runde von Anfang bis Sieg mit Shop und Upgrades, ohne dass die Konsole etwas
      Rotes schreibt.

## Was bewusst noch nicht geht

- **Das Kopfgeld steht nicht über dem Kopf** - es wird angesagt und ausgezahlt, aber der Name im
  Tab und über dem Spieler bleibt, wie er ist.
- **Netzwerkweite Statistik**: die Runde schreibt eine Datei pro Server; sie beim Launcher
  einzusammeln ist ein eigenes Netzwerk-Event und noch nicht gebaut.
- **Rundenstart über das Event-System**: läuft weiter über `/bwdebug` und `/bw start`; die
  `MatchData`, die Modus, Map und Addons mitbringt, kommt mit dem Event-System.
- **Iron Forge IV** macht den Generator nur schneller; die Smaragde am eigenen Base, die Hypixel
  dort ausschüttet, gibt es noch nicht.
- **Gold-Spitzhacke** hat Efficiency III, aber keine Eile II beim Halten.
