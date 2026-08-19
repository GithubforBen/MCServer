package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.team.TeamData;

import java.io.Serializable;
import java.util.UUID;

/**
 * How a write ended. The data is the team as it is stored now, so the caller picks up the new revision.
 */
public class RespondTeamSaveEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4105L;

    private boolean successful;
    private String message;

    public RespondTeamSaveEvent(ListenerAdapter.ServerName receiver, boolean successful, String message,
                                TeamData team, UUID requestId) {
        super(receiver, team, requestId);
        this.successful = successful;
        this.message = message;
    }

    public RespondTeamSaveEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}
