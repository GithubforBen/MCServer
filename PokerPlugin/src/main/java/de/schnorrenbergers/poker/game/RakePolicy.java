package de.schnorrenbergers.poker.game;

/**
 * What the house keeps of a pot.
 * <p>
 * An interface rather than a number, because the rule has a shape: a percentage, a ceiling, and nothing at
 * all from a pot that was never contested. Keeping it out here also keeps the rules of poker free of the
 * rules of this particular casino.
 */
@FunctionalInterface
public interface RakePolicy {

    /** A table that takes nothing, which is what a private round between friends is. */
    RakePolicy NONE = (pot, contested) -> 0;

    /**
     * @param pot       what is in the middle
     * @param contested whether the hand was actually played out rather than won before the flop
     * @return what the house keeps
     */
    int rakeOf(int pot, boolean contested);
}
