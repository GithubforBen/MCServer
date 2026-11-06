---
type: Interface
title: ServerApi
description: Programmatischer Weg, Server im Netzwerk anzulegen, zu starten, zu stoppen und aufzulisten.
status: draft
stale_after: 2027-01-31T22:20:01+01:00
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:01+01:00 }
sources:
  - id: api
    resource: ../../CommonCode/src/main/java/com/benschnorr/api/ServerApi.java
  - id: readme
    resource: ../../README.md
---

# Contract summary

`com.benschnorr.api.ServerApi` legt von überall im Cluster Server an, startet, stoppt und listet sie, ohne dass der Aufrufer Ports oder Jars kennt. `ServerTemplate` legt Software, Standard-RAM und Pflichtplugins fest. Der Host vergibt den Port, installiert die Plugins und meldet den Server an, sodass man sofort hinwarpen kann. Namen werden normalisiert.

`listServers()`, `isRunning()` und `freeName()` warten auf die Antwort des Hosts und gehören nicht in den Main-Thread; dafür gibt es `listServersAsync()` und `PaperContext.async(...)`.

# Consumers

Plugins und Event-Starter, die Server anlegen, etwa für Bedwars-Runden, Events und Speedrun-Läufe.

# Related

- [Netzwerk-Events](/architecture/network-events.md)
- [Launcher](/architecture/launcher.md)
