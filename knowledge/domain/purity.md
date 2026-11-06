---
type: Term
title: Reinheit
description: "Die fünf Reinheitsstufen von Paste und Kokain."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:48:33+01:00 }
sources:
  - id: items
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/lab/item/LabText.java
  - id: data
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/lab/item/LabData.java
  - id: readme
    resource: ../../README.md
---

# Definition

Reinheit ist eine Zahl von 1 bis 5, die Rohpaste und Kokain tragen (`LabData`, bei Paste und Kokain gesetzt). Die Namen aus `LabText`: 1 Dreckig, 2 Schlecht, 3 Ordentlich, 4 Rein, 5 Makellos. Jeder Laborgegenstand zeigt "Reinheit: n von 5" und seinen nächsten Schritt.

# Wie sie sich ändert

- Zuckerrohr ergibt Paste der Reinheit 1.
- Filter heben um eine Stufe: Papier von 1 auf 2, Kohle von 2 auf 3, Seide von 3 auf 4, Gold von 4 auf 5. Die Chance sinkt je Stufe (35, 15, 6, 4 Prozent; der Rest ist Abfall oder nichts), also gilt: je reiner, desto geringer die Ausbeute.
- Feenstaub macht aus Paste Kokain gleicher Reinheit (83 Prozent, Abfall 12, nichts 5).
- Schneiden lässt die Reinheit, Strecken senkt sie um eine Stufe (nicht unter 1).

Das Würfeln mit seiner Regeltabelle liegt in `LabChain` ohne Server, damit die Durchschnitte prüfbar sind: rund 20.000 Zuckerrohr für Reinheit 5 ohne Upgrades, rund 6.500 voll ausgebaut.

# Verwandt

- [Labor: Brauen](/capabilities/lab-brewing.md)
- [Labor: Schneiden und Strecken](/capabilities/lab-cutting.md)
- [Labor: Konsum](/capabilities/lab-consumption.md)
