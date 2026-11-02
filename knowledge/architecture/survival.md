---
type: Component
title: Survival-Plugin (Survival)
description: Survival-Spielmodus mit Teams, Shops, Marktplatz, Chunk Limiter und angepassten Paper-Einstellungen.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:19:59+01:00 }
sources:
  - id: survival
    resource: ../../Survival/
---

# Responsibility

Der Survival-Spielmodus. Unter `featrues/` (Schreibweise wie im Code) liegen Shopkeeper und Marktplatz, Teams, Chunk Limiter, Geld, Admin-Funktionen und der End-Kampf. `MoneyHandler` antwortet aus der lokalen Kopie; das Geld selbst gehört dem Launcher.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)

# Related

- [Bits](/domain/bits.md)
- [Rucksack-Plugin](/architecture/backpack.md)
