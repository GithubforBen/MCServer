package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestServerRestartEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 6L;
    private ListenerAdapter.ServerName serverName;

    public ListenerAdapter.ServerName getServerName() {
        return serverName;
    }

    public RequestServerRestartEvent() {
    }

    public RequestServerRestartEvent(ListenerAdapter.ServerName receiver, ListenerAdapter.ServerName serverName) {
        super(receiver);
        this.serverName = serverName;
    }
}
