package de.schnorrenbergers.bedwars.game.phase;

import de.schnorrenbergers.bedwars.game.Game;

/**
 * One state of the round, with everything that belongs to it.
 * <p>
 * The alternative - a state field that thirty listeners branch on - is what makes minigame plugins
 * unreadable after the third feature. Here a new state is a new class, and nothing that already works has
 * to be opened to add one.
 */
public abstract class GamePhase {

    protected final Game game;

    protected GamePhase(Game game) {
        this.game = game;
    }

    /**
     * @return which state this is
     */
    public abstract PhaseType getType();

    /**
     * Runs once when the round enters this phase.
     */
    public void onEnter() {
    }

    /**
     * Runs every tick while the round is in this phase.
     *
     * @param ticks how many ticks the round has been running in total
     */
    public abstract void tick(long ticks);

    /**
     * Runs once when the round leaves this phase, also on shutdown.
     */
    public void onExit() {
    }

    public Game getGame() {
        return game;
    }
}
