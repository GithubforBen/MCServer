---
type: Component
title: Lobby-Plugin (LobbyPlugin)
description: Lobby-Server mit Parkour, NPCs, Server Manager und dem Start eigener Runden.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:19:58+01:00 }
sources:
  - id: lobby
    resource: ../../LobbyPlugin/
---

# Responsibility

Läuft auf dem Server `LOBBY`. Enthält Parkour (`parkour`), Lobby-NPCs (`npc`), den Server Manager und den Einstieg in eigene Runden (`rounds`, Befehl `/runde`). Die Lobby-Welt wird bei jedem Start aus `assets/lobby-spawn.zip` wiederhergestellt.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)

# Related

- [Runde](/domain/round.md)
- [Gadget-Entscheidung](/decisions/gadget-choices.md)
