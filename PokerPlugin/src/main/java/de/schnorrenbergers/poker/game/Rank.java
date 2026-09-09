package de.schnorrenbergers.poker.game;

/**
 * The thirteen ranks, from deuce to ace.
 * <p>
 * The value is what everything else compares on. The ace is fourteen rather than one, and the one place
 * where that is wrong - the five-high straight - is handled where straights are found rather than by giving
 * the ace two values everywhere else.
 */
public enum Rank {

    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "B"),
    QUEEN(12, "D"),
    KING(13, "K"),
    ACE(14, "A");

    /** The lowest card of a five-high straight, which is the one hand the ace plays low in. */
    public static final int WHEEL_HIGH = 5;

    private final int value;
    private final String symbol;

    Rank(int value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public int getValue() {
        return value;
    }

    /**
     * @return how it is written on the card - German court cards, because the table is German
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @param value two to fourteen
     * @return the rank with that value, or {@code null}
     */
    public static Rank ofValue(int value) {
        for (Rank rank : values()) {
            if (rank.value == value) return rank;
        }
        return null;
    }
}
