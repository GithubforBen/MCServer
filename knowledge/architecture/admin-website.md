---
type: Interface
title: Admin-Website
description: "Browser-Oberfläche des Launchers für Admins, mit Login per Passwort und Authenticator, Panels und Live-Konsole."
status: draft
stale_after: 2027-01-31T22:20:01+01:00
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:01+01:00 }
sources:
  - id: web
    resource: ../../ServerLauncherApplication/src/main/java/com/benschnorr/utils/webconsole/
  - id: readme
    resource: ../../README.md
---

# Contract summary

`WebServer` läuft auf Javalin (Jetty), weil der eingebaute `com.sun.net.httpserver` keine WebSockets kann. Panels sind Module (`WebModule`, unter `webconsole/modules`). Die Auth-Schicht (`Totp`, `Passwords`, `AuthService`, `Session`) ist vom Webserver unabhängig. Einstellungen stehen in der `main-config.yml`.

Beim Packaging zählt `src/assembly/jar-with-dependencies.xml`: Jetty findet Teile von sich per `ServiceLoader`, und der eingebaute Descriptor würde gleichnamige `META-INF/services`-Dateien überschreiben, statt sie zusammenzuführen.

# Consumers

Admins im Browser.

# Related

- [Launcher](/architecture/launcher.md)
