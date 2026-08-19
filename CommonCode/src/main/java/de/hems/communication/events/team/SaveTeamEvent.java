package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.team.TeamData;

import java.io.Serializable;

/**
 * Writes a team on the launcher.
 * <p>
 * The team carries the revision it was read at. The launcher refuses the write if somebody else changed the
 * team meanwhile, so two servers editing the same team cannot quietly lose one of the changes.
 */
public class SaveTeamEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4103L;

    private TeamData team;
    /** Whether the team may be created if it does not exist yet. */
    private boolean createIfMissing;

    public SaveTeamEvent(TeamData team, boolean createIfMissing) {
        super(ListenerAdapter.ServerName.HOST);
        this.team = team;
        this.createIfMissing = createIfMissing;
    }

    public SaveTeamEvent() {
    }

    public TeamData getTeam() {
        return team;
    }

    public boolean isCreateIfMissing() {
        return createIfMissing;
    }
}
