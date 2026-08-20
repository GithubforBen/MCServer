package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Somebody died. Fired after the death has been counted, so the numbers on both players are already right.
 */
public class BedwarsPlayerKillEvent extends BedwarsEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GamePlayer victim;
    private final GamePlayer killer;
    private final boolean finalKill;

    /**
     * @param game      the round
     * @param victim    who died
     * @param killer    who is credited with it, or {@code null} when nobody is
     * @param finalKill whether the victim is out for good, because their bed was gone
     */
    public BedwarsPlayerKillEvent(Game game, GamePlayer victim, @Nullable GamePlayer killer, boolean finalKill) {
        super(game);
        this.victim = victim;
        this.killer = killer;
        this.finalKill = finalKill;
    }

    public GamePlayer getVictim() {
        return victim;
    }

    public @Nullable GamePlayer getKiller() {
        return killer;
    }

    public boolean isFinalKill() {
        return finalKill;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
