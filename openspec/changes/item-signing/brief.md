## Wish

"Ich will, dass Spieler mit /signItem ein Item signieren können. Dort wird in die Lore einfach der Name geschrieben. Dadurch kann ein Spieler Items herstellen, die ihm genau zuzuordnen sind. Wenn ein Spieler Cobblestone 10 mal signiert, ist er identisch. Shopkeeper sollen das verkaufen können. Muss getestet werden. Beim Auto-Craften soll, wenn es geht, dieses Item nicht genommen werden."

## Decisions

| # | Question | Answer | Source |
| --- | --- | --- | --- |
| Q1 | What if an item is already signed by someone else? | Refused: a signature is final. Signing again by the same player changes nothing | user |
| Q2 | Where must signed items not be used as an ingredient? | Nowhere in crafting: not in the crafter, the crafting table, furnaces or the smithing table | user |
| Q3 | What does signing cost? | Nothing | user |
| Q4 | When is it built? | After the lab, on its own branch | user |
| Q5 | What gets signed? | The whole stack in the main hand | delegated |
| Q6 | How is the player stored? | By UUID in the item's data; the lore shows the name at signing time and does not follow a later rename, so two signings by the same player stay identical | delegated |
| Q7 | Do lab brews and the anvil count as crafting? | Yes for lab brews. In the anvil a signed item may be the left item, where the result keeps the signature, but not the sacrifice on the right | delegated |
| Q8 | Which items cannot be signed? | Items that carry a function of the network, such as gadgets, cosmetic items and menu items | delegated |
| Q9 | Where does it work? | On Survival only | delegated |

## Out of scope

- Removing a signature.
- Signing with a cost or a limit.
- Showing signatures anywhere but in the item's own description.

## Open questions

## Confirmation

confirmed_by: human:Githubforben
confirmed_at: 2026-11-05T23:25:55+01:00
