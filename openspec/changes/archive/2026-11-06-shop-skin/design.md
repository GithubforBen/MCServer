# Design

## Context

`Shopkeeper` holds a `Villager`, and its adopt logic exists to keep exactly one per shop. The lobby's `Mannequin`s are not persistent and are rebuilt on every start; shops are persistent and adopted.

## Decisions

- **Entity field:** `Shopkeeper` holds a `LivingEntity`. The kind follows from `skin`: none means villager, set means mannequin. `shopIdOf` accepts both.
- **Same lifecycle for both:** the mannequin is persistent and adopted like the villager, marked with the same `SHOP_ID`. Rejected: rebuilding it on each load like the lobby, because a second lifecycle next to the adopt logic is how shops end up doubled.
- **Wrong kind found:** adopting a figure of the other kind, for example after the skin changed while the chunk was out, removes it and spawns the right one.
- **Input:** the existing `ChatPrompt`. Its `abbrechen` hint is already part of the prompt.
- **Name display:** mannequin with its custom name visible and description `null`, so the nameplate reads like a villager's.

## Risks

- `PlayerInteractEntityEvent` already works on mannequins in the lobby; attacks are covered by invulnerability, checked in game.
- An existing test world needs a check that the one-figure rule holds across a real restart.

## Commitments

None open.
