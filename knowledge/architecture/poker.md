---
type: Component
title: Poker-Plugin (PokerPlugin)
description: Casino-Server einer Pokernacht mit Regeln, Tischen, Bots und Bank, gespielt um Bits.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:19:59+01:00 }
sources:
  - id: poker
    resource: ../../PokerPlugin/
---

# Responsibility

Läuft auf dem Casino-Server einer Pokernacht. Enthält Spielregeln (`game`), Bots (`bot`), Oberfläche (`ui`), Welt (`world`) und `bank/Bank`. Gespielt wird um die Bits des Netzwerks; was das Haus von jedem Pot behält, ist eine Einstellung des Events. Anders als Bedwars wertet die Pokernacht aus eigenen Aufzeichnungen der Tische, nicht aus gemeldeten Ergebnissen.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)

# Related

- [Bits](/domain/bits.md)
- [Event](/domain/event.md)
