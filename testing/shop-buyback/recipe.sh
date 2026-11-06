#!/bin/bash
# Checkliste fuer die Rueckkauf-Funktion (Change shop-buyback). Das ist KEIN Test: das Skript fuehrt nichts
# aus, es druckt die Schritte und worauf man achten muss. Wer sie im Spiel durchgeht, haelt das Ergebnis
# je Schritt fest. Exit 2, damit niemand "gruen" daraus liest.
#
# Voraussetzung: Netzwerk laeuft (testing/clients/netstart), Konto a = Spieler, Konto b = Op, beide im
# Survival-Server, a und b im selben Team "BB" (/cteam create BB BBB, dann einladen).
# Konsolenbefehle nur ueber testing/clients/con, nie tmux send-keys, nie stop/shutdown im Fenster.
# Was BuybackRulesCheck auf Regelebene abdeckt, wird hier im echten Spiel mit Bits und Items gesehen.

cat <<'TXT'
SHOP-BUYBACK - Schritte zum Abgehen (nichts davon wurde automatisch geprueft)

0. Aufbau
   testing/clients/con SURVIVAL "give LongRangeMissile diamond_block 4"
   testing/clients/con SURVIVAL "gamemode survival LongRangeMissile"
   Als b im Team BB auf einer Kiste stehend: /shop create diamant
   Angebot: Diamantblock, Menge 1, Verkaufspreis 900. Zwei Diamantbloecke in die Lagerkiste legen.
   Teamkonto: 5000 Bits (Weg siehe Schritt 7).

1. Neues Angebot startet ohne Rueckkauf
   # spec: shop-buyback#new-offer-starts-without-buyback
   Schleich-Rechtsklick auf den Haendler: der Rueckkauf-Schalter des neuen Angebots steht auf AUS.
   # spec: shop-buyback#offer-without-buyback
   Als a Rechtsklick auf das Angebot im Laden: nichts wird verkauft, Block und Bits von a unveraendert.

2. Rueckkauf einschalten: Preis kommt vom Verkaufspreis
   # spec: shop-buyback#preset-from-the-sale-price
   Als b Schalter auf AN. Erwartet: Rueckkaufpreis 900, Verkaufspreis 900.
   Nur Teammitglieder duerfen schalten: ein Fremder sieht die Einstellungen nicht.

3. Laden zeigt den Rueckkauf
   # spec: shop-buyback#offer-shows-its-buyback
   Als a den Laden oeffnen. Erwartet: am Angebot der Rueckkaufpreis und der Hinweis, dass Rechtsklick verkauft.
   Ein zweites Angebot ohne Rueckkauf zeigt nichts Neues.

4. Team senkt den Rueckkaufpreis
   # spec: shop-buyback#team-lowers-the-price
   Als b Rueckkaufpreis auf 700 stellen, Verkaufspreis bleibt 900.
   Verkaufspreis kurz auf 1000 und zurueck auf 900: der Rueckkaufpreis bleibt 700.

5. Verkauf: Bits und Items bewegen sich
   Vorher merken: Bits von a, Teamkonto, Bloecke bei a und in der Lagerkiste.
   # spec: shop-buyback#selling-a-diamond-block
   Als a Rechtsklick auf das Angebot. Erwartet: a minus 1 Block und plus 700 Bits, Lagerkiste plus 1 Block,
   Teamkonto minus 700 Bits, Summe aller Bits gleich. Linksklick kauft weiter (900 Bits, 1 Block raus).
   # spec: shop-buyback#member-of-the-owning-team-sells
   Dasselbe als b (Mitglied des Teams): geht durch wie bei a.

6. Gleich aussehende Ware
   # spec: shop-buyback#item-only-looks-the-same
   a bekommt einen umbenannten und einen verzauberten Diamantblock, jeweils Rechtsklick.
   Erwartet: abgelehnt mit Meldung, Item bleibt bei a, keine Bits bewegt.
   Gegenprobe: ein normaler Diamantblock geht durch.

