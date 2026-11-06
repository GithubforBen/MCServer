package com.benschnorr.bedwars.round;

import com.benschnorr.types.round.RoundState;
import com.benschnorr.bedwars.Bedwars;
import com.benschnorr.bedwars.api.BedwarsGameEndEvent;
import com.benschnorr.bedwars.api.BedwarsGameStateChangeEvent;
import com.benschnorr.bedwars.game.Game;
import com.benschnorr.bedwars.game.phase.PhaseType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Keeps the lobby's list of rounds truthful about this one.
 * <p>
 * Without this a round that has begun keeps advertising itself as joinable, and one that is over keeps
 * counting against what its owner is allowed to have open. The launcher does clean up after a server that
 * has gone away, but only a minute later and only once it is actually gone - saying so at the moment it
 * happens is the difference between a list that is right and a list that catches up.
 */
public class RoundStateListener implements Listener {

    public RoundStateListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onStateChange(BedwarsGameStateChangeEvent event) {
        if (!RoundContext.exists()) return;
        Game game = Bedwars.getInstance().getGame();
        int online = game == null ? 0 : game.getOnlineCount();
        RoundContext.report(stateOf(event.getTo()), online);
    }

    @EventHandler
    public void onEnd(BedwarsGameEndEvent event) {
        if (!RoundContext.exists()) return;
        RoundContext.report(RoundState.ENDED, 0);
    }

    /**
     * @param type a phase
     * @return the state it means to the rest of the network
     */
    public static RoundState stateOf(PhaseType type) {
        return switch (type) {
            case LOBBY -> RoundState.WAITING;
            case RUNNING -> RoundState.RUNNING;
            case ENDING -> RoundState.ENDED;
        };
    }
}
