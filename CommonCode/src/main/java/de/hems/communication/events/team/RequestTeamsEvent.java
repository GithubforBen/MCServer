package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every team of the network. */
public class RequestTeamsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4101L;

    public RequestTeamsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}
