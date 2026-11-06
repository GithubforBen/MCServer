# Decision

* [Bedwars: eine Arena pro Server, Phasen als Klassen, Addons nur über Events](bedwars-foundations.md) - Die Grundentscheidungen des Bedwars-Plugins, auf denen Rundenablauf und Addons aufbauen.
* [Das Labor bleibt ein Zusatz](lab-is-an-addon.md) - Das Labor bleibt ein vanilla-naher Zusatz auf Survival; Razzien und neue Bits wurden verworfen.
* [Eigener Tick für Zuckerrohr, sonst Vanilla-Brauen](lab-brewing-engine.md) - Zuckerrohr braut mit eigenem Tick auf leeren Slots, alles andere läuft auf dem Vanilla-Brauen.
  * Referenced from code: Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/BrewState.java:7
  * Referenced from code: Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabArrow.java:24
  * Referenced from code: Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabBrewing.java:42
  * Referenced from code: Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabPurifying.java:33
* [Gadgets sind nie ein Spielvorteil und liegen in einem Slot pro Server](gadget-choices.md) - Warum die 18 Gadgets pro Server getrennt ausgewählt werden und warum Bedwars keine Gadgets einschaltet.
* [Rückkauf zahlt die Teamkasse über den bestätigten Weg](shop-buyback-paid-by-team.md) - Rückkauf zahlt die Teamkasse über den bestätigten Weg, auch das eigene Team darf verkaufen.
* [Shop-Mannequins werden übernommen](shop-mannequin-lifecycle.md) - Mannequins der Shops leben wie Villager persistent und werden übernommen, nicht neu aufgebaut wie in der Lobby.
