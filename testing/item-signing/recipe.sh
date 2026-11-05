#!/bin/bash
# Checkliste fuer das Signieren von Items (Change item-signing). Das ist KEIN Test: das Skript fuehrt nichts
# aus, es druckt die Schritte und worauf man achten muss. Wer sie im Spiel durchgeht, haelt das Ergebnis
# je Schritt fest. Exit 2, damit niemand "gruen" daraus liest.
#
# Voraussetzung: Netzwerk laeuft (testing/clients/netstart, JDK 25 in JAVA_HOME und PATH), Konto a = Spieler,
# Konto b = zweiter Spieler und Op, beide im Survival-Server, a und b im selben Team "SG" (/cteam create SG SGG,
# dann einladen). Spieler schuetzen: Tag, doMobSpawning false, Resistenz. Bloecke um Haendler, Werktisch und
# Amboss wegraeumen, sonst setzt der Klick einen Block. Konsolenbefehle nur ueber testing/clients/con, nie
# tmux send-keys, nie stop/shutdown im Fenster.
# Was SigningRulesCheck auf Regelebene abdeckt, wird hier im echten Spiel mit Items und Bloecken gesehen.

cat <<'TXT'
ITEM-SIGNING - Schritte zum Abgehen (nichts davon wurde automatisch geprueft)

0. Aufbau
   testing/clients/con SURVIVAL "time set day"
   testing/clients/con SURVIVAL "gamerule doMobSpawning false"
   testing/clients/con SURVIVAL "effect give LongRangeMissile resistance infinite 4 true"   (dasselbe fuer b)
   testing/clients/con SURVIVAL "gamemode survival LongRangeMissile"
   a bekommt per "give": 32 Cobblestone, 10 weitere Cobblestone (einzeln ablegen), 4 Birkenplanken,
   4 Eisenerz, 2 Kohle, ein Eisenschwert, ein Buch, ein Gadget aus dem Cosmetics-Menue.

1. Stapel signieren
   # spec: item-signing#signing-a-stack
   a haelt 32 Cobblestone, /signItem. Erwartet: Meldung "32 Items signiert", alle 32 tragen die Beschreibung
   "Signiert von <Name von a>", kostenlos (keine Bits abgezogen).

2. Leere Hand
   a mit leerer Hand, /signItem. Erwartet: Hinweis, ein Item in die Hand zu nehmen, nichts aendert sich.

3. Zehnmal signieren, ein Stapel
   # spec: item-signing#signed-ten-times
   a nimmt zehn einzelne Cobblestone nacheinander in die Hand und signiert jeden mit /signItem, dann
   zusammenlegen. Erwartet: sie stapeln sich zu einem Stapel von 10, auch mit dem Stapel aus Schritt 1
   (gleiche Beschreibung, gleicher Stapel).

4. Zweimal signieren
   /signItem auf bereits signierte Items. Erwartet: Meldung, dass sie schon signiert sind, Items unveraendert.

5. Fremde Signatur und Gadget
   a gibt b einen signierten Stein (Drop oder Truhe), b macht /signItem damit. Erwartet: a's Signatur
   bleibt, b erfaehrt, wer es signiert hat. Gadget aus dem Cosmetics-Menue: /signItem lehnt ab, Hinweis,
   dass es zum Netzwerk gehoert.

6. Werktisch und Schmiedetisch
   # spec: item-signing#crafting-table
   a legt signierte Birkenplanken in einen Werktisch (Stock oder Brett). Erwartet: kein Ergebnis. Mit
   unsignierten Planken erscheint das Ergebnis (Gegenprobe). Am Schmiedetisch gibt ein signiertes Item
   ebenfalls kein Ergebnis.

7. Crafter
   # spec: item-signing#crafter-skips-signed-items
   Crafter mit Redstone-Signal, signierte Planken in den Slots eines Stock-Rezepts. Erwartet: beim Signal
   wird nichts gefertigt, die Planken liegen noch signiert im Crafter. Gegenprobe mit unsignierten
   Planken fertigt.

8. Ofen per Trichter
   # spec: item-signing#furnace
   Trichter ueber einem Ofen mit Kohle als Brennstoff, signiertes Eisenerz in einer Kiste darueber.
   Erwartet: das Erz wird nicht geschmolzen, der Ofen zuendet nicht, es wird keine Kohle verbraucht
   (zaehlen). Dasselbe im Schmelzofen und im Raeucherofen. Gegenprobe mit unsigniertem Erz schmilzt.

9. Amboss links und rechts
   # spec: item-signing#anvil-left-and-right
   Signiertes Schwert links, Buch (Verzauberung) rechts: das Ergebnis erscheint und behaelt die Signatur
   (Beschreibung "Signiert von"). Dann ein signiertes Item rechts (z.B. signierter Eisenbarren, links
   ein Eisenschwert): kein Ergebnis.

10. Laborbrau mit signierter Zutat
   Braustand wie in testing/lab-brewing/recipe.sh: Kokain unten und eine signierte Goldkarotte oben.
   Erwartet: der Schneide-Vorgang startet nicht. Dasselbe mit signiertem Zuckerrohr oben (kein Vorgang).
   Gegenprobe mit unsigniertem Zuckerrohr startet. (Dazu steht kein Szenario in der Spec, es ist die
   Folge der Anforderung "nie verbraucht".)

11. Shop-Aufbau
   Als b im Team SG auf einer Kiste stehend: /shop create sig. Ein Angebot: Cobblestone, von b signiert
   (b signiert ein Stack und stellt es ueber das Einstellmenue als Ware ein), Verkaufspreis 10, Rueckkauf
   an, Rueckkaufpreis 5. 20 von b's signiertem Cobblestone in die Lagerkiste legen.

12. Verkauf signierter Ware
   # spec: item-signing#selling-signed-goods
   Als a Linksklick auf das Angebot (Bits genug). Erwartet: a bekommt Cobblestone mit b's Signatur, die
   Bits gehen ans Teamkonto, die Kiste verliert einen Stein.

13. Unsigniert ist nicht signiert
   # spec: item-signing#unsigned-is-not-signed
   Die Lagerkiste leeren und nur unsigniertes Cobblestone hineinlegen. Erwartet: das Angebot zeigt
   "ausverkauft", Kauf geht nicht.

14. Rueckkauf signierter Ware
   # spec: item-signing#buyback-of-signed-goods
   Als a Rechtsklick auf das Angebot mit einem von b signierten Stein: geht durch (Bits fuer a, Stein
   in der Kiste). Dann Rechtsklick mit einem von a signierten Stein und mit einem unsignierten Stein:
   beide abgelehnt, Item bleibt beim Spieler, kein Bit bewegt.

Aufraeumen: Shop loeschen, Items einsammeln, Effekte weg, doMobSpawning wieder true, kein Op auf b lassen.
TXT
exit 2
