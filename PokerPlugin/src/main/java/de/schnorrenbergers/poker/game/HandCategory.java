package de.schnorrenbergers.poker.game;

/**
 * What a five card hand is, from a pair of nothing up to a straight flush.
 * <p>
 * The order of the constants is the order of the hands, so comparing two of them is comparing their
 * ordinals. Everything finer than the category is a kicker, and that lives in {@link HandValue}.
 */
public enum HandCategory {

    HIGH_CARD("Höchste Karte"),
    PAIR("Ein Paar"),
    TWO_PAIR("Zwei Paare"),
    THREE_OF_A_KIND("Drilling"),
    STRAIGHT("Straße"),
    FLUSH("Flush"),
    FULL_HOUSE("Full House"),
    FOUR_OF_A_KIND("Vierling"),
    STRAIGHT_FLUSH("Straight Flush");

    private final String german;

    HandCategory(String german) {
        this.german = german;
    }

    public String getGerman() {
        return german;
    }
}
