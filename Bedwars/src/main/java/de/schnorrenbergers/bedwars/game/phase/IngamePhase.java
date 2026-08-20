package de.schnorrenbergers.bedwars.game.phase;

import de.schnorrenbergers.bedwars.api.BedwarsGameEndEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.util.Messages;

import java.util.List;

/**
 * The round itself.
 * <p>
 * What this phase owns is the question "is it over yet", asked once a second: a team whose bed and players
 * are both gone is out, and a round with one team left is decided. Beds, generators, the shop and the
 * timeline hang themselves off the same tick as they arrive.
 */
public class IngamePhase extends GamePhase {

    /** How long an empty server keeps playing before the round is called off. */
    private static final int EMPTY_SECONDS = 30;

    private int emptySeconds;

    public IngamePhase(Game game) {
        super(game);
    }

    @Override
    public PhaseType getType() {
        return PhaseType.RUNNING;
    }

    @Override
    public void onEnter() {
        Messages.broadcast("game.started", "mode", game.getMode().getDisplayName());
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 20L != 0L) return;

        for (GameTeam team : game.eliminateFinishedTeams()) {
            Messages.broadcast("team.eliminated", "team", team.getColor().getDisplayName());
        }

        if (game.getOnlineCount() == 0) {
            // not immediately: a server that everybody leaves for a moment during a restart is not over
            if (++emptySeconds >= EMPTY_SECONDS) {
                game.end(null, BedwarsGameEndEvent.Reason.EMPTY);
            }
            return;
        }
        emptySeconds = 0;

        List<GameTeam> alive = game.getAliveTeams();
        if (alive.size() == 1) {
            game.end(alive.getFirst(), BedwarsGameEndEvent.Reason.LAST_TEAM);
        } else if (alive.isEmpty()) {
            game.end(null, BedwarsGameEndEvent.Reason.LAST_TEAM);
        }
    }
}
