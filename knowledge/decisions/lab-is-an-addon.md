---
type: Decision
title: Das Labor bleibt ein Zusatz
description: Das Labor bleibt ein vanilla-naher Zusatz auf Survival; Razzien und neue Bits wurden verworfen.
status: draft
decision: accepted
change: 2026-11-06-lab-brewing
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:48:32+01:00 }
sources:
  - id: readme
    resource: ../../README.md
  - id: settings
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/LabSettings.java
  - id: command
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/LabCommand.java
---

# Kontext

Der Wunsch war ein Schedule-1-artiges System mit Kokain, aber es läuft auf Survival, muss vanilla genug bleiben und ein Zusatz sein, kein neuer Spielmodus. Wer nicht interagieren will, soll nicht beeinträchtigt werden.

# Entscheidung

Das Labor ist ein Zusatz nur für Survival (`featrues/lab/`). Laborgegenstände ersetzen keine normalen Zutaten, der normale Braustand arbeitet wie in Vanilla, und Spieler, die das Labor nie benutzen, bekommen keine Meldungen oder Rezepthinweise. Einzige Spur ist `/labor` (`LabCommand`). Die Zahlen liegen in `configs/lab.yml` (`LabSettings`). Bits entstehen nicht: Verkauft wird nur über Spieler-Shops, es gibt keinen NPC-Käufer. Verbesserungen sind Schneiden, Konsum mit Effekten und Upgrades aus Vanilla-Blöcken.

# Alternativen

- Razzien oder Unfälle als Gegengewicht: verworfen, das Labor soll lustig sein und niemanden strafen.
- Ein NPC kauft Kokain und erzeugt neue Bits: verworfen, es soll keine neuen Bits geben.
- Eigener Spielmodus oder eigene Welt: verworfen, es bleibt auf Survival.
- Eigene Blöcke und Items per Resource Pack: verworfen, Laborgegenstände nutzen Basismaterialien mit `item_model`.

# Folgen

Alles Neue braucht eine Abgrenzung nach außen, etwa Abfall, der sich nicht platzieren lässt, und Töpfe, die keine Laborgegenstände annehmen. Der Kampfdeckel beim Konsum schützt Spieler, die nichts mit dem Labor zu tun haben.

# Verwandt

- [Labor: Brauen](/capabilities/lab-brewing.md)
- [Labor: Konsum](/capabilities/lab-consumption.md)
- [Survival-Plugin](/architecture/survival.md)
