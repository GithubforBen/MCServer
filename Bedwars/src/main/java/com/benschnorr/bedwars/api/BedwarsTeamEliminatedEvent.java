package com.benschnorr.bedwars.api;

import com.benschnorr.bedwars.game.Game;
import com.benschnorr.bedwars.game.GameTeam;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A team is out: its bed is gone and its last player has died.
 */
public class BedwarsTeamEliminatedEvent extends BedwarsEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameTeam team;

    public BedwarsTeamEliminatedEvent(Game game, GameTeam team) {
        super(game);
        this.team = team;
    }

    public GameTeam getTeam() {
        return team;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
