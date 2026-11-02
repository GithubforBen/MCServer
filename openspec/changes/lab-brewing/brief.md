## Wish

"Ein spielerisches System wie Schedule 1: Man kann Kokain herstellen, und das ist richtig schwierig und dauert richtig lange. Zuckerrohr ist die Grundsubstanz, hergestellt wird im Braustand. Es gibt unterschiedliche Reinheitsgrade, und für den höchsten muss man durchschnittlich 20.000 Zuckerrohr verarbeiten. Es soll automatisch machbar sein, nicht alles per Hand. Unten kommen drei Outputs: Ware in geringer Reinheit, die man weiter purifizieren kann, Abfall, den man aussortieren muss, oder gar nichts. Purifizieren braucht Filter aus Papier. Je reiner, desto geringer der Yield. Es darf sich nicht an der Realität orientieren, und es soll lustig sein und sich upgraden lassen. Es läuft auf Survival, muss vanilla genug bleiben und ein Addon sein, kein neuer Spielmodus. Wer nicht interagieren will, muss nicht und wird nicht beeinträchtigt."

## Decisions

| # | Question | Answer | Source |
| --- | --- | --- | --- |
| Q1 | What lies in the three bottom slots at the start? | Nothing: truly empty slots, proven by many tests around chunk unloading. If that does not hold up, fall back to glass bottles | user |
| Q2 | How much sugar cane does a brew use? | One per free output slot, each slot rolls on its own | user |
| Q3 | What makes it more fun? | Cutting, consumption with effects, lab upgrades. No raids or accidents | user |
| Q4 | How does it reach players? | As a vanilla-feeling addon on Survival only; people who do not take part notice nothing | user |
| Q5 | How does it become bits? | Only through player shops, no NPC buyer, no new bits | user |
| Q6 | How long is a brew? | 20 seconds, configurable | user |
| Q7 | How are upgrades installed? | Vanilla blocks near the stand | user |
| Q8 | How far does cutting go? | Up to 3 effect ingredients per item, order irrelevant, purity unchanged, and additionally stretching: one item becomes three, one purity lower | user |
| Q9 | How strong may consumption be? | Above vanilla potions only out of combat | user |
| Q10 | What must the waste not do? | Be placed as a block | user |
| Q11 | Who does the 20,000 sugar cane apply to? | A lab without upgrades; fully upgraded about 6,400 | user |
| Q12 | Tree and numbers | As presented: five purity tiers, four filters, Feenstaub, the yields in the spec | user |
| Q13 | What counts as combat? | Dealing or taking a hit from another player within the last 15 seconds | delegated |
| Q14 | What happens to above-vanilla effects in combat? | They drop to the vanilla cap and stay there until they run out | delegated |
| Q15 | How far do upgrades reach? | Two blocks around the stand, because hoppers need the faces next to it | delegated |
| Q16 | How do players learn the recipes? | `/labor` shows the tree, and lab items name their next step | delegated |

## Out of scope

- Raids and lab accidents (backlog).
- An NPC buyer creating new bits (backlog).
- New blocks, resource packs, or any effect outside Survival.

## Open questions

## Confirmation

confirmed_by: human:Githubforben
confirmed_at: 2026-11-02T23:14:11+01:00
