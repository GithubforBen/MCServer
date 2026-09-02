# Gadget-Vorschläge

Stand: 2026-09-02. 18 vorgeschlagene Gadgets, aufgeteilt nach dem Server, auf dem sie laufen
sollen. Freigegeben, aber noch nicht umgesetzt - diese Datei ist die Vorlage für die
Implementierung, kein Zustandsbericht.

## Wo das heute steht

Ein Cosmetic ist zwei Hälften: der Katalogeintrag in `de.hems.types.cosmetic.Cosmetics` (der
beim Launcher in `cosmetics.yml` landet) und der Code, der es auf dem Spielserver spielt. Bei
den Gadgets existiert bisher genau eines - `endless-pearl` - und der Code dazu liegt allein im
Bedwars-Plugin (`Bedwars/.../cosmetic/GadgetListener.java`).

Das heißt für diese Liste: **LobbyPlugin und Survival haben aktuell keinen Cosmetic-Code.**
Beide brauchen ein eigenes Gegenstück zu `GadgetListener`, bevor dort auch nur ein Gadget läuft.

## Was vorher umgebaut werden muss

### Ein Gadget-Slot pro Server

`CosmeticType.GADGET` heißt heute: ein Spieler hat genau ein Gadget angelegt, netzwerkweit. Wer
sich ein Lobby-Gadget anlegt, hätte damit auf Survival und in Bedwars keins mehr. Entschieden ist
deshalb: **je ein Slot für Lobby, Survival und Bedwars.**

Das ist der größere Teil der Arbeit und betrifft:

- `PlayerCosmetics` - die Auswahl hängt heute am `CosmeticType`, künftig zusätzlich am Server
- `CosmeticService.getSelected` / `selectAsync` - beide brauchen den Server-Kontext
- `CosmeticsUi` - der Shop muss zeigen, für welchen Server ein Gadget gilt, und darf ein
  Lobby-Gadget nicht gegen ein Survival-Gadget tauschen
- das `cosmetics.yml`-Format beim Launcher, samt Übernahme des bestehenden Besitzes

Der Kaufweg selbst (Preis lesen, Bits abbuchen, gutschreiben, alles unter einem Lock im Launcher)
bleibt unverändert.

### Aufräumen von Entities

Reittier, Haustier, Ballon und Stuhl legen Entities in die Welt. Logout, Warp, Serverwechsel und
ein Absturz zwischendurch müssen alle vier sauber wegräumen, sonst stehen irgendwann Pferde und
Armorstands in der Lobby herum. Das ist bei diesen Gadgets der wahrscheinlichste Bug, nicht der
Effekt selbst.

## Nur Lobby (7)

| Vorschlag | id | Was es macht | Worauf zu achten ist |
|-----------|-----|--------------|----------------------|
| Doppelsprung | `double-jump` | In der Luft ein zweiter Sprung mit Boost, Landung mit Slow Falling | Kein Fallschaden, Flugmodus-Trick sauber zurücksetzen |
| Raketenstiefel | `rocket-boots` | Feuerwerk schleudert den Spieler hoch, Slow Falling beim Fallen | Reicht auf Dächer - braucht eine Höhen- oder Zonengrenze |
| Schneeball-Kanone | `snowball-cannon` | Schneebälle schubsen andere Spieler weg, kein Schaden | Nicht im Parkour, sonst wirft man andere aus dem Lauf |
| Disco-Boden | `disco-floor` | Blöcke unter dem Spieler leuchten kurz farbig auf | Nur als Block-Change-Paket an nahe Spieler, kein echter Blockwechsel |
| Reittier | `lobby-mount` | Beschworenes Reittier (Pferd, Strider, Schwein) | Verschwindet beim Warp und Logout, siehe Aufräumen |
| Sprungpad-Ei | `jump-pad` | Legt ein temporäres Sprungpad, das hochschleudert | Pad verschwindet nach wenigen Sekunden von selbst |
| Fußspuren | `footsteps` | Kurzlebige Abdrücke am Boden beim Laufen | Thematisch nah an der Partikelspur, im Shop klar trennen |

## Nur Survival (3)

| Vorschlag | id | Was es macht | Worauf zu achten ist |
|-----------|-----|--------------|----------------------|
| Erntehelfer | `harvest-helper` | Rechtsklick auf eine reife Pflanze erntet und pflanzt neu | Bequemlichkeit, kein Ertragsplus - der Drop bleibt der normale |
| Sitzen | `sit` | Hinsetzen auf Treppen und Stufen | Sitz-Entity beim Ausloggen entfernen |
| Mobile Werkbank | `mobile-workbench` | Öffnet unterwegs eine Werkbank | Bewusst nur Werkbank: Amboss und Ofen wären zu stark |

## Lobby und Survival (8)

| Vorschlag | id | Was es macht | Worauf zu achten ist |
|-----------|-----|--------------|----------------------|
| Enterhaken | `grappling-hook` | Angel werfen, der Haken zieht den Spieler zum Trefferpunkt | Cooldown in den Settings (`cooldown-ticks`), kein Fallschaden nach dem Zug |
| Partikelspur | `particle-trail` | Farbige Spur hinter dem laufenden Spieler | Eigentlich ein eigener Cosmetic-Typ (siehe TODO.md), läuft hier als Gadget |
| Haustier-Begleiter | `pet-companion` | Kleiner Begleiter, der folgt: unverwundbar, ohne Kollision | Auf Survival kein Schild und kein Mob-Blocker |
| Konfetti-Kanone | `confetti-cannon` | Konfetti und Sound auf Rechtsklick | Reine Optik, damit auch auf Survival unkritisch |
| Persönliches Wetter | `personal-weather` | Tageszeit und Wetter nur für den Spieler selbst | `setPlayerTime` / `setPlayerWeather`, andere merken nichts |
| Chat-Blase | `chat-bubble` | Die eigene Chatnachricht steht kurz über dem Kopf | Ein Display-Entity pro Spieler, der aufwendigste Eintrag der Liste |
| Ballon | `balloon` | Ein Ballon schwebt an einer Leine über dem Spieler | Siehe Aufräumen von Entities |
| Emote-Rad | `emote-wheel` | Menü mit Gesten: Winken, Jubeln, Verbeugen | Ohne echtes Animationssystem bleibt es Partikel, Sound und Pose |

## Offene Fragen

- Preise in Bits stehen noch keine fest. Der Vergleichswert ist die Endlos-Perle mit 5000.
- Der Erntehelfer ist das einzige Gadget der Liste, das auf Survival in den Spielablauf eingreift.
  Wenn sich das als zu bequem zeigt, gehört ein Verbrauch oder ein Cooldown in die Settings.
- Ob Bedwars über die Endlos-Perle hinaus eigene Gadgets bekommt, ist nicht entschieden. Der
  dritte Slot ist im Umbau aber vorgesehen.
