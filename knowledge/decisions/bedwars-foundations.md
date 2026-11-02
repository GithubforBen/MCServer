---
type: Decision
title: "Bedwars: eine Arena pro Server, Phasen als Klassen, Addons nur über Events"
description: Die Grundentscheidungen des Bedwars-Plugins, auf denen Rundenablauf und Addons aufbauen.
status: draft
decision: accepted
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:17:31+01:00 }
sources:
  - id: bedwars-plan
    resource: ../../Bedwars/PLAN.md
---

# Decision

- Genau eine Arena pro Server. Ist die Runde vorbei, wird der Server weggeworfen; das Plugin setzt keine Map zurück.
- Jede Phase ist eine Klasse mit `onEnter`, `tick` und `onExit` (`GamePhase`). Listener fragen die Phase ab, statt an vielen Stellen `if (running)` zu prüfen.
- Eigene Bukkit-Events (`BedwarsEvent` und seine Unterklassen) sind die einzige Andockstelle. Addons hören nur darauf und fassen die Spiellogik nie an.
- Runden starten über das Event-System, dazu kommt ein Debug-Befehl in der Lobby, der denselben Weg nimmt.
- Shop, Upgrades und Generatoren orientieren sich an Hypixel, kommen aber vollständig aus der Config und lassen sich pro Map überschreiben.
- Addons lassen sich einzeln schalten: `addons.yml` als Basis, der Launcher darf überschreiben, dazu eine GUI in der Warte-Lobby.
- Die Texte des Plugins sind Englisch und kommen aus `messages.yml`.

# Why

Mit Phasen als Klassen ist ein neuer Zustand, etwa ein Warmup, eine neue Klasse und keine Änderung an dreißig Stellen. Weil Addons nur Events hören, heißt Abschalten einfach Listener abmelden; genau deshalb lässt sich jedes Addon wirklich abschalten. Andere Plugins können an derselben Stelle andocken. Der Wegwerf-Server spart jede Logik zum Zurücksetzen der Map.

Die vollständige Liste der Entscheidungen steht in `Bedwars/PLAN.md`.

# Rejected alternatives

Der Plan nennt keine verworfenen Alternativen ausdrücklich. Zustandsabfragen über `if (running)` sind der Ansatz, gegen den die Phasen stehen.

# Related

- [Bedwars-Plugin](/architecture/bedwars.md)
- [Runde](/domain/round.md)
