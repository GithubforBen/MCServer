---
type: Term
title: Signatur
description: Eine Signatur ordnet ein Item dauerhaft einem Spieler zu.
status: draft
generated: { by: claude-code/claude-opus-5-5, at: 2026-11-06T18:50:27+01:00 }
sources:
  - id: rules
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/SigningRules.java
  - id: command
    resource: ../../Survival/src/main/java/com/benschnorr/survival/featrues/signing/SignItemCommand.java
  - id: readme
    resource: ../../README.md
---

# Meaning

Eine Signatur ordnet ein Item dauerhaft einem Spieler zu. Gespeichert ist die UUID des Spielers in den Item-Daten (`survival:signed_by`); sichtbar ist die Zeile "Signiert von <Name>" in der Beschreibung. Der Name ist der zur Zeit des Signierens und folgt einer späteren Umbenennung nicht.

Es gibt weder Uhrzeit noch Zähler. Items derselben Sorte mit derselben Signatur sind deshalb identisch und stapeln sich, gleich wie oft sie signiert wurden, solange der Name gleich ist: zehn einzeln signierte Steine ergeben einen Stapel von zehn. Nach einer Umbenennung des Spielers tragen neue Signaturen den neuen Namen und stapeln sich nicht mit den alten. Eine Signatur ist endgültig, ein anderer Spieler kann sie nicht überschreiben.

Für Shops ist ein signiertes Item eine eigene Ware: es unterscheidet sich vom unsignierten und von der Signatur eines anderen Spielers.

# Not to be confused with

Die Signierung von Skin-Texturen bei Mojang (`skin-signature`) ist etwas anderes und hat mit Items nichts zu tun.

# Related

- [Items signieren](/capabilities/item-signing.md)
- [Shop-Rückkauf](/capabilities/shop-buyback.md)
