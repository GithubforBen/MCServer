package de.hems.paper.cosmetic;

/**
 * Something that happens when its owner kills somebody.
 */
public interface KillEffect {

    /**
     * @return the id it is stored under, the same one the catalogue uses
     */
    String getId();

    /**
     * Plays it. Runs on the main thread, so anything longer than a moment schedules itself.
     * <p>
     * It has to be harmless: whatever it looks like, it may not deal damage, move anybody or change a
     * block. A cosmetic that decides a fight is not a cosmetic.
     *
     * @param context the kill
     */
    void play(KillContext context);
}
