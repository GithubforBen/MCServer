# Proposal

## Why

A shop looks like every other villager on the server. Teams want their shops to have a face, and the lobby npcs already show that real skins work without any plugin.

## What Changes

- The owning team can give a shop a player skin in the shop editor, and take it away again.
- A shop with a skin stands as a mannequin. Everything else about it behaves as before.
- Shops without a skin stay villagers.

## Capabilities

### New Capabilities

- `shop-skin`: giving a shop a player skin, and how such a shop behaves in the world.

### Modified Capabilities

None.

## Impact

Survival plugin only: the shop entity, its lifecycle, the editor and an optional `skin` key in `configs/shop-config.yml`. Builds on the approach of the [Lobby-Plugin](/architecture/lobby.md). Independent of `shop-buyback`. No rejected decision covers this idea.
