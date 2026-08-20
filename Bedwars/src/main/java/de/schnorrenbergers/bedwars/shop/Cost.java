package de.schnorrenbergers.bedwars.shop;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One part of a price.
 * <p>
 * Almost everything in the shop costs one kind of resource, and that is why the price used to be a
 * currency and a number. The bed token is the exception the game mode is built around - something that has
 * to be expensive in two currencies at once - so a price is a list, and the ordinary entry is a list of
 * one.
 * <p>
 * Paying is all or nothing: every part is checked before the first one is taken, or a player would end up
 * having handed over their diamonds for an item they could not afford.
 *
 * @param currency what is paid
 * @param amount   how much of it
 */
public record Cost(Currency currency, int amount) {

    /**
     * @param player who is paying
     * @return whether they have this part
     */
    public boolean covered(Player player) {
        return currency.count(player) >= amount;
    }

    /**
     * @param player who is paying
     * @return how much of it they are short
     */
    public int missing(Player player) {
        return Math.max(0, amount - currency.count(player));
    }

    /**
     * @param player who is paying
     * @param costs  the whole price
     * @return the first part they cannot cover, or {@code null} when they can pay
     */
    public static @Nullable Cost shortfall(Player player, List<Cost> costs) {
        for (Cost cost : costs) {
            if (!cost.covered(player)) return cost;
        }
        return null;
    }

    /**
     * Takes a whole price out of a player's pockets.
     *
     * @param player who is paying
     * @param costs  what it costs
     * @return whether it was taken; nothing is taken when it was not
     */
    public static boolean take(Player player, List<Cost> costs) {
        if (shortfall(player, costs) != null) return false;
        for (Cost cost : costs) {
            cost.currency().take(player, cost.amount());
        }
        return true;
    }
}
