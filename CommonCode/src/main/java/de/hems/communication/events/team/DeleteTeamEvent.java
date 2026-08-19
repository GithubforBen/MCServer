package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Removes a team on the launcher, together with its claims and its backpack. */
public class DeleteTeamEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4104L;

    private String teamName;

    public DeleteTeamEvent(String teamName) {
        super(ListenerAdapter.ServerName.HOST);
        this.teamName = teamName;
    }

    public DeleteTeamEvent() {
    }

    public String getTeamName() {
        return teamName;
    }
}
