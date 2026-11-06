# Design

## Context

Shops already match goods with `ItemStack.isSimilar`, which compares the whole item data, so a signature only has to live in the item's data to make signed goods distinct goods.

## Decisions

- **Signature data:** the signer's UUID in persistent data under one key, plus one lore line "Signiert von <Name>". No timestamp or counter, so signings by the same player are identical. Rejected: the name alone, because names change and can be reused.
- **Pure rules:** whether a stack may be signed and what the outcome is (signed, already yours, someone else's, not signable, empty hand) is a small class without Bukkit types, tested by a plain `main` like the `*Check` classes elsewhere.
- **Blocking ingredients:** `PrepareItemCraftEvent` clears the result at the crafting table; `CrafterCraftEvent` is cancelled for the crafter; `FurnaceBurnEvent` is cancelled so no fuel burns for a signed input, and `BlockCookEvent` stops smelting in furnace, blast furnace, smoker and campfire; `PrepareSmithingEvent` and `PrepareAnvilEvent` clear the result where a signed item would be consumed. Stonecutter, grindstone and vanilla potion brewing were added after verification (user decision 2026-11-06). The lab's brew skips signed items through its own rule table.
- **Rename:** the lore keeps the name the player had when signing (user decision 2026-11-06); after a rename, new signatures show the new name and do not stack with older ones.
- **Not signable:** items carrying the gadget, cosmetic or menu markers that already exist in `CommonCode`.

## Risks

- A crafter whose only matching items are signed stays idle. That is the intended outcome, but a player may not see why; the description line explains it.
- The lab hook depends on the lab change; if this change lands first, the lab change adds the check.

## Commitments

None open.
