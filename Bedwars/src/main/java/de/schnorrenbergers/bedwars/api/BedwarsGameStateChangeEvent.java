package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.phase.PhaseType;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The round moved from one phase to the next. Fired after the new phase has been entered.
 */
public class BedwarsGameStateChangeEvent extends BedwarsEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PhaseType from;
    private final PhaseType to;

    public BedwarsGameStateChangeEvent(Game game, @Nullable PhaseType from, PhaseType to) {
        super(game);
        this.from = from;
        this.to = to;
    }

    /**
     * @return the phase that was left, or {@code null} for the very first one
     */
    public @Nullable PhaseType getFrom() {
        return from;
    }

    public PhaseType getTo() {
        return to;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
