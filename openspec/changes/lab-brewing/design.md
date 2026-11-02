# Design

## Context

Minecraft 26.3 brews through data-driven `minecraft:brewing` recipes. `BrewingStandBlockEntity.isBrewable` needs a bottom slot whose item matches a recipe, so vanilla never starts on empty slots. Hoppers and menus ask two item-level sets: reagents for the top slot and `BREWING_INPUTS` plus a tag for the bottom. Paper exposes `org.bukkit.inventory.BrewingRecipe`, `BrewEvent` with changeable results, `BrewingStandFuelEvent`, and block persistent data.

## Decisions

- **Two paths, one rule table.** Brews with something in a bottom slot (purifying, Kokain, cutting) run on the vanilla engine: a registered `BrewingRecipe` starts them, and a `BrewEvent` handler replaces the results by rolling. Only sugar cane on empty slots needs our own tick. Rejected: our own tick for everything, because vanilla already gives progress, sound and fuel handling for free where it can.
- **Own tick for sugar cane.** A task once per second walks the stands known to be active. Stands enter on inventory changes and on chunk load, leave on chunk unload. Progress lives in the block's persistent data and is written each step, so unloading and crashes need no separate save.
- **Rolling happens at the end.** Ingredients go when a brew finishes, like vanilla. A crash can repeat at most the last seconds of a brew, and never an outcome.
- **Registering reagents.** Sugar cane, filters and Feenstaub get a recipe so the top slot accepts them, using an input item that never occurs, so vanilla cannot start a brew from them.
- **Lab items** use base materials without vanilla recipes and the `item_model` component for their look, with name, lore and purity in persistent data. So they neither need a resource pack nor pass as paper or sugar.
- **Hopper rule without per-slot state.** `InventoryMoveItemEvent` refuses to take an item out of a bottom slot while the top ingredient is a valid step for it. Results are never a valid step for the same ingredient, so no flags per slot are needed. Stretched Kokain carries a mark so sugar is not a valid step for it.
- **Numbers** live in `configs/lab.yml`, written with defaults on first start, like `ShopSettings`.
- **Combat** is new: Survival has no combat tag. A small tracker listens to player-against-player damage.

## Risks and the first task

- **Empty slots are the biggest risk.** Vanilla resets a brew it does not consider brewable, so the progress arrow may need sending to viewers by hand, and hoppers may not treat this cleanly. Group 1 proves this on a real server before anything is built on it. If it does not hold up, the fallback the user chose is glass bottles, with `BrewingRecipe` for sugar cane too. That is a behavior change: stop, ask, update brief and spec.
- A cancelled hopper move may end that hopper's turn instead of trying the next slot. Group 1 tests this.
- Whether recipes registered at runtime reach the item-level sets is unverified. If not, they go in through a datapack at bootstrap.

## Commitments

None open.
