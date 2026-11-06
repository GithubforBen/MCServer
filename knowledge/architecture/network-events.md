---
type: Interface
title: Netzwerk-Events
description: "Der JGroups-Cluster MCServer, über den Launcher, Proxy und Server einander Events schicken und Anfragen beantworten."
status: draft
stale_after: 2027-01-31T22:20:01+01:00
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:01+01:00 }
sources:
  - id: adapter
    resource: ../../CommonCode/src/main/java/com/benschnorr/communication/ListenerAdapter.java
  - id: events
    resource: ../../CommonCode/src/main/java/com/benschnorr/communication/events/
---

# Contract summary

`ListenerAdapter` tritt unter dem eigenen Servernamen dem JGroups-Cluster `MCServer` bei. Empfänger registrieren sich mit `ListenerAdapter.register(EventClass, EventHandler)`. Die Event-Klassen liegen nach Thema geordnet unter `com.benschnorr.communication.events`, etwa `server`, `event`, `money`, `team`, `round` und `cosmetic`. Viele kommen als Paar aus Anfrage und Antwort (`Request…`/`Respond…`). Sieht ein Server den Launcher nicht im Cluster, antwortet ihm niemand.

# Consumers

Alle Module: der Launcher als Host, die Paper-Plugins und das Velocity-Plugin.

# Related

- [Launcher](/architecture/launcher.md)
- [ServerApi](/architecture/server-api.md)
