package de.schnorrenbergers.bedwars.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        even(teams, teamSize);
    }

    /**
     * Moves players off the fullest team until the teams differ by at most one.
     *
     * @param teams    the teams of the round
     * @param teamSize how many fit into one
     */
    private static void even(List<GameTeam> teams, int teamSize) {
        // bounded by the number of players: every move makes the spread smaller, and a spread of one ends it
        for (int guard = 0; guard < 128; guard++) {
            GameTeam biggest = teams.stream().max(Comparator.comparingInt(GameTeam::size)).orElse(null);
            GameTeam smallest = smallest(teams, teamSize);
            if (biggest == null || smallest == null) return;
            if (biggest.size() - smallest.size() <= 1) return;
            List<GamePlayer> members = biggest.getMembers();
            if (members.isEmpty()) return;
            smallest.add(members.getLast());
        }
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
