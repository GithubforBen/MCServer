---
type: Component
title: Velocity-Plugin (VelocityPlugin)
description: Meldet neu angelegte Server am laufenden Proxy an, damit man sofort auf sie warpen kann.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:00+01:00 }
sources:
  - id: velocity
    resource: ../../VelocityPlugin/
---

# Responsibility

Hält den Proxy mit dem Launcher synchron. Das Plugin tritt dem Netzwerk bei, hört auf `ServerRegisteredEvent` und `ServerUnregisteredEvent` und trägt Server am laufenden Proxy ein oder aus. Ohne das Plugin wären nur Server erreichbar, die beim Schreiben der `velocity.toml` schon existierten.

# Interfaces

- [Netzwerk-Events](/architecture/network-events.md)

# Related

- [Launcher](/architecture/launcher.md)
