package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

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
