package de.schnorrenbergers.bedwars.stats;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.Standings;
import de.schnorrenbergers.bedwars.game.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What one round came to, as plain numbers.
 * <p>
 * A snapshot rather than a live view: it is taken when the round ends and does not change afterwards, so
 * whatever writes it down - a file today, the launcher later - never has to reach back into a game that is
 * already being torn apart.
 *
 * @param map     which map was played
 * @param mode    which mode
 * @param seconds how long the round lasted
 * @param winner  who won, or {@code null} when nobody did
 * @param rows    one line per player who took part, best first
 */
public record RoundStats(String map, String mode, int seconds, @Nullable TeamColor winner,
                         List<RoundStats.Row> rows) {

    /**
     * One player's round.
     *
     * @param uuid   who
     * @param name   what they were called at the time
     * @param team   which team, {@code null} for somebody who only watched
     * @param kills  kills in total, final ones included
     * @param finals kills that took somebody out for good
     * @param deaths how often they died
     * @param beds   how many beds they broke
     * @param points what that adds up to by the round's own scoring
     */
    public record Row(UUID uuid, String name, @Nullable TeamColor team, int kills, int finals,
                      int deaths, int beds, int points) {
    }

    /**
     * Takes the snapshot.
     *
     * @param game    the round that is over
     * @param seconds how long it ran
     * @param weights what a bed, a final and a kill are worth
     * @return the numbers of that round
     */
    public static RoundStats of(Game game, int seconds, Standings.Weights weights) {
        List<Row> rows = new ArrayList<>();
        for (Standings.PlayerScore score : Standings.rankPlayers(game, weights)) {
            GamePlayer player = score.player();
            rows.add(new Row(player.getUuid(), player.getName(),
                    player.getTeam() == null ? null : player.getTeam().getColor(),
                    player.getKills(), player.getFinalKills(), player.getDeaths(),
                    player.getBedsBroken(), score.points()));
        }
        return new RoundStats(
                game.getArena() == null ? "-" : game.getArena().getName(),
                game.getMode().getId(),
                Math.max(0, seconds),
                game.getWinner() == null ? null : game.getWinner().getColor(),
                List.copyOf(rows));
    }
}
