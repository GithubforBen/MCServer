package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Announces a server that is gone, so the proxy stops offering it as a warp target.
 */
public class ServerUnregisteredEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 111L;

    private ListenerAdapter.ServerName serverName;

    public ServerUnregisteredEvent() {
    }

    public ServerUnregisteredEvent(ListenerAdapter.ServerName receiver, ListenerAdapter.ServerName serverName) {
        super(receiver);
        this.serverName = serverName;
    }

    public ListenerAdapter.ServerName getServerName() {
        return serverName;
    }
}
