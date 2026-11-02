# Proposal

## Why

Survival has little to do with a long-term goal once a base stands. A deliberately slow, automatable production chain in the spirit of Schedule 1 gives teams a project and something to trade through shops, without touching anyone who ignores it.

## What Changes

- Brewing stands can turn sugar cane into paste, purify it through five purity tiers with crafted filters, and turn paste into Kokain of the same purity.
- Every brew can yield goods, waste or nothing, and the yield falls with each tier.
- The chain runs without a player: hoppers, crafters and chunk reloads all work.
- Vanilla blocks near a stand act as upgrades.
- Kokain can be cut with ingredients for effects, or stretched for quantity.
- Kokain can be consumed for effects with a crash afterwards, above vanilla strength only out of combat.
- Normal brewing, crafting and players who do not take part stay unchanged.

## Capabilities

### New Capabilities

- `lab-brewing`: the production chain in the brewing stand, its outcomes, waste, automation and persistence.
- `lab-upgrades`: blocks near a stand that change how it brews.
- `lab-cutting`: adding effects to Kokain and stretching it.
- `lab-consumption`: consuming Kokain, its effects, the crash and the combat limit.

### Modified Capabilities

None.

## Impact

Survival plugin only: a new feature package, a config file `configs/lab.yml`, crafting recipes, and a `/labor` command. Trading uses shops, where `shop-buyback` helps but is not required. The rule from [Gadget-Entscheidung](/decisions/gadget-choices.md) that a gadget is never a game advantage concerns gadgets, not this consumable; the combat limit keeps fights fair the same way. No rejected decision covers this idea.
