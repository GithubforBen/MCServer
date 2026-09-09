package de.schnorrenbergers.poker.bot;

import de.schnorrenbergers.poker.game.Action;
import de.schnorrenbergers.poker.game.Card;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import de.schnorrenbergers.poker.game.Street;

import java.util.List;
import java.util.Random;

/**
 * What a bot does with its turn.
 * <p>
 * The brief was a bot that is neither of the two bots everybody has played against. One folds everything,
 * including the hands it should be raising, and playing it is free money that feels like nothing. The other
 * moves all-in with any two cards, and playing it is a coin flip that feels like being cheated. Both are
 * easy to write, which is why both are everywhere.
 * <p>
 * What is here instead is a bot that decides on the same two numbers a person decides on: how often this
 * hand wins from here ({@link Equity}), and what the pot is offering to find out. Above that price it goes
 * on, below it it does not, and around the edges it is inconsistent on purpose - a table where every bot
 * plays the same way is a table you beat once and then beat forever.
 * <p>
 * <b>Two hard rules sit above all of it</b>, because they are the two failures that make a bot table not
 * worth sitting at:
 * <ul>
 *   <li>It never folds a hand that is winning. Below {@link #NEVER_FOLD_ABOVE} it might, above it it never
 *       does, whatever its mood.</li>
 *   <li>It never puts a whole stack in on a hand that is losing. All-in needs either a real hand or a stack
 *       so short that folding costs more than shoving, and never both missing.</li>
 * </ul>
 * <p>
 * <b>It is not built to win.</b> The house takes its cut of every contested pot, so a bot that broke even
 * against people would drain the table through the rake alone. It plays a little looser than the maths
 * would - see {@link #LOOSENESS} - which is roughly what it costs to give the rake back.
 */
public final class BotBrain {

    /** A hand this strong is never folded, however tight the bot is feeling. */
    private static final double NEVER_FOLD_ABOVE = 0.68d;
    /** And a hand below this never goes all-in unless the stack is already too short to matter. */
    private static final double NEVER_SHOVE_BELOW = 0.62d;
    /** A stack this short, in big blinds, is one where waiting costs more than playing. */
    private static final int SHORT_STACK_BB = 9;
    /** How much of the maths the bot gives away, which is roughly what the house takes. */
    private static final double LOOSENESS = 0.93d;
    /** A call that costs less than this share of the stack is made on a hand that is nearly good enough. */
    private static final double CHEAP_CALL = 0.06d;
    /**
     * The tightest range a bot ever credits somebody with.
     * <p>
     * Not one, and that matters in both directions: crediting an all-in with only the very best hands
     * would make the bot fold everything to any big bet, which is the other bot nobody wants to play.
     */
    private static final double MAX_RESPECT = 0.85d;

    private final Random random;
    /** Above one it wants more than the pot offers before it calls, below one it wants less. */
    private final double tightness;
    /** How often it raises a hand that is worth raising. */
    private final double aggression;
    /** How often it bets a hand that is worth nothing. */
    private final double bluff;

    /**
     * Gives one bot its temperament.
     * <p>
     * Drawn once when it sits down and kept for as long as it is there, so it is a player with a way of
     * playing rather than a coin flipped every hand. The ranges are narrow: this is the difference between
     * two people at the same table, not between a rock and a maniac.
     *
     * @param random the source of everything random about it
     */
    public BotBrain(Random random) {
        this.random = random;
        this.tightness = 0.92d + random.nextDouble() * 0.22d;
        this.aggression = 0.18d + random.nextDouble() * 0.24d;
        this.bluff = 0.03d + random.nextDouble() * 0.07d;
    }

    /**
     * Decides.
     *
     * @param table where it is sitting
     * @param me    the bot
     * @return what it does
     */
    public Action decide(PokerTable table, PokerPlayer me) {
        int toCall = table.toCall(me);
        int pot = Math.max(1, table.getPot());
        int bigBlind = table.getRules().getBigBlind();
        int stack = me.getChips();

        int opponents = 0;
        for (PokerPlayer other : table.getPlayers()) {
            if (other != me && other.isContesting()) opponents++;
        }
        if (opponents == 0) return toCall > 0 ? Action.call(toCall) : Action.check();

        List<Card> board = table.getCommunity();
        double equity = Equity.of(me.getHole(), board, opponents, random, opponentRange(table));

        if (toCall <= 0) return whenItIsFree(table, me, equity, pot, bigBlind);
        return whenItCosts(table, me, equity, toCall, pot, bigBlind, stack);
    }

