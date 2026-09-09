package de.schnorrenbergers.poker.game;

/**
 * Where a hand stands.
 */
public enum Street {

    /** Two cards each, nothing on the table. */
    PREFLOP("Preflop", 0),
    /** Three community cards. */
    FLOP("Flop", 3),
    /** A fourth. */
    TURN("Turn", 4),
    /** A fifth, and the last betting round. */
    RIVER("River", 5),
    /** Cards on their backs, pots handed out. */
    SHOWDOWN("Showdown", 5);

    private final String title;
    private final int communityCards;

    Street(String title, int communityCards) {
        this.title = title;
        this.communityCards = communityCards;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @return how many cards lie on the table during this street
     */
    public int getCommunityCards() {
        return communityCards;
    }

    /**
     * @return the street after this one, or {@code null} after the showdown
     */
    public Street next() {
        return switch (this) {
            case PREFLOP -> FLOP;
            case FLOP -> TURN;
            case TURN -> RIVER;
            case RIVER -> SHOWDOWN;
            case SHOWDOWN -> null;
        };
    }
}
