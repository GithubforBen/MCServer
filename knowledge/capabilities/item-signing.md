---
type: Capability
title: Items signieren
description: "Warum Spieler Items signieren können und warum signierte Ware nie verbraucht wird."
status: draft
spec: openspec/specs/item-signing/spec.md
resource: ../../openspec/specs/item-signing/spec.md
change: 2026-11-06-item-signing
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T18:50:27+01:00 }
sources:
  - id: rules
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/SigningRules.java
  - id: command
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/SignItemCommand.java
  - id: case
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/SignItemCaseListener.java
  - id: craft-guard
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/guard/SignedCraftGuard.java
  - id: smelt-guard
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/guard/SignedSmeltAnvilGuard.java
  - id: station-guard
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/guard/SignedStationGuard.java
  - id: lab-cut
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/lab/brew/LabCutBrewing.java
  - id: readme
    resource: ../../README.md
  - id: results
    resource: ../../testing/item-signing/results-2026-11-06.md
  - id: proposal-2026-11-06-item-signing
    resource: ../../openspec/changes/archive/2026-11-06-item-signing/proposal.md
  - id: design-2026-11-06-item-signing
    resource: ../../openspec/changes/archive/2026-11-06-item-signing/design.md
---

# Why

Spieler wollten Ware herstellen, die sich genau einem Spieler zuordnen lässt, zum Beispiel für den Verkauf in Shops. `/signItem` setzt dafür den Namen des Spielers auf die Items in der Hand. Es kostet nichts und gilt nur auf Survival. Signierte Ware soll ganz bleiben, deshalb wird sie nirgends als Zutat verbraucht, auch nicht von Automaten wie dem Crafter.

# How it works

`SignItemCommand` signiert den ganzen Stapel in der Haupthand: die UUID des Spielers liegt unter dem Schlüssel `survival:signed_by` in den Item-Daten, die Beschreibung bekommt die Zeile "Signiert von <Name>". Was mit dem Stapel geschieht, entscheidet `SigningRules` ohne Bukkit-Typen. Ergebnisse: leere Hand (nur ein Hinweis), nicht signierbar (Gadgets, Cosmetics und Menü-Items gehören zum Netzwerk), schon die eigene Signatur (nichts ändert sich, Hinweis), Signatur eines anderen (bleibt, der Spieler erfährt, wer es war), sonst wird signiert. Weil Brigadier Befehlsnamen nach Groß- und Kleinschreibung unterscheidet, schreibt `SignItemCaseListener` `/signItem` vor der Ausführung zu `/signitem` um; im ersten Spiellauf war die große Schreibweise unbekannt.

Gesperrt als Zutat sind der Werktisch, der Crafter und der Schmiedetisch (`SignedCraftGuard`), Ofen, Schmelzofen, Räucherofen und Amboss (`SignedSmeltAnvilGuard`) sowie Steinsäge, Schleifstein und das Tränkebrauen im Vanilla-Braustand (`SignedStationGuard`). An der Steinsäge lässt sich das Ergebnis auch per Shift-Klick nicht nehmen; signiertes Gut lässt sich nicht oben in einen Braustand legen (auch nicht per Trichter von oben), signierte Flaschen unten bleiben unverändert. Im Ofen zündet signiertes Gut nicht und verbraucht keinen Brennstoff; im Crafter bleibt es signiert liegen. Im Amboss darf es links liegen, das Ergebnis behält die Signatur, als Opfer rechts gibt es kein Ergebnis. Laborbrauvorgänge fassen signierte Zutaten nicht an; signiertes Kokain wird nicht geschnitten, siehe [Labor: Schneiden](/capabilities/lab-cutting.md).

Shops verkaufen und kaufen signierte Ware zurück wie jede andere. Signiert und unsigniert sowie die Signaturen verschiedener Spieler sind verschiedene Waren, weil Shops genaue Items vergleichen; dafür gibt es keinen eigenen Code. Siehe [Shop-Rückkauf](/capabilities/shop-buyback.md). Stapeln: [Signatur](/domain/signature.md).

Im Spiel wurden alle 13 Szenarien gesehen (Lauf vom 2026-11-06), darunter Crafter, Ofen, Amboss links und rechts, Verkauf und Rückkauf signierter Ware.

# Related

- [Signatur](/domain/signature.md)
- [Shop-Rückkauf](/capabilities/shop-buyback.md)
- [Labor: Schneiden](/capabilities/lab-cutting.md)
- [Survival-Plugin](/architecture/survival.md)
