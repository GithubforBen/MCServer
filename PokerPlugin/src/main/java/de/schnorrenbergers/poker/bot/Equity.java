package de.schnorrenbergers.poker.bot;

import de.schnorrenbergers.poker.game.Card;
import de.schnorrenbergers.poker.game.HandEvaluator;
import de.schnorrenbergers.poker.game.HandValue;
import de.schnorrenbergers.poker.game.Rank;
import de.schnorrenbergers.poker.game.Suit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * How often a hand wins, worked out by playing it out a few hundred times.
 * <p>
 * There is no table of hand strengths here, and that is deliberate. A table has to be right for every
 * number of opponents and every board, and one that is subtly wrong is exactly how a bot ends up shoving
 * bottom pair on a four-flush board. Dealing the rest of the deck out four hundred times answers the only
 * question that matters - "how often does this win from here" - and it answers it the same way on the flop
 * as it does before the cards are out.
 * <p>
 * It costs a few milliseconds per decision, which is affordable because a bot that decides instantly is a
 * bot everybody can feel is a bot. The thinking time was going to be spent anyway.
 */
public final class Equity {

    /** How many times a hand is played out before the flop, where the answer is coarse anyway. */
    private static final int SAMPLES_PREFLOP = 250;
    /** And afterwards, where a few percent decides whether a call is right. */
    private static final int SAMPLES_POSTFLOP = 400;
    /** The Chen score of a hand nobody would play, which is the bottom of the widest range. */
    private static final double MIN_CHEN = 0d;
    /** And of the hands that are always played, which is the top of the narrowest one. */
    private static final double MAX_CHEN = 10.5d;
    /** How often a hand is redrawn before a range is accepted as unfillable. */
    private static final int RANGE_ATTEMPTS = 40;

    private Equity() {
    }

    /**
     * How often this hand wins against that many hands drawn at random.
     *
     * @param hole      the two cards in front of somebody
     * @param board     what is on the table, nought to five cards
     * @param opponents how many other players are still in
     * @param random    the source of the deals
     * @return a share between zero and one, where a split counts as half a win because that is what it pays
     */
    public static double of(List<Card> hole, List<Card> board, int opponents, Random random) {
        return of(hole, board, opponents, random, 0d);
    }

    /**
     * How often this hand wins against opponents who are not holding just anything.
     * <p>
     * This is the difference between a bot that folds to a shove and one that calls it off with second
     * pair. Somebody who has moved a hundred blinds in does not have a random hand, and measuring against
     * a random hand is how a bot talks itself into every losing call it ever makes. So the opponents in the
     * simulation are dealt from a range instead: the harder they have bet, the better the cards they are
     * given, and the honest answer to "am I ahead" comes out the other side.
     *
     * @param hole      the two cards in front of somebody
     * @param board     what is on the table
     * @param opponents how many other players are still in
     * @param random    the source of the deals
     * @param range     how strong to assume they are, from {@code 0} for any two cards to {@code 1} for
     *                  only the hands nobody folds
     * @return a share between zero and one
     */
    public static double of(List<Card> hole, List<Card> board, int opponents, Random random, double range) {
        if (hole == null || hole.size() < 2) return 0d;
        int others = Math.max(1, opponents);
        int samples = board.isEmpty() ? SAMPLES_PREFLOP : SAMPLES_POSTFLOP;

        List<Card> unseen = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                Card card = new Card(rank, suit);
                if (hole.contains(card) || board.contains(card)) continue;
                unseen.add(card);
            }
        }

        int needed = (5 - board.size()) + others * 2;
        if (needed > unseen.size()) return 0.5d;

        // what an opponent hand has to be worth to be dealt at all, on Chen's scale
        double wanted = range <= 0d ? 0d : MIN_CHEN + range * (MAX_CHEN - MIN_CHEN);

        double score = 0d;
        List<Card> deck = new ArrayList<>(unseen);
        for (int sample = 0; sample < samples; sample++) {
            // a partial shuffle: only the cards that are actually dealt are moved, which is the same deal
            // and a fraction of the work of shuffling fifty-odd cards four hundred times
            for (int i = 0; i < needed; i++) {
                int swap = i + random.nextInt(deck.size() - i);
                Card held = deck.get(i);
                deck.set(i, deck.get(swap));
                deck.set(swap, held);
            }

            int cursor = 0;
            List<Card> fullBoard = new ArrayList<>(board);
            while (fullBoard.size() < 5) fullBoard.add(deck.get(cursor++));

            List<Card> mine = new ArrayList<>(fullBoard);
            mine.addAll(hole);
            HandValue best = HandEvaluator.evaluate(mine);

            int ties = 0;
            boolean beaten = false;
            for (int other = 0; other < others; other++) {
                Card first = deck.get(cursor++);
                Card second = deck.get(cursor++);
                if (wanted > 0) {
                    // draw again until the opponent has something somebody would really have bet this
                    // much with. Bounded, because a range this tight against a board that has eaten the
                    // aces can be a range with nothing left in it
                    int tries = 0;
                    while (chenScore(first, second) < wanted && tries++ < RANGE_ATTEMPTS) {
                        int a = cursor + random.nextInt(deck.size() - cursor);
                        int b = cursor + random.nextInt(deck.size() - cursor);
                        first = deck.get(a);
                        second = deck.get(b);
                        if (first.equals(second)) continue;
                    }
                }
                List<Card> theirs = new ArrayList<>(fullBoard);
                theirs.add(first);
                theirs.add(second);
                int comparison = HandEvaluator.evaluate(theirs).compareTo(best);
                if (comparison > 0) {
                    beaten = true;
                    break;
                }
                if (comparison == 0) ties++;
            }
            if (beaten) continue;
            score += 1d / (ties + 1);
        }
        return score / samples;
    }

    /**
     * How good two cards are before the flop, on Bill Chen's scale.
     * <p>
     * Used for one thing only: deciding whether a simulated opponent's hand is one somebody would have put
     * money in with. It is a rule of thumb from the eighties rather than a solved range, and that is fine
     * for what it is doing here - the question is "is this the kind of hand a big bet comes from", and on
     * that question it is right often enough. Aces score twenty, seven-deuce scores minus one.
     *
     * @return the score, higher is better
     */
    public static double chenScore(Card first, Card second) {
        int high = Math.max(first.value(), second.value());
        int low = Math.min(first.value(), second.value());
        double score = switch (high) {
            case 14 -> 10d;
            case 13 -> 8d;
            case 12 -> 7d;
            case 11 -> 6d;
            default -> high / 2d;
        };
        if (first.rank() == second.rank()) {
            score = Math.max(5d, score * 2d);
            return Math.ceil(score);
        }
        if (first.suit() == second.suit()) score += 2d;
        int gap = high - low - 1;
        score -= switch (gap) {
            case 0 -> 0d;
            case 1 -> 1d;
            case 2 -> 2d;
            case 3 -> 4d;
            default -> 5d;
        };
        // connected low cards make straights, which is worth something the raw ranks do not say
        if (gap <= 1 && high < 12) score += 1d;
        return Math.ceil(score);
    }
}
