package de.schnorrenbergers.poker.game;

import java.util.Arrays;
import java.util.List;

/**
 * What a hand is worth, in a form two hands can be compared on.
 * <p>
 * A category and up to five numbers behind it, most significant first. Two aces with a king kicker is
 * {@code PAIR, [14, 13, ...]}, and comparing that against another pair is comparing the lists in order.
 * Everything about a hand that decides who wins is in here, and nothing that does not.
 * <p>
 * Ties are real and common - the board plays, two players hold the same pair - so this compares to exactly
 * zero rather than picking a winner by seat, which is what splits a pot correctly.
 */
public final class HandValue implements Comparable<HandValue> {

    private final HandCategory category;
    private final int[] tiebreak;
    /** The five cards that make the hand, for showing what somebody won with. */
    private final List<Card> best;

    public HandValue(HandCategory category, int[] tiebreak, List<Card> best) {
        this.category = category;
        this.tiebreak = tiebreak;
        this.best = List.copyOf(best);
    }

    public HandCategory getCategory() {
        return category;
    }

    public List<Card> getBest() {
        return best;
    }

    @Override
    public int compareTo(HandValue other) {
        int byCategory = Integer.compare(category.ordinal(), other.category.ordinal());
        if (byCategory != 0) return byCategory;
        int length = Math.min(tiebreak.length, other.tiebreak.length);
        for (int i = 0; i < length; i++) {
            int step = Integer.compare(tiebreak[i], other.tiebreak[i]);
            if (step != 0) return step;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HandValue value && compareTo(value) == 0;
    }

    @Override
    public int hashCode() {
        return category.hashCode() * 31 + Arrays.hashCode(tiebreak);
    }

    /**
     * @return what to write next to somebody's name at showdown, like "Zwei Paare, Damen und Achten"
     */
    public String describe() {
        return switch (category) {
            case HIGH_CARD -> "Höchste Karte " + name(tiebreak[0]);
            case PAIR -> "Paar " + plural(tiebreak[0]);
            case TWO_PAIR -> "Zwei Paare, " + plural(tiebreak[0]) + " und " + plural(tiebreak[1]);
            case THREE_OF_A_KIND -> "Drilling " + plural(tiebreak[0]);
            case STRAIGHT -> "Straße bis " + name(tiebreak[0]);
            case FLUSH -> "Flush, " + name(tiebreak[0]) + " hoch";
            case FULL_HOUSE -> "Full House, " + plural(tiebreak[0]) + " über " + plural(tiebreak[1]);
            case FOUR_OF_A_KIND -> "Vierling " + plural(tiebreak[0]);
            case STRAIGHT_FLUSH -> tiebreak[0] == Rank.ACE.getValue()
                    ? "Royal Flush"
                    : "Straight Flush bis " + name(tiebreak[0]);
        };
    }

    private static String name(int value) {
        return switch (value) {
            case 11 -> "Bube";
            case 12 -> "Dame";
            case 13 -> "König";
            case 14 -> "Ass";
            default -> String.valueOf(value);
        };
    }

    private static String plural(int value) {
        return switch (value) {
            case 11 -> "Buben";
            case 12 -> "Damen";
            case 13 -> "Könige";
            case 14 -> "Asse";
            default -> value + "er";
        };
    }

    @Override
    public String toString() {
        return category + Arrays.toString(tiebreak);
    }
}
