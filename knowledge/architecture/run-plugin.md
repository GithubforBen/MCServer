---
type: Component
title: Speedrun-Plugin (RunPlugin)
description: "Macht aus einem leeren Paper-Server den Server für einen Speedrun-Versuch: Bosse, Hardcore, Zeit und /reset."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:00+01:00 }
sources:
  - id: run
    resource: ../../RunPlugin/
---

# Responsibility

Läuft auf jedem Run-Server. Es beobachtet die Bosse, setzt Hardcore durch, meldet die Zeit an den Launcher und bietet `/reset` und `/abbrechen`. `RunTracker` zählt die Zeit in Ticks und nur, solange jemand spielt. Läuft ein Lauf noch, darf sich der Server nach zehn Minuten ohne Spieler selbst stoppen, und ein neuer Start setzt denselben Lauf fort. Ist der Lauf vorbei, geht der Server, sobald der letzte Spieler weg ist.

Die Modultabelle im README führt dieses Plugin noch nicht auf.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)

# Related

- [Lauf](/domain/run.md)
- [Ghost-Server](/domain/ghost-server.md)
