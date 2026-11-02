---
type: Decision
title: Gadgets sind nie ein Spielvorteil und liegen in einem Slot pro Server
description: "Warum die 18 Gadgets pro Server getrennt ausgewählt werden und warum Bedwars keine Gadgets einschaltet."
status: draft
decision: accepted
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:17:30+01:00 }
sources:
  - id: gadgets
    resource: ../../GADGETS.md
---

# Decision

Ein Gadget darf nie ein Spielvorteil sein. Darum schalten die Bedwars-Server Gadgets gar nicht erst ein, und Endlos-Perle und Enterhaken wirken nur in Lobby und Survival.

Spieler tragen ein Gadget pro Slot statt eines im ganzen Netzwerk. `GadgetSlot` kennt `LOBBY`, `SURVIVAL` und `BEDWARS`; `BEDWARS` bleibt leer und existiert nur noch, damit gespeicherte Auswahlen lesbar bleiben. `PlayerCosmetics` speichert unter `GADGET_<SLOT>`. Eine ältere Auswahl unter `GADGET` gilt in jedem Slot weiter und wird beim ersten Ändern aufgeteilt.

Während eines Parkour-Laufs ist jedes Cosmetic aus, nicht nur das, was hilft (`CosmeticEffects.setOff`).

# Why

Ein Gadget ist eine Regel und kein Bild, und sinnvolle Regeln unterscheiden sich je Server. Mit nur einem Gadget im Netzwerk hätte der Doppelsprung in der Lobby den Erntehelfer auf Survival abgelegt. Eine endlose Perle ist in einer kurzen Runde ein Cosmetic, in einer Welt, in der gebaut wird, aber ein Eingriff in die Wirtschaft.

Noch offen sind die Preise (800 bis 5000 Bits, nicht im Betrieb erprobt) und ein Cooldown für den Erntehelfer. Details stehen in `GADGETS.md`.

# Rejected alternatives

- Ein Gadget netzwerkweit: verworfen, siehe oben.
- Enterhaken auch in Bedwars: lief anfangs, wurde wieder entfernt, weil er ein Spielvorteil war.
- Partikelspur als Gadget: verworfen, denn der Cosmetic-Typ `TRAIL` hat sie schon, sie wäre zweimal verkauft worden.
- Sprungpad und Fußspuren auf beiden Servern: nachträglich auf die Lobby beschränkt.

# Related

- [Cosmetics](/domain/cosmetic.md)
- [Bedwars-Plugin](/architecture/bedwars.md)
