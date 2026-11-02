---
type: Component
title: Bedwars-Plugin (Bedwars)
description: "Bedwars-Minispiel: eine Runde pro Server, mit Phasen, Shop, Upgrades, Endgame und schaltbaren Addons."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:19:59+01:00 }
sources:
  - id: bedwars
    resource: ../../Bedwars/
---

# Responsibility

Trägt genau eine Runde auf einem eigenen Server aus, von der Warte-Lobby bis zum Siegerbildschirm. Der Ablauf liegt in `game/phase`, die Addons in `addon` (Bett-Token, Custom Items, Killstreaks, Kits, Zufallsevents). Die Runde meldet Platzierung und Kills ans Event-System.

# Interfaces

Die Bukkit-Events unter `api` (`BedwarsEvent` und Unterklassen) sind die einzige Andockstelle für Addons.

# Related

- [Bedwars-Grundentscheidungen](/decisions/bedwars-foundations.md)
- [Runde](/domain/round.md)
