package de.schnorrenbergers.bedwars.stats;

/**
 * Where the numbers of a round go.
 * <p>
 * One method, on purpose. Today the only implementation writes a file next to the server, and the one
 * that matters later - the launcher, which knows every round of every server - is a second implementation
 * and not a change to anything that counts. That is the whole reason this interface exists this early:
 * the counting and the keeping must not grow into each other.
 */
public interface StatsRepository {

    /**
     * Writes one round down. Must not throw: a round that is over has to end whether or not its numbers
     * could be kept.
     *
     * @param stats what happened
     */
    void save(RoundStats stats);
}
