---
type: Decision
title: "Eigener Tick für Zuckerrohr, sonst Vanilla-Brauen"
description: "Zuckerrohr braut mit eigenem Tick auf leeren Slots, alles andere läuft auf dem Vanilla-Brauen."
status: draft
decision: accepted
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T14:48:33+01:00 }
sources:
  - id: brewing
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabBrewing.java
  - id: arrow
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabArrow.java
  - id: purifying
    resource: ../../Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabPurifying.java
  - id: proof
    resource: ../../testing/lab-brewing/proof/results-2026-11-05.md
  - id: results
    resource: ../../testing/lab-brewing/results-2026-11-06.md
---

# Kontext

Vanilla braut nur, wenn ein unterer Slot ein Item hat, das zu einem Rezept passt. Der Wunsch ist aber: unten liegt nichts, Zuckerrohr oben, drei Ausgänge. Leere Slots sind das größte Risiko, deshalb wurde zuerst auf einem echten Server geprüft, ob das hält.

# Entscheidung

Zwei Wege mit einer Regeltabelle (`LabChain`). Zuckerrohr auf leeren Slots läuft auf einem eigenen Takt (`LabBrewing`), einmal pro Sekunde über die aktiven Stände. Alles mit Item im unteren Slot (Reinigen, Kokain, Schneiden) läuft auf dem Vanilla-Brauen: ein registriertes `BrewingRecipe` startet es, ein `BrewEvent`-Handler ersetzt das Ergebnis durch Würfeln (`LabPurifying`).

Der Fortschritt eines eigenen Vorgangs steht im Block (`BrewState`, ohne Minecraft, mit Fake-Uhr prüfbar). Er wird jeden Schritt geschrieben, daher braucht es kein Speichern bei Entladen oder Absturz. Gewürfelt wird erst am Ende, wie in Vanilla; ein Absturz wiederholt höchstens die letzten Sekunden, nie ein Ergebnis. Start, Pause, Ende und Wiederaufnahme werden mit Position geloggt.

Fortschrittspfeil (`LabArrow`): Vanilla setzt die Brauzeit eines Stands, der nicht brauen kann, jeden Tick auf 0, und die Bukkit-Setter schreiben genau diesen Wert. Der eigene Weg schickt den Pfeil daher als Datenpaket an jeden Betrachter, per Reflexion mit Mojang-Namen. Ein Minecraft-Update kann die Reflexion brechen; dann schaltet sich der Pfeil mit "Lab: progress arrow disabled" selbst ab, und das Brauen läuft weiter. Das ist ein akzeptiertes Risiko.

Reinigen nutzt neun exakte Rezepte (`ExactChoice` je Filterstufe und Feenstaub je Reinheit), sodass eine normale Tonscherbe nie braut.

# Alternativen

- Eigener Takt für alles: verworfen, Vanilla liefert Fortschritt, Ton und Brennstoff umsonst, wo es kann.
- Glasflaschen unten (Rückfall): nicht nötig, die leeren Slots hielten bei Entladen, Neustart, `kill -9`, Abbauen und acht Ständen. Als Rückfall hätte Zuckerrohr ebenfalls ein `BrewingRecipe` bekommen.
- Pfeil über die Bukkit-Setter: ging im Lauf nicht, der Pfeil blieb leer, bis das Paket kam.

# Folgen

Ein Stand bleibt aktiv, solange er brauen kann; ein Item, das ihn verlässt, weckt ihn. Der Trichter braucht keinen Zustand je Slot, weil Ergebnisse nie ein gültiger Schritt für dieselbe Zutat sind. Einen Belegungsnachweis liefert `testing/lab-brewing/proof/results-2026-11-05.md`.

# Verwandt

- [Labor: Brauen](/capabilities/lab-brewing.md)
- [Reinheit](/domain/purity.md)
- [Das Labor bleibt ein Zusatz](/decisions/lab-is-an-addon.md)
