package de.schnorrenbergers.poker.game;

/**
 * One card. Immutable, and there is exactly one of each in a deck.
 *
 * @param rank the rank
 * @param suit the suit
 */
public record Card(Rank rank, Suit suit) {

    /**
     * @return how it is written: rank and pip, like "A♠"
     */
    @Override
    public String toString() {
        return rank.getSymbol() + suit.getSymbol();
    }

    public int value() {
        return rank.getValue();
    }
}
