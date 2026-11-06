---
type: Capability
title: Shop-Skin
description: "Warum Händler einen Spieler-Skin tragen können und warum sie dafür ein Mannequin werden."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:30:10+01:00 }
sources:
  - id: shop
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/Shopkeeper.java
  - id: skin-name
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/SkinName.java
  - id: editor
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopEditorUi.java
  - id: results
    resource: ../../testing/shop-skin/results-2026-11-06.md
  - id: readme
    resource: ../../README.md
---

# Why

Alle Shops sahen aus wie jeder andere Villager. Teams wollten ihren Shops ein Gesicht geben, so wie die NPCs der Lobby echte Spieler-Skins tragen. Ein Villager kann keinen Skin tragen; ein Shop mit Skin steht deshalb als `Mannequin`, die spielerförmige Entity, die die Lobby ebenfalls nutzt. Shops ohne Skin bleiben Villager und ändern sich nicht (fehlender Schlüssel `skin` in `shop-config.yml` heißt: kein Skin).

# How it works

Im Shop-Editor, den nur das besitzende Team erreicht, fragt der Skin-Knopf per Chat nach einem Minecraft-Spielernamen. `SkinName.parse` nimmt das Format von `/npc skin` (3 bis 16 Buchstaben, Ziffern, Unterstrich); `aus` entfernt den Skin, `abbrechen` lässt alles, ein ungültiger Name wird mit Hinweis abgelehnt. Der Code kennt zusätzlich die englischen Wörter `cancel` und `none`. Ein unbekannter Account zeigt wie in der Lobby den Standard-Look.

`Shopkeeper` hält ein `LivingEntity`; ob Villager oder Mannequin, folgt allein aus `skin`. Das Mannequin trägt dieselbe `SHOP_ID`, ist unverwundbar, ohne KI, lautlos, nicht schiebbar und zeigt den Shop-Namen wie der Villager (ohne Beschreibungszeile). Menüs, Editor und Verschieben laufen wie bisher. Ändert sich der Skin, tauscht `setSkin` die Figur sofort aus, bei entladenem Chunk beim nächsten Laden. Warum das Mannequin übernommen statt neu aufgebaut wird: [Shop-Mannequins werden übernommen](/decisions/shop-mannequin-lifecycle.md).

Im Spiel wurden alle Szenarien gesehen (Lauf vom 2026-11-06), auch Neustart und Chunk-Nachladen mit genau einer Figur. Offen geblieben: mit dem Namen `Notch` erschien ein Alex-artiger Skin, ob das sein echter ist, ist nicht geklärt; `LongRangeMissile` zeigte den echten Skin.

# Related

- [Shop-Mannequins werden übernommen](/decisions/shop-mannequin-lifecycle.md)
- [Lobby-Plugin](/architecture/lobby.md)
- [Survival-Plugin](/architecture/survival.md)
