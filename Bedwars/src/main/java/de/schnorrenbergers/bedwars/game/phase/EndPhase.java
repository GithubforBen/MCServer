package de.schnorrenbergers.bedwars.game.phase;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * It is decided.
 * <p>
 * The winners get a moment, then everybody goes back to the lobby and this server asks the launcher to
 * stop it. That is what makes the map reset free: the world is thrown away with the server, so nothing has
 * to be rolled back and no round can inherit the last one's craters.
 */
public class EndPhase extends GamePhase {

    private int remaining;
    private boolean sentHome;

    public EndPhase(Game game) {
        super(game);
        this.remaining = game.getSettings().getEndReturnSeconds();
    }

    @Override
    public PhaseType getType() {
        return PhaseType.ENDING;
    }

    @Override
    public void onEnter() {
        GameTeam winner = game.getWinner();
        if (winner == null) {
            Messages.broadcast("game.ended.nobody");
        } else {
            Messages.broadcast("game.ended.winner", "team", winner.getColor().getDisplayName());
        }
    }

    @Override
    public void tick(long ticks) {
        if (sentHome || ticks % 20L != 0L) return;
        if (remaining-- > 0) return;
        sentHome = true;
        sendEverybodyHome();
        stopThisServer();
    }

    /**
     * Puts everybody back onto the lobby. A player the proxy cannot move stays where they are - the server
     * stops underneath them anyway, and the proxy catches them.
     */
    private void sendEverybodyHome() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Bedwars] Could not send " + player.getName()
                        + " back to the lobby: " + e.getMessage());
            }
        }
    }

    /**
     * Asks the launcher to stop this server, off the main thread because it waits for an answer.
     */
    private void stopThisServer() {
        if (!game.getSettings().isStopServerWhenDone()) return;
        String self = ListenerAdapter.getName().toString();
        PaperContext.async(() -> {
            try {
                ServerApi.stopServer(self);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Bedwars] Could not stop " + self + ": " + e.getMessage());
            }
        });
    }

    /**
     * @return how many seconds are left before everybody is sent back
     */
    public int getRemaining() {
        return remaining;
    }
}
