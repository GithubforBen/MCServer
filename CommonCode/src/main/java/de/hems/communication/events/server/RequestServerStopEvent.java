package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestServerStopEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 7L;
    private ListenerAdapter.ServerName serverName;

    public RequestServerStopEvent(ListenerAdapter.ServerName receiver, ListenerAdapter.ServerName serverName) {
        super(receiver);
        this.serverName = serverName;
    }

    public ListenerAdapter.ServerName getServerName() {
        return serverName;
    }
}
