---
type: Term
title: Lauf
description: Ein Versuch in einem zeitgemessenen Event, auf einem eigenen Run-Server.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:02+01:00 }
sources:
  - id: data
    resource: ../../CommonCode/src/main/java/com/benschnorr/types/event/RunData.java
  - id: tracker
    resource: ../../RunPlugin/src/main/java/com/benschnorr/run/RunTracker.java
---

# Meaning

Ein Versuch in einem zeitgemessenen Event, also derzeit `UHC_BOSSES` oder `UHC_DRAGON` (`RunData`). Jeder Versuch bekommt einen eigenen Server `RUN_<event>_<lauf>`. Ein Lauf ist `RUNNING`, `PAUSED`, `FINISHED`, `FAILED` oder `ABANDONED`. Jeder Lauf zählt als Versuch, auch ein gescheiterter oder abgebrochener.

# Not to be confused with

- [Runde](/domain/round.md): eine Partie Bedwars, ohne Zeitmessung gegen die Uhr.

# Related

- [Speedrun-Plugin](/architecture/run-plugin.md)
- [Ghost-Server](/domain/ghost-server.md)