    /**
     * How strong to assume the other hands are.
     * <p>
     * This is the correction that separates a bot that plays from a bot that goes broke. Left to itself,
     * {@link Equity} deals the rest of the deck out and assumes everybody else got their cards at random -
     * which is true before anybody has done anything and gets less true with every chip that goes in.
     * Somebody who has moved a hundred blinds in does not have a random hand, and against the hand they
     * actually have, top pair is not the seventy percent the simulation says it is.
     * <p>
     * Uncorrected, that is exactly how a bot talks itself into every losing call it ever makes: it reads
     * its own hand as far stronger than it is, calls the shove, and is shown aces. So the size of the bet
     * is turned into how good the opponents' cards are assumed to be, and the simulation deals them that.
     * <p>
     * Before the flop the size is measured in big blinds, because the pot is small and "twice the pot" is
     * a limp and a shove alike. Afterwards it is measured against the pot, which is what a bet means then.
     *
     * @return how tight to make them, from {@code 0} for any two cards up to {@link #MAX_RESPECT}
     */
    private double opponentRange(PokerTable table) {
        int bet = table.getCurrentBet();
        int bigBlind = table.getRules().getBigBlind();
        // the blinds are not a statement about anybody's cards
        if (bet <= bigBlind) return 0d;
        double pressure;
        if (table.getStreet() == Street.PREFLOP) {
            pressure = (bet - bigBlind) / (14d * bigBlind);
        } else {
            pressure = (double) bet / Math.max(bigBlind, table.getPot());
        }
        return Math.min(MAX_RESPECT, Math.max(0d, pressure));
    }

    /**
     * Nothing to call: check or make a bet.
     */
    private Action whenItIsFree(PokerTable table, PokerPlayer me, double equity, int pot, int bigBlind) {
        int minRaise = table.minRaiseTo(me);
        int maxRaise = table.maxRaiseTo(me);
        if (minRaise <= 0 || minRaise > maxRaise) return Action.check();

        boolean worthBetting = equity > 0.60d && random.nextDouble() < aggression * 2.2d;
        // a bluff is only worth anything where somebody can still be made to fold, and on the river
        // against four players that is nobody
        boolean worthBluffing = equity < 0.35d
                && table.getStreet() != Street.PREFLOP
                && random.nextDouble() < bluff;

        if (!worthBetting && !worthBluffing) return Action.check();

        // half to two thirds of the pot is what a bet is for: enough that calling is a real decision,
        // small enough that being wrong costs one bet rather than the stack
        double share = worthBluffing ? 0.4d : 0.45d + random.nextDouble() * 0.25d;
        int target = clamp((int) Math.round(pot * share), minRaise, maxRaise);
        return sized(table, me, target, equity, bigBlind);
    }

