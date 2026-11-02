---
type: Component
title: Gemeinsamer Code (CommonCode)
description: Netzwerk-Events, ServerApi, gemeinsame Typen und Paper-Bausteine, die alle anderen Module benutzen.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:19:58+01:00 }
sources:
  - id: common
    resource: ../../CommonCode/
---

# Responsibility

Die Bibliothek, auf der alle anderen Module aufbauen, und mit 310 Java-Dateien das größte Modul. Drei Bereiche:

- `de.hems.communication`: die Netzwerk-Events und ihr Transport.
- `de.hems.api` und `de.hems.types`: die `ServerApi` und die Datentypen, die zwischen Servern reisen.
- `de.hems.paper`: fertige Bausteine für Paper-Plugins, etwa Cosmetics, Event-Oberflächen, Geld, Runden und das Warp-System.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)
- [ServerApi](/architecture/server-api.md)

# Related

- [Launcher](/architecture/launcher.md)
- [Cosmetic](/domain/cosmetic.md)
- [Netzwerk lokal testen](/runbooks/local-client-testing.md)
