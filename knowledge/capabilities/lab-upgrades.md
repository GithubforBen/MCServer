---
type: Capability
title: "Labor: Upgrades"
description: "Warum Vanilla-Blöcke am Stand als Upgrades wirken statt eigener Blöcke oder Items."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:48:32+01:00 }
sources:
  - id: reader
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/upgrade/LabUpgradeReader.java
  - id: upgrades
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/upgrade/LabUpgrades.java
  - id: fuel
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/upgrade/LabFuel.java
  - id: results
    resource: ../../testing/lab-brewing/results-2026-11-06.md
  - id: readme
    resource: ../../README.md
---

# Warum

Ein Labor soll sich ausbauen lassen, ohne dass Spieler ein neues Item oder einen neuen Block lernen müssen. Deshalb wirken Vanilla-Blöcke in der Nähe des Stands als Upgrades. Ohne Ausbau bleibt der Stand ein normaler Braustand; wer nichts danebenstellt, merkt keinen Unterschied.

# Wie es funktioniert

Gelesen werden Blöcke bis zwei Felder um den Stand (`LabUpgradeReader`). Die Reichweite ist klein gehalten, weil Trichter die Flächen direkt am Stand brauchen. Jeder Typ zählt einmal, egal wie viele davon stehen (`LabUpgrades`):

- Wasserkessel mit Wasser und Amethystblock: je 10 Prozent mehr Chance auf Ware (Faktor 1,1 je Typ).
- Blitzableiter: der Vorgang dauert ein Viertel kürzer (Faktor 0,75).
- Räucherofen: Brennstoff reicht länger.

Gelesen wird beim Start eines Vorgangs. Ein laufender Vorgang behält seine Upgrades, auch wenn man währenddessen eines abbaut; erst der nächste sieht die Änderung. Die Upgrades liegen als kleine Zahl (`bits()`) am laufenden Vorgang. Im Spiel zeigten sich der Wasserkessel (nur gefüllt zählt er), der Blitzableiter (150 statt 200 Ticks bei Testzeit 10 s) und das Abbauen genau so.

Brennstoff (`LabFuel`): Ein Vorgang kostet eine Stufe. `fuel.brews-per-item` gilt für Lohenstaub ohne Räucherofen (Standard 20, wie Vanilla), `upgrade.smoker-brews-per-item` mit Räucherofen (Standard 30). Beide gelten nur für das Labor, der normale Braustand bleibt unberührt.

Mit allen Upgrades braucht ein Kokain von Reinheit 5 rund 6.500 statt rund 20.000 Zuckerrohr (siehe [Labor: Brauen](/capabilities/lab-brewing.md)).

# Verwandt

- [Labor: Brauen](/capabilities/lab-brewing.md)
- [Eigener Tick für Zuckerrohr](/decisions/lab-brewing-engine.md)
