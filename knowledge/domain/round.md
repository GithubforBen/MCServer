---
type: Term
title: Runde
description: Eine Partie Bedwars auf einem eigenen Server, von einem Event oder einem Spieler gestartet.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:02+01:00 }
sources:
  - id: data
    resource: ../../CommonCode/src/main/java/com/benschnorr/types/round/RoundData.java
  - id: readme
    resource: ../../README.md
---

# Meaning

Eine Partie Bedwars auf einem eigenen Server. Eine eigene Runde (`RoundData`) stellt ein Spieler in der Lobby über `/runde start` zusammen, mit Map, Modus und Addons, und ist darin Rundenadmin. Das ist standardmäßig aus und wird unter `/runde admin` freigeschaltet.

# Not to be confused with

- [Lauf](/domain/run.md): ein Versuch in einem zeitgemessenen Event.

# Related

- [Bedwars-Plugin](/architecture/bedwars.md)
- [Lobby-Plugin](/architecture/lobby.md)
