---
type: Term
title: Event
description: "Eine angekündigte Veranstaltung im Netzwerk mit Typ, Zeitraum, Einstellungen und Belohnungen."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:02+01:00 }
sources:
  - id: data
    resource: ../../CommonCode/src/main/java/de/hems/types/event/EventData.java
  - id: type
    resource: ../../CommonCode/src/main/java/de/hems/types/event/EventType.java
---

# Meaning

Eine Veranstaltung, die Admins unter `/events` anlegen (`EventData`). `EventType` bestimmt die Regeln: reine Ankündigung, andere Welt, das End öffnen, UHC-Speedrun, Bedwars, Pokernacht oder Hunger Games. Einstellungen und Belohnungen sitzen bei jedem Typ an derselben Stelle.

# Not to be confused with

- Netzwerk-Events: Nachrichten im Cluster (`de.hems.communication.events`), nichts, woran Spieler teilnehmen.
- Bukkit-Events: Ereignisse innerhalb eines Servers, etwa die `Bedwars…Event`-Klassen.

# Related

- [Lauf](/domain/run.md)
- [Netzwerk-Events](/architecture/network-events.md)
- [Hunger-Games-Plugin](/architecture/hunger-games.md)
