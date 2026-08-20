package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A bed is about to fall. Cancelling it leaves the bed standing, which is what a bed protecting addon or a
 * trap would do.
 */
public class BedwarsBedDestroyEvent extends BedwarsEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameTeam owner;
    private final GamePlayer breaker;
    private boolean cancelled;

    /**
     * @param game    the round
     * @param owner   whose bed it is
     * @param breaker who broke it, or {@code null} when the round itself did it at sudden death
     */
    public BedwarsBedDestroyEvent(Game game, GameTeam owner, @Nullable GamePlayer breaker) {
        super(game);
        this.owner = owner;
        this.breaker = breaker;
    }

    public GameTeam getOwner() {
        return owner;
    }

    public @Nullable GamePlayer getBreaker() {
        return breaker;
    }

    /**
     * @return whether nobody broke this bed, so it fell because the round wanted it to
     */
    public boolean isByGame() {
        return breaker == null;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
