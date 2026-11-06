---
type: Capability
title: "Shop-Rückkauf"
description: "Warum Händler ankaufen können und warum das aus der Teamkasse statt aus neuen Bits kommt."
status: draft
spec: openspec/specs/shop-buyback/spec.md
resource: ../../openspec/specs/shop-buyback/spec.md
change: 2026-11-06-shop-buyback
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:19:23+01:00 }
sources:
  - id: shop
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/Shopkeeper/Shopkeeper.java
  - id: rules
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/Shopkeeper/BuybackRules.java
  - id: results
    resource: ../../testing/shop-buyback/results-2026-11-06.md
  - id: proposal-2026-11-06-shop-buyback
    resource: ../../openspec/changes/archive/2026-11-06-shop-buyback/proposal.md
  - id: design-2026-11-06-shop-buyback
    resource: ../../openspec/changes/archive/2026-11-06-shop-buyback/design.md
---

# Why

Ein Shop verkaufte bisher nur, Ware konnte nie zu ihm zurück. Der Wunsch war ein Villager, der Diamantblöcke für 900 Bits verkauft und sie auch für 900 Bits wieder annimmt. Der Rückkauf ist eine Option am bestehenden Angebot und standardmäßig aus, damit alte Shops unverändert bleiben (fehlende Schlüssel `buyback.enabled` und `buyback.price` in `shop-config.yml` heißen aus).

# How it works

Das Team legt je Angebot einen eigenen Rückkaufpreis fest; beim Einschalten ist er mit dem Verkaufspreis vorbelegt. Ein Rechtsklick im Shop oder in der Anbieterliste des Marktplatzes verkauft ein Los, der Linksklick kauft weiter. Der Marktplatz zeigt je Listing den besten Rückkaufpreis pro Stück.

`Shopkeeper.sellItem` nimmt das Los dem Spieler ab, bucht die Teamkasse asynchron ab und legt erst in der Antwort die Ware in die Kiste und zahlt den Spieler. Scheitert die Abbuchung, bekommt der Spieler das Los zurück; ist die Kiste inzwischen voll, geht das Los zurück und das Team bekommt die Bits erstattet. Welche Vorprüfungen es gibt, steckt als reine Logik in `BuybackRules`, damit ein einfaches `main` sie testen kann. Es entstehen keine neuen Bits.

Im Spiel wurden alle 18 Szenarien gesehen (Läufe vom 2026-11-03 und 2026-11-06).

# Related

- [Rückkauf zahlt die Teamkasse](/decisions/shop-buyback-paid-by-team.md)
- [Bits](/domain/bits.md)
- [Survival-Plugin](/architecture/survival.md)
