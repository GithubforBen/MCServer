---
type: Term
title: Ghost-Server
description: "Ein vorbereiteter Run-Server, den der Launcher bereithält, damit der nächste Lauf nicht auf die Weltgenerierung wartet."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:03+01:00 }
sources:
  - id: ghost
    resource: ../../ServerLauncherApplication/src/main/java/de/hems/utils/event/GhostRunServers.java
  - id: tracker
    resource: ../../RunPlugin/src/main/java/de/schnorrenbergers/run/RunTracker.java
  - id: testing
    resource: ../../testing/README.md
---

# Meaning

Ein Server, den der Launcher für ein Event bereithält (`GhostRunServers`). Nach `/reset` übernimmt ihn der nächste Lauf (`ClaimGhostRunEvent`), sodass keine Welt generiert werden muss, und der Launcher baut gleich den nächsten. Sind weniger als zwei Spieler im Event übrig, wirft er ihn weg. Bis ein Lauf ihn übernimmt, zählt `RunTracker` dort nichts und stoppt ihn auch nicht.

# Not to be confused with

- Run-Server eines laufenden Versuchs: ein Ghost-Server hat noch keinen Lauf und wird erst durch die Übernahme zu einem Run-Server.

# Related

- [Lauf](/domain/run.md)
- [Speedrun-Plugin](/architecture/run-plugin.md)
