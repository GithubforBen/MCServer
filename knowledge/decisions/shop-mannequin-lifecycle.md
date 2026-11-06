---
type: Decision
title: "Shop-Mannequins werden übernommen"
description: "Mannequins der Shops leben wie Villager persistent und werden übernommen, nicht neu aufgebaut wie in der Lobby."
status: draft
decision: accepted
change: 2026-11-06-shop-skin
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:30:10+01:00 }
sources:
  - id: shop
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/Shopkeeper.java
  - id: lobby-npcs
    resource: ../../LobbyPlugin/src/main/java/de/schnorrenbergers/lobby/npc/LobbyNpcs.java
  - id: readme
    resource: ../../README.md
  - id: results
    resource: ../../testing/shop-skin/results-2026-11-06.md
---

# Decision

Das Mannequin eines Shops lebt wie der Villager: persistent, mit der `SHOP_ID` markiert, und beim Laden wird die vorhandene Figur übernommen (`spawnOrAdoptVillager`). Findet die Übernahme eine Figur der falschen Art, etwa weil sich der Skin änderte, während der Chunk entladen war, wird sie entfernt und die richtige gestellt; übrige Dopplungen räumt sie ebenfalls weg.

# Why

Die Lobby-NPCs sind `setPersistent(false)` und werden bei jedem Start und Chunk-Laden neu aufgestellt (`LobbyNpcs`). Das passt dort, weil ihre Einstellungen in `npcs.yml` stehen und nichts an der Figur hängt. Shops sind persistent und übernehmen schon ihren Villager, damit nach einem Neustart keine zweite Figur zurückbleibt. Ein zweiter Lebenszyklus neben dieser Übernahme wäre genau der Weg, auf dem Shops doppelt entstehen. Im Spiel gab es nach Neustart und Chunk-Nachladen je genau ein Mannequin und keinen Villager.

# Rejected alternatives

- Mannequin je Laden neu aufbauen wie in der Lobby: zwei Lebenszyklen für eine Figur, Risiko doppelter Shops.

# Related

- [Shop-Skin](/capabilities/shop-skin.md)
- [Lobby-Plugin](/architecture/lobby.md)
