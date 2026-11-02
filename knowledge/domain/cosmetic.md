---
type: Term
title: Cosmetic
description: "Etwas Kaufbares, das Aussehen oder Spielgefühl ändert, darunter Gadgets und Partikelspuren."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:03+01:00 }
sources:
  - id: types
    resource: ../../CommonCode/src/main/java/de/hems/types/cosmetic/
  - id: readme
    resource: ../../README.md
  - id: gadgets
    resource: ../../GADGETS.md
---

# Meaning

Etwas, das Spieler für Bits kaufen und tragen. Partikelspuren sind der Cosmetic-Typ `TRAIL`. Gadgets sind die eine Art Cosmetic, die eine Regel ist und kein Bild, und sie liegen je Server in einem eigenen `GadgetSlot`. Die Auswahl liegt beim Launcher in `cosmetics.yml`.

# Not to be confused with

- Gadget: nur eine Unterart. Nicht jedes Cosmetic hat einen Slot.

# Related

- [Gadget-Entscheidung](/decisions/gadget-choices.md)
- [Bits](/domain/bits.md)
