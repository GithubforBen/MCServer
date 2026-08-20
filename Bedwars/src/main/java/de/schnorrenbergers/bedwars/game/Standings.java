package de.schnorrenbergers.bedwars.game;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Who is doing best, by one rule.
 * <p>
 * Two places need a ranking and they must not disagree: the hard time limit, which has to name a winner
 * out of nothing but numbers, and the end screen, which shows the best players of the round. Both come
 * from here, so the board at the end explains the decision that was just made instead of contradicting it.
 */
public final class Standings {

    /**
     * What each thing a player did is worth, out of {@code timeline.yml}.
     *
     * @param bed       points for a bed the team broke
     * @param finalKill points for a kill that took somebody out for good
     * @param kill      points for an ordinary kill
     */
    public record Weights(int bed, int finalKill, int kill) {

        /**
         * @param beds   how many beds
         * @param finals how many final kills
         * @param kills  how many kills in total, final ones included
         * @return what that adds up to
         */
        public int points(int beds, int finals, int kills) {
            // final kills are counted in the kills as well, so they are worth their own bonus on top
            return beds * bed + finals * finalKill + kills * kill;
        }
    }

    /**
     * One team's line of the table.
     *
     * @param team   whose line
     * @param beds   how many beds it broke
     * @param finals how many final kills it got
     * @param kills  how many kills it got
     * @param points what that is worth
     */
    public record TeamScore(GameTeam team, int beds, int finals, int kills, int points) {
    }

    /**
     * One player's line of the table.
     *
     * @param player whose line
     * @param beds   how many beds they broke
     * @param finals how many final kills they got
     * @param kills  how many kills they got
     * @param points what that is worth
     */
    public record PlayerScore(GamePlayer player, int beds, int finals, int kills, int points) {
    }

    private Standings() {
    }

    /**
     * @param game    the round
     * @param weights what counts how much
     * @return every team that is still in the round, best first
     */
    public static List<TeamScore> rankTeams(Game game, Weights weights) {
        return game.getTeams().stream()
                .filter(team -> !team.isEmpty())
                .map(team -> score(team, weights))
                .sorted(Comparator.comparingInt(TeamScore::points).reversed()
                        .thenComparing(Comparator.comparingInt(TeamScore::beds).reversed())
                        .thenComparing(Comparator.comparingInt(TeamScore::finals).reversed()))
                .toList();
    }

    /**
     * @param game    the round
     * @param weights what counts how much
     * @return every player who took part, best first
     */
    public static List<PlayerScore> rankPlayers(Game game, Weights weights) {
        return game.getPlayers().stream()
                .filter(GamePlayer::hasTeam)
                .map(player -> new PlayerScore(player,
                        player.getBedsBroken(), player.getFinalKills(), player.getKills(),
                        weights.points(player.getBedsBroken(), player.getFinalKills(), player.getKills())))
                .sorted(Comparator.comparingInt(PlayerScore::points).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerScore::finals).reversed())
                        .thenComparing(Comparator.comparingInt(PlayerScore::kills).reversed()))
                .toList();
    }

    /**
     * Names the winner of a round that ran out of time.
     * <p>
     * A team that is out cannot win on points - it lost the round the moment its last player went down,
     * and a table that hands it the win afterwards would make the beds pointless. A tie at the top is a
     * draw on purpose: there is nothing left to break it with that is not a coin toss.
     *
     * @param game    the round
     * @param ranking the table, best first
     * @return who won, or {@code null} when nobody did
     */
    public static @Nullable GameTeam winner(Game game, List<TeamScore> ranking) {
        List<TeamScore> alive = ranking.stream().filter(score -> score.team().isAlive()).toList();
        if (alive.isEmpty()) return null;
        if (alive.size() > 1 && alive.get(0).points() == alive.get(1).points()) return null;
        return alive.getFirst().team();
    }

    /**
     * @param team    whose numbers
     * @param weights what counts how much
     * @return the team's line, which is what everybody in it did added up
     */
    private static TeamScore score(GameTeam team, Weights weights) {
        int beds = 0;
        int finals = 0;
        int kills = 0;
        for (GamePlayer member : team.getMembers()) {
            beds += member.getBedsBroken();
            finals += member.getFinalKills();
            kills += member.getKills();
        }
        return new TeamScore(team, beds, finals, kills, weights.points(beds, finals, kills));
    }
}
