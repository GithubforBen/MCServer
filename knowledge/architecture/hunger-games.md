---
type: Component
title: Hunger-Games-Plugin (HungerGamesPlugin)
description: "Arena eines Hunger-Games-Events mit Füllhorn, Supply Drops, schrumpfender Grenze und Wertung."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:00+01:00 }
sources:
  - id: hunger-games
    resource: ../../HungerGamesPlugin/
---

# Responsibility

Führt ein Hunger-Games-Event auf dem Arena-Server `HUNGER_GAMES_<id>` aus: Warten auf Zeit und Mindestzahl, Countdown, Schutzzeit, freies Spiel, schrumpfende Weltgrenze und Showdown. Wer stirbt oder geht, schaut zu. Der Letzte gewinnt, Ergebnisse gehen ans Event-System.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)

# Related

- [Event](/domain/event.md)
