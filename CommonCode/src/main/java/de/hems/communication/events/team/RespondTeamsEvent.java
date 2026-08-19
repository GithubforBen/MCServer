package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.team.TeamData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Every team the launcher knows. */
public class RespondTeamsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4102L;

    public RespondTeamsEvent(ListenerAdapter.ServerName receiver, ArrayList<TeamData> teams, UUID requestId) {
        super(receiver, teams, requestId);
    }

    public RespondTeamsEvent() {
    }
}
