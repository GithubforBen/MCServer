---
type: Capability
title: "Labor: Konsum"
description: "Warum Konsum über Vanilla-Effekte gehen darf, aber im Kampf gedeckelt ist."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:48:32+01:00 }
sources:
  - id: consume
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/consume/LabConsume.java
  - id: combat
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/consume/LabCombat.java
  - id: rules
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/consume/LabConsumeRules.java
  - id: results
    resource: ../../testing/lab-brewing/results-2026-11-06.md
  - id: readme
    resource: ../../README.md
---

# Warum

Kokain soll sich beim Konsum anfühlen wie ein Preis für die lange Arbeit: Tempo und Eile über Vanilla-Tränken. Im Kampf wäre das ein unfairer Vorteil gegen Spieler, die nichts mit dem Labor zu tun haben, deshalb ist die Stärke dort gedeckelt. Wer nicht konsumiert, spürt davon nichts.

# Wie es funktioniert

Rechtsklick verbraucht ein Kokain (`LabConsume`) und gibt Tempo und Eile nach Reinheit, Stufe 1 bis 3 und 30 bis 120 Sekunden, dazu alle eingeschnittenen Effekte. Im Spiel gab Reinheit 5 Tempo III und Eile III für 120 s. Danach folgt ein Absturz aus Übelkeit und Langsamkeit (`LabConsumeRules`). Zwischen zwei Kokain liegen 60 Sekunden; die Meldung nennt die Restzeit. Das Tragen allein bewirkt nichts.

Kampf (`LabCombat`) ist ein Treffer von oder gegen einen anderen Spieler in den letzten 15 Sekunden, auch durch Pfeile. Mobs zählen nicht. Mit Kampf fallen Tempo und Eile auf Stufe II und behalten ihre Restdauer. Eigene Wirkungen unterscheidet der Code von Trank oder Leuchtfeuer über den Eintrag in den Spielerdaten (gleiche Stufe, Ende innerhalb von zwei Sekunden), ein Leuchtfeuer-Eile II bleibt unberührt.

Der Absturz hängt als Fälligkeitszeit am Spieler. Nach einem Relog laufen die Effekte auf die Uhrzeit des Endes ausgerichtet: Ist der Absturz fällig, nimmt der Code Tempo, Eile und die eingeschnittenen Effekte weg und startet ihn; sonst werden sie auf die Restzeit gesetzt. Ausloggen überspringt den Absturz also nicht, und Sterben bricht ihn ab. Im Lauf vom 2026-11-06 liefen die eingeschnittenen Effekte nach dem Relog noch weiter; dafür gibt es die Anpassung.

# Verwandt

- [Labor: Brauen](/capabilities/lab-brewing.md)
- [Labor: Schneiden und Strecken](/capabilities/lab-cutting.md)
- [Reinheit](/domain/purity.md)
