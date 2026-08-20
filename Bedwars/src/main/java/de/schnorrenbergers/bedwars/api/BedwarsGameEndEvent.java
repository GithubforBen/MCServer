package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GameTeam;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The round is over. Fired once, before anybody is sent anywhere, so results can still be read off the
 * teams and the players.
 */
public class BedwarsGameEndEvent extends BedwarsEvent {

    /**
     * Why the round ended.
     */
    public enum Reason {
        /** One team was left standing. */
        LAST_TEAM,
        /** The hard time limit ran out and the score decided. */
        TIME_LIMIT,
        /** Nobody was left to play. */
        EMPTY,
        /** An operator ended it. */
        STOPPED
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameTeam winner;
    private final Reason reason;

    /**
     * @param game   the round
     * @param winner who won, or {@code null} when nobody did
     * @param reason why it ended
     */
    public BedwarsGameEndEvent(Game game, @Nullable GameTeam winner, Reason reason) {
        super(game);
        this.winner = winner;
        this.reason = reason;
    }

    public @Nullable GameTeam getWinner() {
        return winner;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
