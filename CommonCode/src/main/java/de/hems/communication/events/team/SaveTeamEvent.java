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
    /** The name the team had before, when this write renames it, otherwise {@code null}. */
    private String renameFrom;

    public SaveTeamEvent(TeamData team, boolean createIfMissing) {
        this(team, createIfMissing, null);
    }

    /**
     * @param team            the team to store
     * @param createIfMissing whether it may be created
     * @param renameFrom      the name the team had before, for a rename, or {@code null}
     */
    public SaveTeamEvent(TeamData team, boolean createIfMissing, String renameFrom) {
        super(ListenerAdapter.ServerName.HOST);
        this.team = team;
        this.createIfMissing = createIfMissing;
        this.renameFrom = renameFrom;
    }

    public SaveTeamEvent() {
    }

    public TeamData getTeam() {
        return team;
    }

    public boolean isCreateIfMissing() {
        return createIfMissing;
    }

    public String getRenameFrom() {
        return renameFrom;
    }
}
