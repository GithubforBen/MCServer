---
type: Capability
title: "Labor: Brauen"
description: "Warum es eine lange, automatisierbare Produktionskette im Braustand gibt und wie sie Survival unberührt lässt."
status: draft
spec: openspec/specs/lab-brewing/spec.md
resource: ../../openspec/specs/lab-brewing/spec.md
change: 2026-11-06-lab-brewing
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:48:32+01:00 }
sources:
  - id: brewing
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/lab/brew/LabBrewing.java
  - id: chain
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/lab/chain/LabChain.java
  - id: settings
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/lab/LabSettings.java
  - id: results
    resource: ../../testing/lab-brewing/results-2026-11-06.md
  - id: proof
    resource: ../../testing/lab-brewing/proof/results-2026-11-05.md
  - id: readme
    resource: ../../README.md
  - id: proposal-2026-11-06-lab-brewing
    resource: ../../openspec/changes/archive/2026-11-06-lab-brewing/proposal.md
  - id: design-2026-11-06-lab-brewing
    resource: ../../openspec/changes/archive/2026-11-06-lab-brewing/design.md
---

# Warum

Der Wunsch war ein spielerisches System wie in Schedule 1: Kokain herstellen soll richtig schwierig sein und richtig lange dauern. Grundstoff ist Zuckerrohr, hergestellt wird im Braustand, und es muss automatisch machbar sein, nicht nur per Hand. Es darf sich nicht an der Realität orientieren und soll lustig bleiben. Wer nicht mitmacht, merkt nichts davon: der normale Braustand arbeitet wie in Vanilla. Siehe [Das Labor bleibt ein Zusatz](/decisions/lab-is-an-addon.md).

# Wie es funktioniert

Zuckerrohr kommt oben in den Braustand, die drei unteren Slots bleiben leer. Jeder freie Slot würfelt für sich: Rohpaste, Pflanzenabfall oder nichts. Paste von Reinheit 1 geht mit dem passenden Filter (Papier, Kohle, Seide, Gold) eine Stufe hoch, Feenstaub macht aus ihr Kokain gleicher Reinheit. Je reiner, desto geringer die Ausbeute. Die Stufen stehen in [Reinheit](/domain/purity.md), die Technik in [Eigener Tick für Zuckerrohr](/decisions/lab-brewing-engine.md).

Die Rechnung der Läufe: Für ein Kokain von Reinheit 5 braucht ein Labor ohne Upgrades im Mittel rund 20.000 Zuckerrohr (simuliert 19.739). Voll ausgebaut sind es rund 6.500 (simuliert 6.516; die erste Schätzung lag bei 6.400, weil der Kokain-Schritt bei 100 Prozent gedeckelt ist). Die Zahlen kommen aus der Kette in `LabChain`, die Werte stehen in `configs/lab.yml` (`LabSettings`).

Der Fortschritt steckt im Braustand selbst. Im Spiel überstand er Chunk-Entladen, Neustart und `kill -9` ohne Verlust an Items; das belegt der Lauf vom 2026-11-05.

Trichter: Oben nehmen Zutaten, Filter und Feenstaub, seitlich Paste und Brennstoff, ein Trichter unten holt Ergebnisse und Abfall, aber nichts, was oben noch ein gültiger Schritt wäre. Liegt Paste oder Kokain unten, bevor Filter oder Zutat oben sind, holt der untere Trichter es unverändert heraus. Das bleibt so, es war die Entscheidung des Nutzers. Wer eine Linie ganz ohne Spieler will, baut sich selbst einen Itemfilter aus Redstone, der den Stand in der richtigen Reihenfolge füttert (README, Abschnitt Labor).

`/labor` (`LabCommand`) zeigt den Baum im Chat. Jede Zahl darin kommt aus den Einstellungen, der Text folgt also `configs/lab.yml`. Jeder Laborgegenstand nennt außerdem seine Reinheit und seinen nächsten Schritt.

Aussehen (`LabMaterials`): Laborgegenstände sind Töpferscherben mit `item_model`, weil Scherben sonst keinen Nutzen haben und sich nicht platzieren lassen. Paste sieht aus wie Lehm, Kokain wie Zucker, die Filter wie Papier, Holzkohle, Faden und Goldklumpen, Feenstaub wie Glowstonestaub, Pflanzenabfall wie ein toter Busch.

Bits entstehen dadurch nicht; verkaufen lässt sich nur über Spieler-Shops.

# Verwandt

- [Labor: Upgrades](/capabilities/lab-upgrades.md)
- [Labor: Schneiden und Strecken](/capabilities/lab-cutting.md)
- [Labor: Konsum](/capabilities/lab-consumption.md)
- [Reinheit](/domain/purity.md)
- [Eigener Tick für Zuckerrohr](/decisions/lab-brewing-engine.md)
- [Das Labor bleibt ein Zusatz](/decisions/lab-is-an-addon.md)
- [Survival-Plugin](/architecture/survival.md)
