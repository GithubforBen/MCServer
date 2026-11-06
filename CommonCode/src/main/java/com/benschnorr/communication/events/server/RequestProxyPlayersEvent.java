package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Asks the proxy who is connected to which server.
 * <p>
 * Every server can be asked the same thing directly, but only if the plugin on it is alive - and a server
 * whose plugin failed to enable is exactly the one nobody is on and nobody notices. The proxy hands the
 * connections out itself, so it knows the answer for every server whether or not anything on that server
 * still works.
 */
public class RequestProxyPlayersEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 1940L;

    public RequestProxyPlayersEvent() {
        super(ListenerAdapter.ServerName.VELOCITY);
    }
}
