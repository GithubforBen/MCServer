package de.schnorrenbergers.poker.game;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Fifty-two cards, shuffled.
 * <p>
 * Shuffled with {@link SecureRandom} rather than {@link Random}, and that is not superstition: the ordinary
 * generator is seeded with something guessable and its whole state can be worked out from a few outputs.
 * At a table where the cards are worth real bits, a deck somebody can predict is not a game.
 * <p>
 * A fresh deck is made for every hand rather than the same one being shuffled again, so a hand can never
 * run into a card that is still lying somewhere from the last one.
 */
public final class Deck {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final List<Card> cards = new ArrayList<>(52);
    private int next;

    public Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(cards, RANDOM);
    }

    /**
     * @return the next card off the top
     * @throws IllegalStateException if the deck is empty, which can only happen if somebody dealt to more
     *                               than twenty-two seats and should be heard about rather than papered over
     */
    public Card draw() {
        if (next >= cards.size()) {
            throw new IllegalStateException("The deck is empty - too many cards were dealt from one hand.");
        }
        return cards.get(next++);
    }

    /**
     * Burns a card, the way a live dealer does before every community card.
     * <p>
     * It changes nothing about the odds - the deck is already shuffled - and it is here because the table
     * is meant to feel like a table.
     */
    public void burn() {
        if (next < cards.size()) next++;
    }

    /**
     * @return how many cards are still in it
     */
    public int remaining() {
        return cards.size() - next;
    }
}
