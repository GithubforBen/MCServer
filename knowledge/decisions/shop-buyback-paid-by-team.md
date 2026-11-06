---
type: Decision
title: "Rückkauf zahlt die Teamkasse über den bestätigten Weg"
description: "Rückkauf zahlt die Teamkasse über den bestätigten Weg, auch das eigene Team darf verkaufen."
status: draft
decision: accepted
change: 2026-11-06-shop-buyback
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:19:23+01:00 }
sources:
  - id: brief
    resource: ../../openspec/changes/archive/2026-11-06-shop-buyback/brief.md
  - id: design
    resource: ../../openspec/changes/archive/2026-11-06-shop-buyback/design.md
  - id: money
    resource: ../../CommonCode/src/main/java/com/benschnorr/paper/money/MoneyService.java
  - id: events
    resource: ../../ServerLauncherApplication/src/main/java/com/benschnorr/events/MoneyEvents.java
---

# Decision

Beim Rückkauf zahlt die Teamkasse, und zwar über den bestätigten Weg `MoneyService.changeAsync` mit Deckungsprüfung: der Launcher entscheidet, die Antwort kommt auf dem Hauptthread zurück. Die Ware landet erst danach in der Kiste. Auch Mitglieder des besitzenden Teams dürfen an den eigenen Shop verkaufen.

Eine Abbuchung trägt eine Frist (`ChangeBalanceEvent.deadline`). `changeBlocking` setzt sie eine Sekunde vor das Ende der 5 Sekunden Wartezeit, und `MoneyEvents.onChange` beantwortet eine abgelaufene Anfrage als gescheitert, ohne den Kontostand anzufassen. Gesendete Änderungen ohne Frist (`MoneyService.change`) bleiben, wie sie waren; auch die Erstattung bei voller Kiste läuft so. Die Frist gilt auch für die `Bank` des Poker-Plugins.

# Why

Wertvolle Ware ist der Sinn des Rückkaufs, und zwei Verkäufer gegen ein Konto dürfen nicht beide ein Ja bekommen. Die lokale Kopie reicht dafür nicht. Eine verspätete Abbuchung würde sonst Bits bewegen, obwohl der Verkäufer die Ware längst zurückhat.

Akzeptiertes Risiko (Brief Q5): ein einzelnes Teammitglied kann das Teamkonto leeren, indem es den Rückkaufpreis hochsetzt und selbst verkauft. Das hat der Nutzer so entschieden.

Bekannte Lücken: Wird eine Abbuchung kurz vor der Frist angewandt und die Antwort geht verloren, verliert das Team Bits und der Verkäufer hat die Ware ohne Bezahlung zurück. Die Frist verkleinert die Lücke auf die Laufzeit der Antwort, schließt sie nicht. Stoppt ein Server, während ein Verkauf wartet, kann das reservierte Los verloren gehen.

# Rejected alternatives

- Unbestätigte Zahlung mit `MoneyHandler.removeMoney`: abgelehnt, weil zwei Verkäufer beide ein Ja bekommen könnten.
- Ware reserviert halten und später abrechnen: abgelehnt, viel mehr Aufwand und für Spieler verwirrend.
- Mitglieder des eigenen Teams vom Verkauf ausschließen: abgelehnt, sie verkaufen wie alle anderen.
- Ein NPC-Käufer, der neue Bits erzeugt: außerhalb dieser Änderung, siehe [Backlog](/backlog/hehler-npc-der-kokain-gegen-neue-bits-ankauft.md).

# Related

- [Shop-Rückkauf](/capabilities/shop-buyback.md)
- [Bits](/domain/bits.md)
- [Launcher](/architecture/launcher.md)
