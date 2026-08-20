package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import org.bukkit.event.Event;

/**
 * The base of everything the round announces.
 * <p>
 * These are the only place addons are allowed to hook into. An addon never reaches into the game logic
 * itself, which is what makes turning one off nothing more than unregistering its listener - and it means
 * any other plugin of the network can listen in without the bedwars code knowing about it.
 */
public abstract class BedwarsEvent extends Event {

    private final Game game;

    protected BedwarsEvent(Game game) {
        this.game = game;
    }

    /**
     * @return the round this happened in
     */
    public Game getGame() {
        return game;
    }
}
