#!/bin/bash
# Ablauf fuer das Labor im Spiel (Change lab-brewing, Gruppe 9, abgenommen in Aufgabe 10.1). Das ist KEIN
# Test: das Skript druckt nur die Schritte. Ergebnisse gehoeren in testing/lab-brewing/results-*.md. Exit 2,
# damit niemand "gruen" liest. Die Teile ohne Spieler (Chunk entladen, Neustart, Absturz) stehen in
# testing/lab-brewing/proof/recipe.sh. Konsole nur ueber testing/clients/con, nie per tmux send-keys,
# in keinem Fenster stop oder shutdown tippen.
cat <<'TXT'
LAB-ABLAUF - Spieler, Trichter, Upgrades, Konsum

0. Vorbereiten
   Netzwerk mit JDK 25 starten: JAVA_HOME und PATH auf ~/.jdks/openjdk-25*, dann testing/clients/netstart.
   Spieler schuetzen: time set day, gamerule doDaylightCycle false, gamerule doMobSpawning false,
   effect give <spieler> resistance infinite 4 true. Blockfeld vor dem Klick freiraeumen, sonst trifft der
   Klick den falschen Block. Aufbau weit weg: forceload add, Stein als Boden, Staende darauf.
   Im Start-Log lesen, alle muessen stehen:
     Lab: sugar cane reagent recipe added=true, Bukkit reports it=true
     Lab: 9 purifying recipes added
     Lab: 6 cutting recipes added          (N ist 6; eine Warnung dort heisst, die Prädikat-Zutat ging nicht durch)
     Lab: brewing input ... registered      (die Zeilen zu den Eingaben der Filter und Zutaten)
   Jede Zeile genau einmal: kommen Lab brew-Zeilen oder Rezepte doppelt, ist ein init doppelt aufgerufen.

1. Dauer aus der Config
   configs/lab.yml: brew-seconds auf 10 stellen, neu starten. Erwartet: ein Vorgang dauert 10 s, nicht 20,
   der Schluessel bleibt unveraendert in der Datei, fehlende Werte stehen mit Kommentar darin.

2. Drei leere Slots
   # spec: lab-brewing#three-empty-slots
   Braustand, Lohenstaub als Brennstoff, 3+ Zuckerrohr oben, unten nichts. Erwartet: Log "Lab brew start",
   nach der Dauer "finish slots=3", drei Zuckerrohr weniger, jeder Slot Rohpaste, Pflanzenabfall oder leer.
   Fortschrittspfeil: Stand waehrend des Vorgangs oeffnen, der Pfeil laeuft mit.
   # spec: lab-brewing#watching-a-brew

3. Filter herstellen
   # spec: lab-brewing#crafter-makes-filters
   Werkbank: 3 Papier ergeben 8 Papierfilter. Crafter mit 3 Papier im Muster (Redstone) gibt 8 Papierfilter aus.
   # spec: lab-brewing#plain-item-does-not-pass-as-a-filter
   Kohlefilter aus normalem Papier und Holzkohle: kein Rezept. Nur Papierfilter und Holzkohle gehen.
   Ebenso Seidenfilter (Kohlefilter und 4 Faeden), Goldfilter (Seidenfilter und 8 Goldnuggets), Feenstaub
   (Glowstone-Staub und Zucker). Die Rezepte tauchen bei Spielern, die nie ein Laborgegenstand hatten, nicht auf.

4. Der passende Filter
   # spec: lab-brewing#matching-filter
   Papierfilter oben, Rohpaste Reinheit 1 in allen drei Slots. Erwartet: ein Filter verbraucht, jedes Stuek
   wurde Reinheit 2, Abfall oder leer. Der Goldfilter auf Reinheit 1 startet nichts.

5. Abfall
   # spec: lab-brewing#placing-is-refused
   Pflanzenabfall in der Hand auf einen Block setzen. Erwartet: kein Block, der Gegenstand bleibt im Inventar.

