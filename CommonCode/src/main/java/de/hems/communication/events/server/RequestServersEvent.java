package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

public class RequestServersEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 101L;
    public RequestServersEvent() {
    }

    public RequestServersEvent(ListenerAdapter.ServerName receiver) {
        super(receiver);
    }

}
