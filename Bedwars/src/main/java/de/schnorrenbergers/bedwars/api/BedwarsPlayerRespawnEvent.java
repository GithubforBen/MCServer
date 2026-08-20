package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A player is coming back. Fired before they are put down, so a kit or an addon can still change where
 * they land and what they land with.
 */
public class BedwarsPlayerRespawnEvent extends BedwarsEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GamePlayer player;
    private Location location;

    public BedwarsPlayerRespawnEvent(Game game, GamePlayer player, Location location) {
        super(game);
        this.player = player;
        this.location = location;
    }

    public GamePlayer getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    /**
     * @param location where they should appear instead
     */
    public void setLocation(Location location) {
        if (location != null) this.location = location;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
