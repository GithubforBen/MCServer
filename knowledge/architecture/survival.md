---
type: Component
title: Survival-Plugin (Survival)
description: Survival-Spielmodus mit Teams, Shops, Marktplatz, Chunk Limiter und angepassten Paper-Einstellungen.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:19:59+01:00 }
sources:
  - id: survival
    resource: ../../Survival/
---

# Responsibility

Der Survival-Spielmodus. Unter `featrues/` (Schreibweise wie im Code) liegen Shopkeeper und Marktplatz, Teams, Chunk Limiter, Geld, Admin-Funktionen und der End-Kampf. `MoneyHandler` antwortet aus der lokalen Kopie; das Geld selbst gehört dem Launcher.

Shops kaufen optional an: je Angebot, standardmäßig aus. `Shopkeeper.sellItem` verkauft ein Los gegen die Teamkasse, die Vorprüfungen stehen in `BuybackRules`, der Marktplatz zeigt den Rückkauf mit an. Siehe [Shop-Rückkauf](/capabilities/shop-buyback.md).

Shops können einen Spieler-Skin tragen und stehen dann als `Mannequin` statt als Villager; mit demselben Lebenszyklus (Übernahme statt Neuaufbau). Siehe [Shop-Skin](/capabilities/shop-skin.md).

Survival enthält außerdem das Labor (`featrues/lab/`): eine Produktionskette im Braustand von Zuckerrohr bis Kokain mit Upgrades, Schneiden, Konsum und dem Befehl `/labor`, als Zusatz ohne Wirkung auf Spieler, die nicht mitmachen. Siehe [Labor: Brauen](/capabilities/lab-brewing.md).

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)

# Related

- [Shop-Rückkauf](/capabilities/shop-buyback.md)
- [Shop-Skin](/capabilities/shop-skin.md)
- [Labor: Brauen](/capabilities/lab-brewing.md)
- [Bits](/domain/bits.md)
- [Rucksack-Plugin](/architecture/backpack.md)
