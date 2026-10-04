# Gadgets: was beschlossen wurde und was daraus geworden ist

Stand: 2026-09-03. Diese Datei ist die Entscheidungsspur zu den 18 freigegebenen Gadgets. Was sie
heute tun und wo sie wirken, steht im README unter „Cosmetics" - hier steht, warum sie so
aussehen, und was von der Liste anders gebaut wurde als besprochen.

## Die Liste

Freigegeben wurden 18 Vorschläge, aufgeteilt nach dem Server, auf dem sie laufen sollen: sieben nur
Lobby, drei nur Survival, acht auf beiden. Umgesetzt sind sie alle. Drei Punkte weichen ab:

- **Der Enterhaken war schon da.** Er kam mit der Cosmetic-Runde auf `master` dazu, bevor diese
  Liste umgesetzt wurde. Er hat hier nur seine Slots bekommen - Lobby und Survival. In Bedwars lief er
  anfangs auch, das ist wieder raus: ein Gadget darf kein Spielvorteil sein.
- **Die Partikelspur ist kein Gadget.** Es gibt inzwischen den Cosmetic-Typ `TRAIL` und drei Spuren
  darin. Sie als Gadget ein zweites Mal zu bauen, hätte dasselbe zweimal verkauft.
- **Sprungpad und Fußspuren sind Lobby-only**, wie nachträglich entschieden - nicht auf beiden
  Servern, wie ursprünglich vorgeschlagen.

## Ein Slot pro Server

Vorher galt: ein Spieler trägt genau ein Gadget, netzwerkweit. Damit hätte der Doppelsprung in der
Lobby den Erntehelfer auf Survival abgelegt. Jetzt gibt es `GadgetSlot` mit `LOBBY`, `SURVIVAL` und
`BEDWARS`, und ein Slot hält je ein Gadget. `BEDWARS` bleibt leer - die Bedwars-Server schalten Gadgets
nicht ein, der Slot ist nur noch da, damit gespeicherte Auswahlen lesbar bleiben.

Angefasst dafür:

- `PlayerCosmetics` speichert die Auswahl unter `GADGET_<SLOT>` statt unter `GADGET`. Eine ältere
  Auswahl unter dem bloßen `GADGET` gilt in jedem Slot weiter und wird beim ersten Ändern auf alle
  Slots aufgeteilt - niemand verliert beim Update, was er anhatte.
- `SelectCosmeticEvent`, `CosmeticService.getSelected`/`selectAsync` und `CosmeticStore.select`
  führen den Slot mit. Das Format der `cosmetics.yml` ändert sich dabei nicht, nur die Schlüssel
  darin.
- `Gadget.slots()` sagt, wohin ein Gadget gehört. Der Shop liest das und schreibt es in die
  Beschreibung, ein Klick legt in alle Slots des Gadgets gleichzeitig an.

## Was beim Bauen aufgefallen ist

- **Items sammeln sich.** In der Lobby wird beim Join, beim Respawn und beim Weltwechsel ausgeteilt,
  und niemand räumt dazwischen ein Inventar auf. `Gadgets.handOut` gibt deshalb nichts, was der
  Spieler schon hat, und `Gadgets.stop` nimmt die Items wieder mit.
- **Der Erntehelfer hätte Saatgut gedruckt.** `breakNaturally` plus Neupflanzen ist pro Ernte ein
  Samen mehr, als Ernten und Pflanzen von Hand kostet. Er zieht jetzt genau einen Samen von den
  Drops ab und meldet den Abbau als `BlockBreakEvent` an, damit geschützte Regionen nein sagen
  können.
- **Nichts wird gespeichert, was gespawnt wird.** Ballon, Haustier, Pferd und Sitz sind
  `persistent = false` und werden über `GadgetEntities` geführt, das sie beim Ablegen, beim
  Weltwechsel und beim Ausloggen einsammelt.

## Offen

- Preise sind gesetzt, aber nicht erprobt: 800 bis 5000 Bits, mit der Endlos-Perle bei 5000 als
  oberem Anker. Ob das Verhältnis stimmt, zeigt erst der Betrieb.
- Der Erntehelfer ist das einzige Gadget der Liste, das auf Survival in den Spielablauf eingreift.
  Wenn er sich als zu bequem zeigt, gehört ein Cooldown in seine Settings - der ist noch nicht da.
- Das Emote-Rad bewegt den echten Spieler: jedes Emote ist eine Choreografie aus dem, was ein Server
  einem Körper auftragen kann (Arm schwingen, ducken, hüpfen, Riptide-Drehung, flach hinlegen), auf den
  Tick genau. Einzelne Gliedmaßen frei zu stellen geht ohne Resource Pack nicht. Ducken und Hinlegen sieht
  man an sich selbst nicht, nur die anderen sehen es - der eigene Client bestimmt die eigene Haltung.
  Zehn Sekunden nach einem Treffer startet kein Emote, und ein Treffer bricht es ab.
- In Bedwars gibt es keine Gadgets: ein Gadget darf nie ein Spielvorteil sein. Endlos-Perle und Enterhaken
  wirken nur noch in Lobby und Survival, und die Bedwars-Server schalten Gadgets gar nicht erst ein.
