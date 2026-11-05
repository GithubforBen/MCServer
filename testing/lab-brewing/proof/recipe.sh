#!/bin/bash
# Beweis fuer das Brauen auf leeren Slots (Change lab-brewing, Gruppe 2). Das ist KEIN Test: das Skript
# druckt nur die Schritte. Ergebnisse stehen in results-*.md daneben. Exit 2, damit niemand "gruen" liest.
# Keine Clients noetig, ausser fuer Schritt 8. Konsole nur ueber testing/clients/con.
cat <<'TXT'
LAB-BEWEIS - leere Slots, ohne Spieler

0. Netzwerk mit JDK 25: JAVA_HOME und PATH auf ~/.jdks/openjdk-25*, dann testing/clients/netstart.
   Im Log: "Lab: sugar cane reagent recipe added=true, Bukkit reports it=true".
   Aufbau bei 2000/2000: forceload add 2000 2000; Stein 2000 200 2000, Braustand 2000 201 2000,
   Hopper darueber (facing=down), Lohenstaub in container.4.

1. Zuckerrohr per Hopper
   # spec: lab-brewing#sugar-cane-through-a-hopper
   Zuckerrohr in den oberen Hopper. Erwartet: es landet im Zutatenslot, Log "Lab brew start".

2. Entladen mitten im Vorgang
   # spec: lab-brewing#chunk-unloads-mid-brew
   Nach ~15 s: forceload remove 2000 2000, warten, forceload add. Erwartet: "pause ... progress=N",
   dann "resume ... progress=N" mit demselben N, danach "finish".

3. Neustart mitten im Vorgang
   # spec: lab-brewing#restart-mid-brew
   netstop mitten im Vorgang, netstart. Erwartet: ein "start ... progress=N" nahe dem alten Stand,
   genau ein "finish" fuer diesen Vorgang.

4. Absturz
   # spec: lab-brewing#crash-mid-brew
   Bilanz vorher: Zuckerrohr in Stand und Hopper plus Ergebnisse in der Kiste. kill -9 nur auf den
   SURVIVAL-Java-Prozess (pgrep -P <pane pid> java), dann netstop und netstart.
   Erwartet: die Bilanz bleibt bei jedem Messpunkt gleich.

5. Zutat entnehmen
   # spec: lab-brewing#ingredient-taken-out-mid-brew
   Zutatenslot und Hopper leeren. Erwartet: "reset ... ingredient gone", mit neuer Zutat Neustart bei 0.

6. Abbauen
   # spec: lab-brewing#stand-broken-mid-brew
   setblock 2000 201 2000 air destroy. Erwartet: Stand und Inhalt fallen heraus, kein "finish" danach.

7. Viele Staende
   # spec: lab-brewing#many-stands-reloading
   8 Staende im selben Chunk, Chunk dreimal entladen und laden. Erwartet je Stand: 1 start, 3 pause,
   3 resume, 1 finish, 3 Rohr verbraucht.

8. Fortschrittspfeil (braucht einen Client)
   # spec: lab-brewing#watching-a-brew
   Stand im Spiel oeffnen waehrend des Vorgangs. Erwartet: der Pfeil passt zum Fortschritt.
TXT
exit 2
