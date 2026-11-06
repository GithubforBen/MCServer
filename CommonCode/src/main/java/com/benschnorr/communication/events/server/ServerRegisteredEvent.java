package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.Server;

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
