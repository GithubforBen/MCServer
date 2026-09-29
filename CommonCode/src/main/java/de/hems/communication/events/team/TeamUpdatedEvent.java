package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.team.TeamData;

import java.io.Serializable;

/**
 * Announces that a team changed.
 * <p>
 * Sent by the launcher to the whole network after every write, so a change made on one server shows up on
 * the others without them having to poll.
 */
public class TeamUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4106L;

    private String teamName;
    /** The new state, or {@code null} when the team was deleted. */
    private TeamData team;
    /** What the team was called before, when this change is a rename - {@code null} otherwise. */
    private String previousName;

    public TeamUpdatedEvent(String teamName, TeamData team) {
        this(teamName, team, null);
    }

    /**
     * @param teamName     the team
     * @param team         its new state, or {@code null} when it was deleted
     * @param previousName what it was called before, when it was renamed
     */
    public TeamUpdatedEvent(String teamName, TeamData team, String previousName) {
        super(ListenerAdapter.ServerName.ALL);
        this.teamName = teamName;
        this.team = team;
        this.previousName = previousName;
    }

    public TeamUpdatedEvent() {
    }

    public String getTeamName() {
        return teamName;
    }

    public TeamData getTeam() {
        return team;
    }

    /**
     * Anything a server keeps under the team's name - a shop, an account - has to follow a rename, and
     * the old name being deleted and a new one appearing are two announcements that cannot be told apart
     * from a disband and a new team without this.
     *
     * @return what the team was called before, or {@code null} when this is not a rename
     */
    public String getPreviousName() {
        return previousName;
    }

    public boolean isDeleted() {
        return team == null;
    }
}
