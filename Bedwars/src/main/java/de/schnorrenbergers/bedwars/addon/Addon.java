package de.schnorrenbergers.bedwars.addon;

import de.schnorrenbergers.bedwars.game.Game;

/**
 * One piece of the game that can be missing.
 * <p>
 * An addon is not allowed to reach into the round - it listens to the events in
 * {@code de.schnorrenbergers.bedwars.api} and acts on them. That rule is what makes switching one off
 * honest: there is no branch anywhere in the game logic that has to be told about it, so a round with an
 * addon off behaves exactly like a round that never had it.
 */
public interface Addon {

    /**
     * @return the key it is switched by, in configs, commands and the menu
     */
    String getId();

    /**
     * @return one line about what it does, shown in {@code /bw addons}
     */
    String getDescription();

    /**
     * @return whether it is on when nothing says otherwise
     */
    default boolean isDefaultEnabled() {
        return true;
    }

    /**
     * Starts listening.
     *
     * @param game the round it belongs to
     */
    void enable(Game game);

    /**
     * Stops listening and undoes whatever it put into the world.
     *
     * @param game the round it belonged to
     */
    void disable(Game game);
}
