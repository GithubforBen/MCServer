# Proposal

## Why

Shops only sell today, so a team that wants goods has to trade by hand and goods can never flow back to a shop. Buyback lets a team name a price it pays for an item, which also gives the planned lab goods a market without creating new bits.

## What Changes

- An offer can buy back: off by default, with its own price, set by the owning team.
- Customers sell one lot to a shop with a right click, paid from the team account, with the goods going into the stock chest.
- The marketplace shows who buys what and for how much, and selling works from its offer list.
- Existing shops load unchanged and keep selling only.

## Capabilities

### New Capabilities

- `shop-buyback`: selling goods to a shop, the conditions for it, and how it shows in the shop and the marketplace.

### Modified Capabilities

None.

## Impact

Survival plugin only: offers and their storage in `configs/shop-config.yml` (new optional keys), shop and editor menus, marketplace menus. Relies on [Bits](/domain/bits.md) and the [Survival-Plugin](/architecture/survival.md). No rejected decision covers this idea.
