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

    public TeamUpdatedEvent(String teamName, TeamData team) {
        super(ListenerAdapter.ServerName.ALL);
        this.teamName = teamName;
        this.team = team;
    }

    public TeamUpdatedEvent() {
    }

    public String getTeamName() {
        return teamName;
    }

    public TeamData getTeam() {
        return team;
    }

    public boolean isDeleted() {
        return team == null;
    }
}
