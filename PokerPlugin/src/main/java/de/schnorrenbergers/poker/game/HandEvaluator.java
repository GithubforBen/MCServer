package de.schnorrenbergers.poker.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Works out what somebody's best five cards are worth.
 * <p>
 * Takes five to seven cards and answers with the best five that can be made from them, which is what
 * Hold'em needs: two in the hand, five on the board, and the player uses whichever five are worth most,
 * including none of their own.
 * <p>
 * It does not try every combination. Seven cards make twenty-one five card hands, and ranking all of them
 * is both slower and easier to get subtly wrong than reading the hand off the counts directly - and this
 * runs a few hundred thousand times a second when a bot is working out whether it is ahead.
 */
public final class HandEvaluator {

    private HandEvaluator() {
    }

    /**
     * @param cards five to seven cards
     * @return what the best five of them are worth
     */
    public static HandValue evaluate(List<Card> cards) {
        if (cards == null || cards.size() < 5) {
            throw new IllegalArgumentException("A hand needs at least five cards, got "
                    + (cards == null ? 0 : cards.size()));
        }

        int[] rankCounts = new int[15];
        int[] suitCounts = new int[Suit.values().length];
        for (Card card : cards) {
            rankCounts[card.value()]++;
            suitCounts[card.suit().ordinal()]++;
        }

        Suit flushSuit = null;
        for (Suit suit : Suit.values()) {
            if (suitCounts[suit.ordinal()] >= 5) {
                flushSuit = suit;
                break;
            }
        }

        // a straight flush beats everything, so it is looked for first and only inside the flushed suit -
        // five cards of one suit in a row cannot exist in two suits at once with seven cards
        if (flushSuit != null) {
            List<Card> suited = of(cards, flushSuit);
            int high = straightHigh(suited);
            if (high > 0) {
                return new HandValue(HandCategory.STRAIGHT_FLUSH, new int[]{high},
                        straightCards(suited, high));
            }
        }

        int quad = highestWithCount(rankCounts, 4);
        if (quad > 0) {
            List<Card> best = pick(cards, quad, 4);
            List<Card> kicker = highestExcluding(cards, best, 1);
            best.addAll(kicker);
            return new HandValue(HandCategory.FOUR_OF_A_KIND,
                    new int[]{quad, kicker.getFirst().value()}, best);
        }

        int trips = highestWithCount(rankCounts, 3);
        if (trips > 0) {
            // the second three of a kind counts as a pair here: with seven cards two trips are possible,
            // and the hand is a full house rather than only a set
            int pair = highestPairBelow(rankCounts, trips);
            if (pair > 0) {
                List<Card> best = pick(cards, trips, 3);
                best.addAll(pick(cards, pair, 2));
                return new HandValue(HandCategory.FULL_HOUSE, new int[]{trips, pair}, best);
            }
        }

        if (flushSuit != null) {
            List<Card> suited = of(cards, flushSuit);
            suited.sort(Comparator.comparingInt(Card::value).reversed());
            List<Card> best = new ArrayList<>(suited.subList(0, 5));
            return new HandValue(HandCategory.FLUSH, values(best), best);
        }

        int straight = straightHigh(cards);
        if (straight > 0) {
            return new HandValue(HandCategory.STRAIGHT, new int[]{straight}, straightCards(cards, straight));
        }

        if (trips > 0) {
            List<Card> best = pick(cards, trips, 3);
            List<Card> kickers = highestExcluding(cards, best, 2);
            best.addAll(kickers);
            return new HandValue(HandCategory.THREE_OF_A_KIND,
                    new int[]{trips, kickers.get(0).value(), kickers.get(1).value()}, best);
        }

        int topPair = highestWithCount(rankCounts, 2);
        if (topPair > 0) {
            int secondPair = highestPairBelow(rankCounts, topPair);
            if (secondPair > 0) {
                List<Card> best = pick(cards, topPair, 2);
                best.addAll(pick(cards, secondPair, 2));
                List<Card> kicker = highestExcluding(cards, best, 1);
                best.addAll(kicker);
                return new HandValue(HandCategory.TWO_PAIR,
                        new int[]{topPair, secondPair, kicker.getFirst().value()}, best);
            }
            List<Card> best = pick(cards, topPair, 2);
            List<Card> kickers = highestExcluding(cards, best, 3);
            best.addAll(kickers);
            return new HandValue(HandCategory.PAIR, new int[]{topPair,
                    kickers.get(0).value(), kickers.get(1).value(), kickers.get(2).value()}, best);
        }

        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(Card::value).reversed());
        List<Card> best = new ArrayList<>(sorted.subList(0, 5));
        return new HandValue(HandCategory.HIGH_CARD, values(best), best);
    }

    /**
     * The high card of the best straight in a set of cards.
     * <p>
     * The wheel - ace to five - is the one place the ace is worth one, and it is handled here by letting
     * an ace also count as a one while it is being scanned for. Doing it anywhere else would make the ace
     * ambiguous in every other comparison.
     *
     * @param cards the cards to look in
     * @return the value of the highest card of the straight, or {@code 0} when there is none
     */
    private static int straightHigh(List<Card> cards) {
        boolean[] present = new boolean[16];
        for (Card card : cards) present[card.value()] = true;
        if (present[Rank.ACE.getValue()]) present[1] = true;
        for (int high = Rank.ACE.getValue(); high >= Rank.WHEEL_HIGH; high--) {
            boolean all = true;
            for (int step = 0; step < 5; step++) {
                if (!present[high - step]) {
                    all = false;
                    break;
                }
            }
            if (all) return high;
        }
        return 0;
    }

    /**
     * @param cards the cards the straight was found in
     * @param high  its top card
     * @return the five cards that make it, one per value, highest first
     */
    private static List<Card> straightCards(List<Card> cards, int high) {
        List<Card> best = new ArrayList<>(5);
        for (int step = 0; step < 5; step++) {
            int wanted = high - step;
            // the wheel's ace sits at one in the scan but is still an ace in the deck
            int lookFor = wanted == 1 ? Rank.ACE.getValue() : wanted;
            for (Card card : cards) {
                if (card.value() == lookFor) {
                    best.add(card);
                    break;
                }
            }
        }
        return best;
    }

    /**
     * @param counts how often every rank appears
     * @param howMany the count to look for
     * @return the highest rank that appears at least that often, or {@code 0}
     */
    private static int highestWithCount(int[] counts, int howMany) {
        for (int value = Rank.ACE.getValue(); value >= 2; value--) {
            if (counts[value] >= howMany) return value;
        }
        return 0;
    }

    /**
     * @param counts how often every rank appears
     * @param above  a rank that is already used
     * @return the highest other rank that appears at least twice, or {@code 0}
     */
    private static int highestPairBelow(int[] counts, int above) {
        for (int value = Rank.ACE.getValue(); value >= 2; value--) {
            if (value != above && counts[value] >= 2) return value;
        }
        return 0;
    }

    /**
     * @param cards  everything available
     * @param value  the rank to take
     * @param howMany how many of them
     * @return that many cards of that rank
     */
    private static List<Card> pick(List<Card> cards, int value, int howMany) {
        List<Card> picked = new ArrayList<>(howMany);
        for (Card card : cards) {
            if (card.value() != value) continue;
            picked.add(card);
            if (picked.size() == howMany) break;
        }
        return picked;
    }

    /**
     * @param cards  everything available
     * @param used   what is already spoken for
     * @param howMany how many kickers are needed
     * @return the highest cards that are not already used
     */
    private static List<Card> highestExcluding(List<Card> cards, List<Card> used, int howMany) {
        List<Card> rest = new ArrayList<>(cards);
        rest.removeAll(used);
        rest.sort(Comparator.comparingInt(Card::value).reversed());
        return new ArrayList<>(rest.subList(0, Math.min(howMany, rest.size())));
    }

    /**
     * @param cards the cards of one suit to collect
     * @param suit  the suit
     * @return only the cards of that suit
     */
    private static List<Card> of(List<Card> cards, Suit suit) {
        List<Card> suited = new ArrayList<>();
        for (Card card : cards) {
            if (card.suit() == suit) suited.add(card);
        }
        return suited;
    }

    private static int[] values(List<Card> cards) {
        int[] values = new int[cards.size()];
        for (int i = 0; i < cards.size(); i++) values[i] = cards.get(i).value();
        return values;
    }
}
