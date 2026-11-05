#!/bin/bash
# Checkliste fuer Haendler-Skins (Change shop-skin). Das ist KEIN Test: das Skript fuehrt nichts aus,
# es druckt die Schritte und worauf man achten muss. Wer sie im Spiel durchgeht, haelt das Ergebnis je
# Schritt fest. Exit 2, damit niemand "gruen" daraus liest.
#
# Voraussetzung: Netzwerk laeuft (testing/clients/netstart, braucht JAVA_HOME und PATH auf JDK 25),
# Konto a = Spieler im Team "SK", Konto b = Spieler ohne Team, beide im Survival-Server.
# Konsolenbefehle nur ueber testing/clients/con, nie tmux send-keys, nie stop/shutdown im Fenster.
# SkinNameCheck deckt die Namensregel als reinen Code ab; hier wird die Figur im echten Spiel gesehen.

cat <<'TXT'
SHOP-SKIN - Schritte zum Abgehen (nichts davon wurde automatisch geprueft)

0. Aufbau
   Spieler schuetzen: Tag, kein Mob-Spawn, Resistenz.
     testing/clients/con SURVIVAL "time set day"
     testing/clients/con SURVIVAL "gamerule doMobSpawning false"
     testing/clients/con SURVIVAL "effect give <a> resistance infinite 4 true"
   Als a im Team SK auf einer Kiste stehend: /shop create skin
   Bloecke rund um den Haendler wegraeumen, sonst trifft der Klick den Block statt der Figur
   (ein Rechtsklick mit Block in der Hand setzt ihn, wenn die Figur verfehlt wird).
   Figuren zaehlen (Erwartung steht bei jedem Schritt):
     testing/clients/con SURVIVAL "execute as <a> at @s if entity @e[type=mannequin,distance=..6]"
     testing/clients/con SURVIVAL "execute as <a> at @s if entity @e[type=villager,distance=..6]"
   Die Antwort nennt nur, ob es eine gibt. Fuer die genaue Zahl:
     testing/clients/con SURVIVAL "execute as <a> at @s store result score #n tmp if entity @e[type=mannequin,distance=..6]"
   (vorher "scoreboard objectives add tmp dummy"), oder die Figuren mit "tp" an einen Ort ziehen und
   zaehlen. Wichtig ist: 1 heisst eine, nicht zwei uebereinander.

1. Haendler ohne Skin bleibt Villager
   # spec: shop-skin#existing-shop
   Neuer Haendler (und, falls vorhanden, ein Haendler aus einer alten shop-config.yml ohne Skin-Feld):
   Erwartet: 1 Villager, 0 Mannequins, kein Fehler beim Laden im Log.

2. Fremder kommt nicht an die Einstellungen
   # spec: shop-skin#stranger-cannot-reach-it
   Als b (kein Teammitglied) Schleich-Rechtsklick auf den Haendler. Erwartet: der Editor oeffnet sich nicht.

3. Skin setzen
   # spec: shop-skin#setting-a-skin
   Als a Schleich-Rechtsklick, Spielerkopf-Knopf (Skin) im Einstellungsmenue, im Chat "Notch" eingeben.
   Erwartet: der Haendler steht als Spielerfigur mit Notchs Skin da, 1 Mannequin, 0 Villager.
   Name ueber dem Kopf und Platz unveraendert.

4. Ungueltiger Name
   # spec: shop-skin#invalid-name
   Knopf erneut, "not a name!" eingeben. Erwartet: Meldung, der Name sei ungueltig; Skin bleibt Notch.
   Gleiches mit "ab" (zu kurz) und einem 17-Zeichen-Namen.

5. Abbrechen
   # spec: shop-skin#cancelling
   Knopf, "abbrechen" eingeben. Erwartet: nichts aendert sich, Skin bleibt Notch, 1 Mannequin.

6. Skinned Haendler verhaelt sich wie jeder Laden
   # spec: shop-skin#customer-clicks-a-skinned-shop
   Als b Rechtsklick auf die Figur: das Ladenmenue oeffnet sich.
   # spec: shop-skin#owner-sneak-clicks-a-skinned-shop
   Als a Schleich-Rechtsklick auf die Figur: der Editor oeffnet sich.
   # spec: shop-skin#skinned-shop-cannot-be-hurt
   Als a (Survival) mehrfach auf die Figur schlagen. Erwartet: kein Schaden, die Figur bleibt,
   steht still und wird nicht weggeschoben.

7. Chunk neu laden
   # spec: shop-skin#chunk-reload
   a und b weit wegteleportieren (ueber die Simulationsdistanz, z.B. 500 Bloecke), 30 Sekunden
   warten, zurueck. Erwartet: genau 1 Mannequin mit Notchs Skin, 0 Villager.
   Zweimal wiederholen: die Zahl bleibt 1.

8. Haendler versetzen
   # spec: shop-skin#moving-a-skinned-shop
   Als a im Editor "Haendler versetzen", neuen Ort waehlen. Erwartet: am neuen Ort 1 Mannequin, am alten
   Ort 0 Figuren (Zaehlbefehl an beiden Stellen). Danach noch einmal Chunk neu laden wie Schritt 7.

9. Skin entfernen, Chunk spaeter neu laden
   # spec: shop-skin#removing-the-skin
   Als a Knopf, "aus" eingeben. Erwartet: der Haendler steht wieder als Villager da, 1 Villager, 0 Mannequins.
   # spec: shop-skin#skin-removed-while-the-chunk-reloads-later
   Der Chunk muss beim Aendern des Skins entladen sein, sonst ist nichts bewiesen. Ein Spieler kann den
   Skin nur aus der Naehe aendern, daher dieser Weg:
     a setzt wieder einen Skin (Schritt 3), dann testing/clients/netstop,
     in servers/SURVIVAL/configs/shop-config.yml das Skin-Feld des Haendlers von Hand loeschen,
     testing/clients/netstart, a bleibt fern und geht erst danach zum Haendler zurueck.
   Erwartet nach der Rueckkehr: genau 1 Villager, 0 Mannequins (kein uebrig gebliebenes Mannequin).
   Wer den Chunk ohne Neustart entladen will, muss das der Orchestrator entscheiden lassen.

10. Neustart
   # spec: shop-skin#restart
   Skin wieder auf Notch setzen. testing/clients/netstop, dann testing/clients/netstart (JDK 25).
   Erwartet: genau 1 Mannequin mit Notchs Skin, 0 Villager. Danach zweimal Chunk neu laden, die Zahl bleibt 1.
   Im Log keine Fehler beim Laden von shop-config.yml.

Aufraeumen: Shops loeschen, gamerule doMobSpawning true, Effekte entfernen, Items einsammeln, kein Op auf b lassen.
TXT
exit 2
