# Design

## Context

Shops already match goods with `ItemStack.isSimilar`, which compares the whole item data, so a signature only has to live in the item's data to make signed goods distinct goods.

## Decisions

- **Signature data:** the signer's UUID in persistent data under one key, plus one lore line "Signiert von <Name>". No timestamp or counter, so signings by the same player are identical. Rejected: the name alone, because names change and can be reused.
- **Pure rules:** whether a stack may be signed and what the outcome is (signed, already yours, someone else's, not signable, empty hand) is a small class without Bukkit types, tested by a plain `main` like the `*Check` classes elsewhere.
- **Blocking ingredients:** `PrepareItemCraftEvent` clears the result for the crafting table and the crafter; `CrafterCraftEvent` is cancelled for the crafter; `FurnaceSmeltEvent` and `FurnaceStartSmeltEvent` stop smelting; `PrepareSmithingEvent` and `PrepareAnvilEvent` clear the result where a signed item would be consumed. The lab's brew skips signed items through the lab's own rule table once that change lands.
- **Not signable:** items carrying the gadget, cosmetic or menu markers that already exist in `CommonCode`.

## Risks

- A crafter whose only matching items are signed stays idle. That is the intended outcome, but a player may not see why; the description line explains it.
- The lab hook depends on the lab change; if this change lands first, the lab change adds the check.

## Commitments

None open.
