# Migration plan: MCServer

## Repository

- Default branch: master
- Stack: Java 25, Maven, Paper- und Velocity-Plugins
- Layout: Monorepo (Maven-Multi-Modul mit 10 Modulen). Korrigiert: die Inventur meldete "single package".
- CI: GitHub Actions
- Source globs: `*/src/**`. Korrigiert: der Fallback `src/**` trifft in diesem Repository keine einzige Datei.
- Agent tools: agents, claude
- Sprache: Fließtext in Konzepten, Bericht und PR auf Deutsch. Technische Begriffe, Bezeichner und OpenSpec-Specs auf Englisch.

## Capability domains

- Netzwerk und Launcher: startet und konfiguriert die Server, Updates, Speicherbudget (`ServerLauncherApplication`, `VelocityPlugin`)
- Gemeinsame Plattform: Netzwerk-Events, `ServerApi`, Warps, Geld, Runden (`CommonCode`)
- Spielmodi und Events: Survival, Bedwars, Pokernacht, Hunger Games, Speedrun (`Survival`, `Bedwars`, `PokerPlugin`, `HungerGamesPlugin`, `RunPlugin`)
- Lobby: Parkour, NPCs, Server Manager, eigene Runden (`LobbyPlugin`)
- Spielerbesitz: Teams, Rucksack, Shops, Cosmetics, Lotto (`BackpackPlugin`, `CommonCode`)
- Verwaltung: Admin-Website, Discord-Bot, Tickets, Whitelist (`ServerLauncherApplication`)

## Mapping

| Source | Becomes | Target | Note |
| --- | --- | --- | --- |
| README.md | keep | | Einstieg für Menschen, Quelle für die Konzepte aus dem Code |
| testing/README.md | Runbook | knowledge/runbooks/local-client-testing.md | Enthält die Sicherheitsregel zu den tmux-Fenstern der Server |
| GADGETS.md | Decision | knowledge/decisions/gadget-choices.md | Entscheidungsspur, von Hand ergänzt, fehlte in der Inventur |
| Bedwars/PLAN.md | Decision | knowledge/decisions/bedwars-foundations.md | Nur die getroffenen Entscheidungen, nicht die Phasen; fehlte in der Inventur |
| TODO.md | keep | | Über mehrere Sitzungen abgearbeitet, die Häkchen sind nicht aktuell. Relevant sind laut Ben nur noch Survival, Bedwars, Speedrun und Lobby. Kein Import als Backlog; im Bericht als Kandidat für später genannt |
| ABNAHME.md | keep | | Testplan; fehlte in der Inventur |
| Bedwars/TESTS.md | keep | | Testplan; fehlte in der Inventur |
| ServerLauncherApplication/src/main/resources/discord-info/ | keep | | Texte, die der Bot an Spieler und Admins schickt, keine Projektdoku |

Originaldateien werden weder verschoben noch gelöscht.

## Seed from code

- Cap: 30 concepts
- From: `*/src/**`, gegliedert nach `graphify-out/GRAPH_REPORT.md`
- Vorher läuft `specfold graphify setup`: nur der Code wird ausgewertet (`GRAPHIFY_BACKEND=none`), nichts verlässt den Rechner. Das fügt `.agents/skills/graphify/`, `.graphifyignore` und einen Guard in `.claude/settings.json` hinzu. `graphify-out/` bleibt ungetrackt.

## Gate

- This PR is the adoption PR: G4 is skipped, every other rule runs.
- From the next PR on, source changes need an OpenSpec change (G4), oder das Label `no-spec` plus eine Zeile `No-spec reason:` im PR-Text.
- `master` ist nicht geschützt. Der Check meldet also Fehler, sperrt das direkte Mergen aber erst, wenn er als Pflicht-Check eingerichtet wird. Das wird am Ende der Migration entschieden.

## Open questions

none

## Approval

approved_by: human:Githubforben
approved_at: 2026-11-02T22:14:22+01:00
