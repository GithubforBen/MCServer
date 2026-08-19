package de.schnorrenbergers.survival.featrues.team;

import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import org.bukkit.Chunk;

/**
 * Which team owns which chunk.
 * <p>
 * The claims used to sit in a config file next to this server, which meant they were lost with that file
 * and invisible to everybody else. They are now part of the team itself and therefore stored on the
 * launcher, so a claim survives a wiped server directory and is the same wherever the team is looked at.
 * <p>
 * Reads are answered from the local copy of the teams, so checking who owns a chunk - which happens on
 * every block a player touches - never goes near the network.
 */
public class ClaimManager {

    private ClaimManager() {
    }

    /**
     * @param chunk the chunk to look up
     * @return the name of the team that owns it, or {@code null} if nobody does
     */
    public static String getTeamOfChunk(Chunk chunk) {
        TeamData team = getTeamDataOfChunk(chunk);
        return team == null ? null : team.getName();
    }

    /**
     * @param chunk the chunk to look up
     * @return the team that owns it, or {@code null}
     */
    public static TeamData getTeamDataOfChunk(Chunk chunk) {
        if (chunk == null) return null;
        String key = keyOf(chunk);
        for (TeamData team : TeamService.getTeams()) {
            if (team.getClaims().contains(key)) return team;
        }
        return null;
    }

    /**
     * Adds a chunk to a team, in memory only - the caller stores the team afterwards, so claiming and
     * paying for it end up in a single write.
     *
     * @param team  the team that wants it
     * @param chunk the chunk
     * @return whether it was free
     */
    static boolean addClaim(TeamData team, Chunk chunk) {
        if (team == null || chunk == null) return false;
        if (getTeamDataOfChunk(chunk) != null) return false;
        return team.getClaims().add(keyOf(chunk));
    }

    /**
     * Removes a chunk from a team, in memory only.
     *
     * @param team  the team that owns it
     * @param chunk the chunk
     * @return whether the team owned it
     */
    static boolean removeClaim(TeamData team, Chunk chunk) {
        if (team == null || chunk == null) return false;
        return team.getClaims().remove(keyOf(chunk));
    }

    /**
     * @param team the team to count for
     * @return how many chunks it owns
     */
    public static int getTeamChunkAmount(String team) {
        TeamData data = TeamService.getTeam(team);
        return data == null ? 0 : data.getClaims().size();
    }

    /**
     * @param chunk the chunk
     * @return the key it is stored under
     */
    static String keyOf(Chunk chunk) {
        return TeamData.claimKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
}
