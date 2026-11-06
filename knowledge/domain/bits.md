---
type: Term
title: Bits
description: "Die Währung des Netzwerks. Sie gehört dem Launcher, die Server halten nur Kopien."
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-02T22:20:03+01:00 }
sources:
  - id: service
    resource: ../../CommonCode/src/main/java/com/benschnorr/paper/money/MoneyService.java
  - id: readme
    resource: ../../README.md
---

# Meaning

Die Währung des Netzwerks. Sie liegt beim Launcher in `money.yml`. Jede Änderung ist eine Differenz und keine neue Summe und wird unter einem Lock angewandt, damit sich zwei gleichzeitige Auszahlungen addieren. Server antworten aus ihrer lokalen Kopie; wo an der Antwort etwas Wertvolles hängt, wie beim Cosmetic-Kauf, gibt es `MoneyService.changeBlocking`. Diese Abbuchung trägt eine Frist, kurz vor dem Ende der Wartezeit des Senders; kommt der Launcher erst danach dazu, verwirft er sie und antwortet mit einem Fehler, sodass keine Bits mehr bewegt werden, wenn niemand mehr wartet. Gesendete Änderungen ohne Frist gelten immer.

Ein Teamkonto hängt am Teamnamen. Bei einer Umbenennung zieht es mit, bei einer Auflösung bekommt der Anführer den Kontostand.

# Not to be confused with

- Chips: am Pokertisch ist ein Chip genau ein Bit, ohne Umrechnungskurs.

# Related

- [Rückkauf zahlt die Teamkasse](/decisions/shop-buyback-paid-by-team.md)
- [Launcher](/architecture/launcher.md)
- [Poker-Plugin](/architecture/poker.md)