    /**
     * There is a price. Work out whether the hand is worth it.
     */
    private Action whenItCosts(PokerTable table, PokerPlayer me, double equity, int toCall, int pot,
                               int bigBlind, int stack) {
        // what the pot is offering: call this much to win what is already there
        double priceOfStaying = (double) toCall / (pot + toCall);
        double wanted = priceOfStaying * tightness * LOOSENESS;

        boolean shortStack = stack <= SHORT_STACK_BB * bigBlind;
        int minRaise = table.minRaiseTo(me);
        int maxRaise = table.maxRaiseTo(me);
        boolean canRaise = minRaise > 0 && minRaise <= maxRaise;

        // a hand that is winning is never thrown away, and a hand that is winning well gets raised
        if (equity >= NEVER_FOLD_ABOVE) {
            if (canRaise && (equity > 0.80d || random.nextDouble() < aggression)) {
                return sized(table, me, raiseTo(table, pot), equity, bigBlind);
            }
            return Action.call(toCall);
        }

        if (equity >= wanted) {
            // ahead of the price but not by much: mostly just pay it, sometimes put the pressure back
            if (canRaise && equity > 0.58d && random.nextDouble() < aggression * 0.5d) {
                return sized(table, me, raiseTo(table, pot), equity, bigBlind);
            }
            return Action.call(toCall);
        }

        // a stack this short is not folding its way anywhere, and a hand that is close to the price is
        // worth the shove rather than the slow bleed of the blinds
        if (shortStack && equity > 0.45d && canRaise) {
            return Action.allIn(0);
        }

        // a call that costs almost nothing is worth making on a hand that is nearly good enough, which is
        // also what stops a bot folding the big blind to a minimum raise every single orbit
        if (equity > wanted * 0.75d && toCall <= Math.max(bigBlind, stack * CHEAP_CALL)) {
            return Action.call(toCall);
        }
        return Action.fold();
    }

    /**
     * Turns a wanted size into a legal action, and refuses to turn it into an all-in on a hand that does
     * not deserve one.
     *
     * @param target what it would like to raise to
     * @param equity how often the hand wins from here
     * @return the action
     */
    private Action sized(PokerTable table, PokerPlayer me, int target, double equity, int bigBlind) {
        int minRaise = table.minRaiseTo(me);
        int maxRaise = table.maxRaiseTo(me);
        if (minRaise <= 0 || minRaise > maxRaise) {
            int toCall = table.toCall(me);
            return toCall > 0 ? Action.call(toCall) : Action.check();
        }
        target = clamp(target, minRaise, maxRaise);

        boolean wouldBeAllIn = target >= maxRaise;
        boolean shortStack = me.getChips() <= SHORT_STACK_BB * bigBlind;
        if (wouldBeAllIn && equity < NEVER_SHOVE_BELOW && !shortStack) {
            // it wanted to bet more than it can afford to lose on this hand. Betting less is a worse bet
            // than not betting, so it takes the free card or pays the price instead
            int toCall = table.toCall(me);
            if (toCall <= 0) return Action.check();
            return equity >= (double) toCall / (table.getPot() + toCall) * tightness
                    ? Action.call(toCall) : Action.fold();
        }
        // a raise past four fifths of the stack leaves a stub nobody can play with and nobody folds to, so
        // it is really an all-in. Which means it needs what an all-in needs: either the hand for it, or a
        // stack short enough that it makes no odds. Otherwise it is cut back to something survivable
        if (target > maxRaise * 0.8d) {
            if (equity >= NEVER_SHOVE_BELOW || shortStack) return Action.allIn(0);
            target = clamp((int) Math.round(maxRaise * 0.55d), minRaise, maxRaise);
            if (target >= maxRaise) {
                int toCall = table.toCall(me);
                return toCall > 0 ? Action.call(toCall) : Action.check();
            }
        }
        return table.getCurrentBet() > 0 ? Action.raise(target) : Action.bet(target);
    }

    /**
     * How much to raise to when somebody has already bet.
     * <p>
     * Two and a half to three times what is in front of them, which is what a raise is - not "the pot plus
     * what I owe", which sounds the same and is not: that makes every re-raise larger than the pot it has
     * just enlarged, and four streets of it turn a table into people playing their whole stack on second
     * pair. A raise is an answer to a bet, so it is measured against the bet.
     *
     * @param table where we are
     * @param pot   what is in the middle, for the case where the bet is tiny against it
     * @return the total to raise to
     */
    private int raiseTo(PokerTable table, int pot) {
        int bet = table.getCurrentBet();
        double factor = 2.4d + random.nextDouble() * 0.7d;
        int target = (int) Math.round(bet * factor);
        // against a token bet into a big pot, the pot is the better yardstick
        return Math.max(target, (int) Math.round(bet + pot * 0.5d));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @return how long this bot takes to decide, in milliseconds - long enough to look like somebody
     *         thinking and short enough not to hold a table up
     */
    public long thinkingTime() {
        return 700L + random.nextInt(1800);
    }
}
