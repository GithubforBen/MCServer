package de.hems.paper.cosmetic;

/**
 * Something that happens when its owner wins.
 */
public interface WinEffect {

    /**
     * @return the id it is stored under, the same one the catalogue uses
     */
    String getId();

    /**
     * Plays it. Runs on the main thread, so anything longer than a moment schedules itself.
     *
     * @param context the round that was won
     */
    void play(WinContext context);
}