7. Team kann nicht zahlen
   # spec: shop-buyback#team-cannot-pay
   Fuer das Teamkonto gibt es keinen Konsolenbefehl (/admin money gilt nur fuer Spieler). Weg:
     testing/clients/netstop
     money.yml beim Launcher: unter balances.BB den Wert setzen (hier 500, weniger als 700)
     testing/clients/netstart
   Als a verkaufen. Erwartet: abgelehnt, Meldung nennt das Teamkonto, Block bei a, kein Bit bewegt.
   Danach genauso zurueck auf 5000.

8. Kiste voll
   # spec: shop-buyback#chest-is-full
   Lagerkiste mit anderem Zeug fuellen (alle 27 Plaetze, z.B. Stein 64 per Hand oder per
   testing/clients/con SURVIVAL "item replace block <x> <y> <z> container.N with stone 64").
   Als a verkaufen. Erwartet: abgelehnt, Meldung zur vollen Kiste, Block bei a, kein Bit bewegt.

9. Ausverkauftes Angebot kauft weiter
   # spec: shop-buyback#sold-out-offer-still-buys
   Kiste leeren, bis kein Diamantblock mehr drin ist. Der Laden zeigt "ausverkauft".
   Rechtsklick von a verkauft trotzdem (Block in die Kiste, Bits wie in Schritt 5).

10. Host antwortet nicht
   # spec: shop-buyback#host-does-not-answer
   SCHRITT FUER DEN ORCHESTRATOR, nicht ungefragt ausfuehren. Der Launcher-Prozess wird nur kurz
   eingefroren, nicht beendet:
     L=$(pgrep -f '^java -jar ServerLauncherApplication' | head -1)
     kill -STOP $L
     # als a sofort Rechtsklick auf das Angebot, auf die Fehlermeldung warten
     kill -CONT $L        # spaetestens nach etwa 10 Sekunden, sonst haengt der Cluster
   Erwartet: a bekommt den Block zurueck, keine Bits bewegt, Teamkonto gleich. Danach geht ein Verkauf normal.
   Nie kill -9, den Launcher nie im Fenster stoppen.

11. Zwei Verkaeufer, ein Konto
   # spec: shop-buyback#two-sellers-against-one-account
   Teamkonto auf genau 700 (Weg wie Schritt 7, balances.BB: 700), Lagerkiste mit Platz, Rueckkaufpreis 700.
   a und b stehen am Laden, beide mit einem Block, beide klicken Rechtsklick moeglichst im selben Moment
   (zwei Terminals, je ein testing/clients/clk).
   Erwartet: genau ein Verkauf gelingt (Teamkonto 0), der andere behaelt seinen Block, Summe der Bits stimmt.

12. Marktplatz
   Zweiter Shop eines anderen Teams mit demselben Item, Rueckkauf 800 (Shop 1 zahlt 700).
   # spec: shop-buyback#listing-shows-the-best-buyer
   /shop: der Diamantblock zeigt 800 Bits Rueckkauf je Item als besten Preis.
   # spec: shop-buyback#selling-from-the-offer-list
   Eintrag oeffnen: die Angebotsliste zeigt den Rueckkaufpreis je Angebot. Rechtsklick auf Shop 1 verkauft
   einen Block dorthin (700 Bits); Linksklick kauft weiter.

13. Neustart: Einstellung bleibt
   # spec: shop-buyback#setting-survives-a-restart
   Rueckkauf an, Preis 700. testing/clients/netstop, dann testing/clients/netstart. Als a Angebot ansehen
   und verkaufen. Erwartet: Rueckkauf noch an, Preis 700, Verkauf klappt. (Das ist auch der Beweis, dass
   shop-config.yml mit den neuen Feldern geladen wird.)

14. Alter Shop bleibt reiner Verkauf
   # spec: shop-buyback#existing-offer-stays-sell-only
   Netzwerk aus (netstop). In servers/SURVIVAL/configs/shop-config.yml bei einem Angebot die Rueckkauf-Felder
   von Hand loeschen (Stand wie vor dem Update). netstart. Erwartet: das Angebot verkauft wie vorher,
   der Schalter steht auf AUS, Rechtsklick verkauft nichts, kein Fehler beim Laden im Log.

Aufraeumen: Shops loeschen, Teamkonto zuruecksetzen, Items einsammeln, kein Op auf b lassen.
TXT
exit 2
