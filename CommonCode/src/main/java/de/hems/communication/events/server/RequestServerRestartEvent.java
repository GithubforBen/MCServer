package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestServerRestartEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 6L;
    private String serverName;

    public String getServerName() {
        return serverName;
    }

    public RequestServerRestartEvent() {
    }

    public RequestServerRestartEvent(ListenerAdapter.ServerName sender, ListenerAdapter.ServerName receiver, UUID eventId, String serverName) {
        super(sender, receiver, eventId);
        this.serverName = serverName;
    }
}
