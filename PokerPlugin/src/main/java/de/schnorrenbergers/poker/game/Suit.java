package de.schnorrenbergers.poker.game;

/**
 * The four suits.
 * <p>
 * No suit beats another - poker has no suit order - so this carries nothing but how a card is written and
 * painted. The colours are the ones people expect from a real deck, because a card that is the wrong
 * colour is read wrong for a second, and a second is a lot when there is money in the middle.
 * <p>
 * Deliberately free of anything from bukkit, like the rest of this package: the rules of poker are worth
 * being able to run and check without a server around them.
 */
public enum Suit {

    SPADES('♠', "Pik", false, 0x202020),
    HEARTS('♥', "Herz", true, 0xC02020),
    DIAMONDS('♦', "Karo", true, 0xC02020),
    CLUBS('♣', "Kreuz", false, 0x202020);

    private final char symbol;
    private final String german;
    private final boolean red;
    private final int rgb;

    Suit(char symbol, String german, boolean red, int rgb) {
        this.symbol = symbol;
        this.german = german;
        this.red = red;
        this.rgb = rgb;
    }

    public char getSymbol() {
        return symbol;
    }

    public String getGerman() {
        return german;
    }

    /**
     * @return whether it is one of the two red suits
     */
    public boolean isRed() {
        return red;
    }

    /**
     * @return the colour the pips are drawn in on the table
     */
    public int getRgb() {
        return rgb;
    }
}
