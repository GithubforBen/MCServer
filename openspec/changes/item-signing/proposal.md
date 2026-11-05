# Proposal

## Why

Players cannot mark what they made as theirs, so a crafter's goods look like anyone's once they change hands. A signature lets a player build a name for their goods and sell them through shops as their own product.

## What Changes

- `/signItem` signs the stack in the main hand with the player's name, for free.
- Items signed by the same player are identical and stack; a signature cannot be changed or replaced.
- Signed items are never consumed as an ingredient, so a signature is never lost silently.
- Shops sell and buy back signed goods like any other item, as distinct from unsigned ones.

## Capabilities

### New Capabilities

- `item-signing`: signing items, what a signature means, and where signed items may and may not be used.

### Modified Capabilities

None.

## Impact

Survival plugin only: a new command, a small feature package and listeners on crafting, smelting, smithing, the anvil and, once it exists, the lab's brewing. Works with the shops of `shop-buyback` without changing them. No rejected decision covers this idea.
