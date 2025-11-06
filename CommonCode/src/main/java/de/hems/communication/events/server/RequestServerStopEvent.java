package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestServerStopEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 7L;
    private String serverName;

    public RequestServerStopEvent(ListenerAdapter.ServerName sender, ListenerAdapter.ServerName receiver, UUID eventId, String serverName) {
        super(sender, receiver, eventId);
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }
}
