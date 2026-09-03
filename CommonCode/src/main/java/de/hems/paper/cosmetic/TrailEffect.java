package de.hems.paper.cosmetic;

/**
 * Something that follows its owner around.
 */
public interface TrailEffect {

    /**
     * @return the id it is stored under, the same one the catalogue uses
     */
    String getId();

    /**
     * Draws one step. Called a few times a second for every wearer who moved, on the main thread, so it
     * has to be short - this is the one cosmetic that runs while the game is being played.
     *
     * @param context where its owner just walked
     */
    void draw(TrailContext context);
}
