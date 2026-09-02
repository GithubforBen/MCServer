package de.schnorrenbergers.survival.featrues.money;

import de.hems.paper.money.MoneyService;
import org.bukkit.Material;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

/**
 * The money of a player or a team.
 * <p>
 * This used to be the owner of the money: it read and wrote {@code configs/money-config.yml} next to the
 * survival server. The money now belongs to the launcher ({@link MoneyService}), so the lobby can sell
 * something for bits too, and this is what is left - the survival flavoured way of asking for it.
 * <p>
 * The signatures did not change, and neither did what they promise. {@link #removeMoney} still answers
 * right away and still says no when the account is short. It answers from this server's copy of the
 * balances rather than from the launcher, which is a guess in exactly one situation: the same account is
 * emptied on two servers within the same second. The launcher refuses the second change and pushes the
 * corrected balance back. For a shop where one player buys from one server that is the right trade;
 * anything that hands over something expensive should use {@link MoneyService#changeBlocking} instead.
 */
public class MoneyHandler {
    public static final Material MONEY_ITEM = Material.DIAMOND;

    public static void addMoney(int amount, UUID uuid) {
        MoneyService.change(MoneyService.holderOf(uuid), amount, false, "survival");
    }

    public static boolean removeMoney(int amount, UUID uuid) {
        return MoneyService.change(MoneyService.holderOf(uuid), -amount, true, "survival");
    }

    public static int getMoney(UUID uuid) {
        return MoneyService.get(uuid);
    }

    public static void addMoney(int amount, Team team) {
        MoneyService.change(MoneyService.holderOf(team), amount, false, "survival team");
    }

    public static boolean removeMoney(int amount, Team team) {
        return MoneyService.change(MoneyService.holderOf(team), -amount, true, "survival team");
    }

    public static int getMoney(Team team) {
        return MoneyService.get(team);
    }
}
