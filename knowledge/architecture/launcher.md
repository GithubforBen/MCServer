---
type: Component
title: Launcher (ServerLauncherApplication)
description: "Startet Proxy und Server, hält die netzwerkweiten Daten und betreibt Discord-Bot und Admin-Website."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:19:58+01:00 }
sources:
  - id: launcher
    resource: ../../ServerLauncherApplication/
  - id: readme
    resource: ../../README.md
---

# Responsibility

Der Launcher (`de.hems.Main`) läuft außerhalb der Minecraft-Server und ist der Host des Netzwerks. Er startet zuerst den Proxy, dann die Server aus `autostart` in der `main-config.yml`, und legt weitere Server an, wenn jemand sie anfordert. Netzwerkweite Daten gehören ihm: Geld, Teams, Events, Läufe und Ergebnisse liegen als `.yml` beim Launcher und nicht auf einem Spielserver, damit kein einzelner Server sie mitnimmt.

Dazu kommen Discord-Bot, Admin-Website, Tickets, Lotto und der Ablauf von `/neustart`, der bei `update` auch `git pull` und den Build ausführt.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md): antwortet auf Anfragen der Server.
- [Admin-Website](/architecture/admin-website.md): Bedienung im Browser.

# Related

- [Gemeinsamer Code](/architecture/common-code.md)
- [Velocity-Plugin](/architecture/velocity.md)
- [Bits](/domain/bits.md)
