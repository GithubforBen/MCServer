package de.schnorrenbergers.bedwars.game;

import java.util.List;

/**
 * How a round is cut up: how many teams play and how many players fit into one of them.
 * <p>
 * The four hypixel modes are nothing but four of these, which is what lets an event ask for two teams of
 * eight without a fifth branch anywhere in the code.
 */
public final class GameMode {

    private final String id;
    private final String displayName;
    private final int teamCount;
    private final int teamSize;

    /**
     * @param id          the key in {@code modes.yml}
     * @param displayName what players are told it is called
     * @param teamCount   how many teams play, at least two
     * @param teamSize    how many players fit into one team, at least one
     */
    public GameMode(String id, String displayName, int teamCount, int teamSize) {
        this.id = id;
        this.displayName = displayName;
        this.teamCount = Math.max(2, Math.min(TeamColor.values().length, teamCount));
        this.teamSize = Math.max(1, teamSize);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public int getTeamSize() {
        return teamSize;
    }

    /**
     * @return how many players fit into a full round
     */
    public int getMaximumPlayers() {
        return teamCount * teamSize;
    }

    /**
     * @return the colours this mode plays with, taken from the front of {@link TeamColor}
     */
    public List<TeamColor> getColors() {
        return List.of(TeamColor.values()).subList(0, teamCount);
    }

    @Override
    public String toString() {
        return id + " (" + teamCount + "x" + teamSize + ")";
    }
}
