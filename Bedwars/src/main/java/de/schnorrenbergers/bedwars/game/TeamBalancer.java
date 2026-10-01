package de.schnorrenbergers.bedwars.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fills the teams before the round starts.
 * <p>
 * Two jobs, in this order: everybody who never picked a team gets one, and then the teams are evened out
 * until no team has more than one player over another. Somebody who picked a team keeps it as long as that
 * is fair - only the last player to join an oversized team is moved, so a group that queued together is
 * taken apart last rather than first.
 */
public final class TeamBalancer {

    private TeamBalancer() {
    }

    /**
     * Puts everybody who has not picked a team into one, and evens the teams out with them.
     * <p>
     * A pick is kept, also in a round that is not full: two friends who chose red play red together, even
     * if that makes red the bigger team. Only the players the balancer placed itself are moved to even
     * things out - with one exception: when everybody is in the same team there is nobody to play against,
     * and the round would be won before it began, so then one of them moves.
     *
     * @param game the round about to start
     */
    public static void balance(Game game) {
        List<GameTeam> teams = new ArrayList<>(game.getTeams());
        if (teams.isEmpty()) return;
        int teamSize = game.getMode().getTeamSize();

        List<GamePlayer> unassigned = new ArrayList<>();
        for (GamePlayer player : game.getPlayers()) {
            if (!player.hasTeam() && player.isOnline()) unassigned.add(player);
        }
        Collections.shuffle(unassigned);
        for (GamePlayer player : unassigned) {
            GameTeam smallest = smallest(teams, teamSize);
            if (smallest == null) break;
            smallest.add(player);
        }
        even(teams, teamSize, new HashSet<>(unassigned));
        opponent(teams, teamSize);
    }

    /**
     * Moves players the balancer placed itself off the fullest team until the teams differ by at most one,
     * or until only players who chose their team are left to move.
     *
     * @param teams    the teams of the round
     * @param teamSize how many fit into one
     * @param movable  the players who did not pick a team
     */
    private static void even(List<GameTeam> teams, int teamSize, Set<GamePlayer> movable) {
        // bounded by the number of players: every move makes the spread smaller, and a spread of one ends it
        for (int guard = 0; guard < 128; guard++) {
            GameTeam smallest = smallest(teams, teamSize);
            if (smallest == null) return;
            GameTeam from = null;
            GamePlayer moving = null;
            for (GameTeam team : teams) {
                if (team.size() - smallest.size() <= 1) continue;
                if (from != null && team.size() <= from.size()) continue;
                GamePlayer candidate = team.getMembers().stream().filter(movable::contains).reduce((a, b) -> b)
                        .orElse(null);
                if (candidate == null) continue;
                from = team;
                moving = candidate;
            }
            if (moving == null) return;
            smallest.add(moving);
        }
    }

    /**
     * When every player is in one team, one of them goes into another - a round needs two sides.
     *
     * @param teams    the teams of the round
     * @param teamSize how many fit into one
     */
    private static void opponent(List<GameTeam> teams, int teamSize) {
        List<GameTeam> occupied = teams.stream().filter(team -> !team.isEmpty()).toList();
        if (occupied.size() != 1) return;
        GameTeam only = occupied.getFirst();
        if (only.size() < 2) return;
        GameTeam other = teams.stream().filter(team -> team != only && !team.isFull(teamSize)).findFirst()
                .orElse(null);
        if (other != null) other.add(only.getMembers().getLast());
    }

    /**
     * @param teams    the teams
     * @param teamSize how many fit into one
     * @return the emptiest team that still has room, or {@code null} when they are all full
     */
    private static GameTeam smallest(List<GameTeam> teams, int teamSize) {
        return teams.stream()
                .filter(team -> !team.isFull(teamSize))
                .min(Comparator.comparingInt(GameTeam::size))
                .orElse(null);
    }
}
