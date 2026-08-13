package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.Server;

import java.io.Serializable;

/**
 * Announces a server that just became available, so the proxy can register it and every lobby can warp to
 * it without a restart.
 */
public class ServerRegisteredEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 110L;

    private Server server;

    public ServerRegisteredEvent() {
    }

    public ServerRegisteredEvent(ListenerAdapter.ServerName receiver, Server server) {
        super(receiver);
        this.server = server;
    }

    public Server getServer() {
        return server;
    }
}
