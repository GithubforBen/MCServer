---
type: Capability
title: "Labor: Schneiden und Strecken"
description: Warum Kokain nach der Herstellung gestreckt und mit Effekten versehen werden kann.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:48:32+01:00 }
sources:
  - id: cutting
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/cutting/LabCutting.java
  - id: cutbrew
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabCutBrewing.java
  - id: check
    resource: ../../Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/cutting/LabCuttingCheck.java
  - id: results
    resource: ../../testing/lab-brewing/results-2026-11-06.md
  - id: readme
    resource: ../../README.md
---

# Warum

Kokain soll nach der Herstellung noch etwas zu tun geben und lustig sein: Man kann es mit Zutaten schneiden, die Effekte beim Konsum hinzufügen, und mit Zucker strecken, um aus wenig mehr zu machen. Beides kostet Reinheit oder bleibt bei ihr, nie bringt es Bits.

# Wie es funktioniert

Kokain liegt unten, die Zutat oben (`LabCutting`, Rezepte in `LabCutBrewing`). Schneiden fügt dem Kokain einen Effekt hinzu: Goldene Karotte (Nachtsicht), Phantomhaut (Sprungkraft), Feuerkugel (Feuerresistenz), Honigflasche (Regeneration), Magmacreme (Stärke). Bis zu drei verschiedene, die Reihenfolge ist egal, die Reinheit bleibt. Honig lässt eine leere Glasflasche zurück.

Strecken mit Zucker macht aus einem Kokain ab Reinheit 2 drei Kokain mit einer Stufe weniger. Gestrecktes trägt eine Markierung: Es lässt sich nicht erneut strecken, und Zucker ist dafür kein gültiger Schritt mehr.

Beides gilt für den ganzen Slot: Schneiden behält jedes Item im Slot, Strecken macht aus jedem Item drei. Passen dreimal so viele Items nicht in einen Stapel, ist Strecken kein Schritt, es wird nichts verbraucht und nichts geht verloren. Dasselbe gilt für alles, was nicht geht: Es ist kein Schritt, statt zu scheitern.

Anders als das Reinigen lässt sich Schneiden nicht über feste Rezepte aufzählen, weil die Effektkombinationen zu viele sind. Das Vanilla-Rezept nimmt deshalb ein breiteres Eingabeitem, und die Prüfung sitzt im `BrewEvent`-Handler. Der Handler und die Regeln werden von `LabCuttingCheck` geprüft.

Im Spiel gesehen: Karotte oben, Kokain seitlich ergibt Nachtsicht bei gleicher Reinheit; 1 Kokain R3 mit Zucker ergibt 3 gestreckte R2 mit erhaltener Nachtsicht.

# Verwandt

- [Labor: Brauen](/capabilities/lab-brewing.md)
- [Labor: Konsum](/capabilities/lab-consumption.md)
- [Reinheit](/domain/purity.md)
