# Design

## Context

`Shopkeeper.buyItem` takes money from the local copy with `MoneyHandler`, which the class comment of `MoneyService` allows for one player on one server. Buyback pays out of a shared team account, and expensive goods are the point of it, so a guess is not good enough here.

## Decisions

- **Payment:** the team account is debited with `MoneyService.changeAsync` (`requireCover = true`), which waits for the launcher and calls back on the main thread. Rejected: `MoneyHandler.removeMoney`, because two sellers could both be told yes.
- **Order of a sale:** on the main thread check everything and take the lot out of the player's inventory; debit asynchronously; on the callback put the lot into the chest and pay the player. If the debit fails, the lot goes back to the player. If the chest filled up in between, the lot goes back and the team is refunded. A player who logged off meanwhile gets the goods dropped at the shop's chest rather than lost, like `buyItem` does with spills.
- **Rules are pure:** whether a sale may start lives in a small class without Bukkit types so it can be tested by a plain `main`, like the `*Check` classes in `PokerPlugin`.
- **Storage:** new optional keys `buyback.enabled` and `buyback.price` under each offer in `shop-config.yml`. Missing means off, which is the whole migration.

## Risks

- A member of the owning team can drain the team account; accepted by the user (brief Q5).
- Selling from the marketplace may load the chest's chunk, as buying from it already does.
- A server that stops while a sale waits for the launcher can lose the reserved lot, because the callback no longer runs. The window is at most the launcher timeout; accepted and named here rather than fixed.

## Commitments

None open.
