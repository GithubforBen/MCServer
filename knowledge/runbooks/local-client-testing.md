---
type: Runbook
title: Netzwerk lokal mit echten Clients testen
description: "Netzwerk auf dem Entwicklerrechner starten und Features im Spiel mit zwei per Skript gesteuerten Minecraft-Clients prüfen."
status: draft
stale_after: 2027-01-31T22:17:30+01:00
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:17:30+01:00 }
sources:
  - id: testing-readme
    resource: ../../testing/README.md
---

# When to use

Wenn eine Änderung im Spiel geprüft werden soll, bevor sie nach `master` geht. Was auf `master` landet, kann über `/neustart … update` sofort auf dem echten Server ankommen. Die vollständige Anleitung mit allen Rezepten steht in `testing/README.md`; diese Seite fasst den Ablauf und die Regeln zusammen, die man nicht übersehen darf.

# Steps

1. Grundregeln zuerst:
   - Nie in ein `server-*`-tmux-Fenster tippen, dessen Java-Prozess nicht mehr läuft. Der Text landet dann in einer normalen Shell; `shutdown` hat so zweimal den ganzen Rechner ausgeschaltet. Konsolenbefehle gehen über `testing/clients/con`, das vorher prüft, ob Java läuft.
   - Discord bleibt aus (`MCSERVER_DISCORD=off` in der `.env.local`), sonst meldet sich der Testrechner mit dem Bot des echten Servers an.
   - Arbeitsspeicher: Clients mit 2 GB, Server über `MCSERVER_MAX_MEMORY_MB=1024` gedeckelt, höchstens ein zusätzlicher Runden- oder Event-Server gleichzeitig.
2. Einmalig einrichten: Java 25 in der `.env.local` eintragen, `testing/clients/setup.sh` ausführen und die Clients unter XWayland starten, sonst sieht xdotool sie nicht.
3. Bauen: `./mvnw -B -q install -DskipTests`.
4. Starten: `testing/clients/netstart` wartet, bis LOBBY und SURVIVAL bereit sind.
5. Prüfen: die Skripte in `testing/clients/` steuern die Clients (`mc`, `cmd`, `hclk`, `con`, `place`). Für jedes geprüfte Feature gibt es in `testing/README.md` ein Rezept, teils ohne Client über die Programme in `testing/probes/`.
6. Stoppen: `testing/clients/netstop`, nie `stop` oder `shutdown` in einem Fenster.

# Verify

- Alle Server sehen sich im Cluster: `grep -h "JGroups\] Connected" servers/*/logs/latest.log` zeigt in jeder Zeile `HOST`. Sonst antwortet der Launcher auf nichts.
- `xwininfo -root -tree | grep com.mojang.minecraft` zeigt das Spielfenster.
- Nach dem Test: X-Ray-Paket abwählen und Test-Op wieder entziehen. Probe-Läufe aus `runs.yml` nehmen, solange der Launcher aus ist.

# Related

- [Gemeinsamer Code und Netzwerk-Events](/architecture/common-code.md)
