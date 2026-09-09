package de.schnorrenbergers.poker.bank;

import de.hems.paper.PaperContext;
import de.hems.paper.money.MoneyService;
import de.hems.paper.poker.PokerStatsService;
import de.hems.types.money.BalanceResult;
import de.hems.types.poker.PokerStatsData;
import de.schnorrenbergers.poker.CasinoContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The one door between the bits of the network and the chips on a table.
 * <p>
 * Nothing else in this plugin touches money. That is the whole design: chips are moved around by the rules
 * of poker, which know nothing about accounts, and they only turn into bits here - once on the way in and
 * once on the way out.
 * <p>
 * <b>In blocks, out does not.</b> A buy-in waits for the launcher to confirm the bits are really gone
 * before any chips exist, because handing over chips for money that was never taken is how a table prints
 * money. A cash-out is a credit: it cannot fail for lack of cover, and making somebody wait at a loading
 * screen for their own winnings would be worse than posting it and moving on.
 * <p>
 * <b>What the launcher knows.</b> Every chip that is sitting in front of somebody is reported as an open
 * stack. That is what makes this survivable: if this server dies mid-hand, the chips are not lost with it -
 * the launcher hands them back when the night is settled, because it already knew they were there.
 */
public final class Bank {

    /** The rows of this night, by the account they belong to. */
    private static final Map<UUID, PokerStatsData> rows = new ConcurrentHashMap<>();
    /** What is currently in front of each account, across every table. */
    private static final Map<UUID, Integer> openStacks = new ConcurrentHashMap<>();

    private Bank() {
    }

    /**
     * Takes bits and answers with chips.
     * <p>
     * Runs the debit in the background and reports back on the main thread, because the caller is a click
     * and the launcher is a network round trip.
     *
     * @param player   who is buying in
     * @param bits     what it costs
     * @param reason   what it is for, for the launcher's log
     * @param callback how many chips they got, on the main thread. Zero means it did not go through, and
     *                 the reason has already been said to the player
     */
    public static void buyIn(Player player, int bits, String reason, Consumer<Integer> callback) {
        buyFor(player, player.getUniqueId(), player.getName(), bits, reason, callback);
    }

    /**
     * Takes bits from one account and answers with chips for something else - which is what putting a bot
     * down is: the chips are the bot's, the money is its owner's.
     *
     * @param payer       who is charged, for the message
     * @param account     the account to charge
     * @param accountName the name to record it under
     * @param bits        what it costs
     * @param reason      what it is for
     * @param callback    how many chips came out, on the main thread
     */
    public static void buyFor(Player payer, UUID account, String accountName, int bits, String reason,
                              Consumer<Integer> callback) {
        if (bits <= 0) {
            callback.accept(0);
            return;
        }
        PaperContext.async(() -> {
            BalanceResult result = MoneyService.changeBlocking(MoneyService.holderOf(account), -bits,
                    true, reason);
            PaperContext.sync(() -> {
                if (!result.isSuccessful()) {
                    if (payer != null && payer.isOnline()) {
                        payer.sendMessage(org.bukkit.ChatColor.RED + "Das ging nicht: "
                                + (result.getMessage() == null ? "zu wenig Bits." : result.getMessage()));
                    }
                    callback.accept(0);
                    return;
                }
                row(account, accountName).addBoughtIn(bits);
                push(account);
                callback.accept(bits);
            });
        });
    }

    /**
     * Turns chips back into bits.
     * <p>
     * Posted rather than waited for, and retried by {@link MoneyService} underneath. The stack is taken out
     * of the open stacks first: from this moment the chips are money that has been paid, and the launcher
     * must not hand them back a second time when the night is settled.
     *
     * @param account     whose money it is
     * @param accountName the name to record it under
     * @param chips       how many
     * @param reason      what it was
     */
    public static void cashOut(UUID account, String accountName, int chips, String reason) {
        if (chips <= 0) return;
        MoneyService.change(MoneyService.holderOf(account), chips, false, reason);
        row(account, accountName).addCashedOut(chips);
        push(account);
    }