6. Normaler Trank und Laborgegenstand im normalen Rezept
   # spec: lab-brewing#normal-potion
   Wasserflaschen mit Netherwarze brauen. Erwartet: braut wie in Vanilla, Dauer 20 s (400 Ticks), Pfeil normal.
   # spec: lab-brewing#lab-item-in-a-normal-recipe
   Einen Papierfilter in ein Rezept legen, das Papier will (Buch, Karte): passt nicht. Einen Kohlefilter im
   Ofen oder als Brennstoff, Feenstaub als Glowstone-Staub: ebenso nicht.
   # spec: lab-brewing#bystander-sees-nothing
   Zweiter Client, nie am Labor: keine Laborzeile im Chat, keine Rezept-Benachrichtigung, Gegenstaende unveraendert.

7. Ganze Linie ohne Spieler
   # spec: lab-brewing#automatic-purification
   Seitlich ein Trichter mit Rohpaste Reinheit 1, oben ein Trichter mit Papierfilter, Lohenstaub vorhanden.
   Erwartet: der Stand braut, ohne dass jemand ihn beruehrt.
   # spec: lab-brewing#input-is-not-pulled-out
   Trichter unter dem Stand, Papierfilter oben, Paste Reinheit 1 unten: die Paste bleibt, bis sie gebraut ist.
   # spec: lab-brewing#results-are-pulled-out
   Nach dem Brauen liegen Rohpaste und Pflanzenabfall unten: der untere Trichter nimmt beides heraus.
   Dann die ganze Linie: Zuckerrohr, vier Filter nacheinander, Feenstaub, Schneiden, Konsum (Schritt 11),
   ohne dass ein Spieler zwischendurch etwas anfasst. Bilanz der Gegenstaende vorher und nachher.

8. Upgrades
   # spec: lab-upgrades#water-cauldron-raises-the-yield
   Wasserkessel (mit Wasser) neben dem Stand, Zuckerrohr brauen. Erwartet: "Lab brew start ... upgrades=1";
   die Chance auf Rohpaste liegt bei 52,8 % (viele Vorgaenge zaehlen, nicht einen).
   # spec: lab-upgrades#empty-cauldron-does-nothing
   Nur ein leerer Kessel: "upgrades=0", Chance unveraendert.
   # spec: lab-upgrades#removing-an-upgrade
   Waehrend eines Vorgangs den Amethystblock abbauen: der laufende Vorgang behaelt die Chance, der naechste
   zeigt upgrades ohne das Bit.
   # spec: lab-upgrades#normal-potion-near-an-upgrade
   Ein Blitzableiter neben dem Stand, Wasserflaschen brauen: sie brauen 20 s, nicht kuerzer.

9. Schneiden und Strecken von Hand und per Trichter
   Kokain Reinheit 3, Goldkarotte oben: Reinheit bleibt 3, Nachtsicht in der Beschreibung. Zucker auf Reinheit
   4: drei gestreckte Kokain Reinheit 3. Honigflasche oben: es bleibt eine Glasflasche im Slot oder im
   Trichter zurueck, keine Honigflasche verschwindet spurlos.
   # spec: lab-cutting#automatic-cutting
   Seitlich ein Trichter mit Kokain, oben ein Trichter mit Goldkarotten: der Stand schneidet ohne Spieler,
   ein Trichter darunter nimmt das Ergebnis.

10. Konsum und Relog
   Kokain per Rechtsklick: Tempo und Eile nach Tabelle, ein Stueck weniger, Pause 60 s.
   # spec: lab-consumption#holding-it-does-nothing
   Kokain nur tragen, nicht benutzen: keine Wirkung, auch nicht bei Umstehenden.
   # spec: lab-consumption#logging-out-does-not-skip-it
   Kokain nehmen, abmelden, nach Ablauf der Wirkung wieder anmelden: der Absturz startet beim Beitritt.
   # spec: lab-consumption#death-cancels-it
   Kokain nehmen, vor dem Absturz sterben (kill): nach dem Respawn kommt kein Absturz.
   # spec: lab-consumption#hit-during-purity-5
   Reinheit 5 nehmen (Tempo III), dann von einem zweiten Spieler schlagen: Tempo faellt auf II.

11. /labor
   # spec: lab-brewing#asking-for-the-tree
   /labor im Chat: der Baum von Zuckerrohr bis Kokain, mit den Zahlen aus configs/lab.yml. Einen Wert in der
   Datei aendern (zum Beispiel yield.papierfilter.goods), neu starten: der Text zeigt den neuen Wert.

12. Aufraeumen
   Op nehmen, Staende und Kisten abbauen, forceload remove, brew-seconds wieder auf 20.
TXT
exit 2
