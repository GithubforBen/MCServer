package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Fetches the shared backpack of a team from the launcher. */
public class RequestBackpackEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4107L;

    private String teamName;
    /** The size the backpack should have, worked out from how many members pay. */
    private int wantedSize;

    public RequestBackpackEvent(String teamName, int wantedSize) {
        super(ListenerAdapter.ServerName.HOST);
        this.teamName = teamName;
        this.wantedSize = wantedSize;
    }

    public RequestBackpackEvent() {
    }

    public String getTeamName() {
        return teamName;
    }

    public int getWantedSize() {
        return wantedSize;
    }
}