    /**
     * Reports what somebody has in front of them right now.
     *
     * @param account     whose it is
     * @param accountName the name to record it under
     * @param chips       what is on the table for them, over every seat they hold
     */
    public static void setOpenStack(UUID account, String accountName, int chips) {
        int before = openStacks.getOrDefault(account, 0);
        if (before == chips) return;
        openStacks.put(account, Math.max(0, chips));
        PokerStatsData row = row(account, accountName);
        row.setOpenStack(Math.max(0, chips));
        push(account);
    }

    /**
     * Writes down that a hand was played.
     * <p>
     * Only for the person sitting there, never for a bot: the money a bot plays with belongs to whoever put
     * it down, but the hands do not. Counting them would let somebody clear the qualifying bar for the
     * ranking by letting three bots play the evening for them.
     *
     * @param account     whose row it is
     * @param accountName the name to record it under
     * @param won         whether they won the pot
     * @param pot         how big it was
     */
    public static void recordHand(UUID account, String accountName, boolean won, int pot) {
        row(account, accountName).addHand(won, pot);
        push(account);
    }

    /**
     * @param account the account
     * @return what has been paid in for it over the night
     */
    public static int boughtIn(UUID account) {
        PokerStatsData row = rows.get(account);
        return row == null ? 0 : row.getBoughtIn();
    }

    /**
     * @param player somebody here
     * @return what they have in bits, out of the copy this server keeps
     */
    public static int balanceOf(Player player) {
        return MoneyService.get(player.getUniqueId());
    }

    private static PokerStatsData row(UUID account, String accountName) {
        return rows.computeIfAbsent(account, id -> {
            PokerStatsData fresh = new PokerStatsData(
                    CasinoContext.hasEvent() ? CasinoContext.getEvent().getId() : null, id, accountName);
            // a night that was interrupted has a row already, and starting a new one would forget what
            // somebody put in before the crash - which is exactly what must not be forgotten
            PokerStatsData known = CasinoContext.hasEvent()
                    ? PokerStatsService.getRow(CasinoContext.getEvent().getId(), id) : null;
            return known == null ? fresh : known.copy();
        });
    }

    /**
     * Sends a row to the launcher.
     * <p>
     * Silently does nothing for a house table. There is no night to record against, and inventing an id for
     * one would put rows into the file that no event will ever settle or clean up.
     */
    private static void push(UUID account) {
        if (!CasinoContext.hasEvent()) return;
        PokerStatsData row = rows.get(account);
        if (row == null) return;
        row.setEventId(CasinoContext.getEvent().getId());
        PokerStatsService.save(row);
    }

    /**
     * Hands everything on every table back, which is what a casino does when it is switched off in an
     * orderly way.
     * <p>
     * The launcher would do it anyway when the night is settled, out of the open stacks. Doing it here as
     * well means that in the ordinary case - the night ends, the server stops - nobody has to wait for the
     * settlement to see their money.
     *
     * @param stacks who has how much, over every seat
     */
    public static void payOutEverything(Map<UUID, StackEntry> stacks) {
        for (Map.Entry<UUID, StackEntry> entry : stacks.entrySet()) {
            StackEntry stack = entry.getValue();
            if (stack.chips() <= 0) continue;
            cashOut(entry.getKey(), stack.name(), stack.chips(), "Poker: Casino geschlossen");
            Bukkit.getLogger().info("Paid " + stack.chips() + " chips back to " + stack.name()
                    + " because the casino is closing.");
        }
        for (UUID account : stacks.keySet()) {
            setOpenStack(account, stacks.get(account).name(), 0);
        }
    }

    /**
     * What one account has sitting on the tables.
     *
     * @param name  who it is, for the log
     * @param chips how much
     */
    public record StackEntry(String name, int chips) {
    }
}
