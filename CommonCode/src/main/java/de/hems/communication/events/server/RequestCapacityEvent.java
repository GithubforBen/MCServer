package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher how much room the machine still has. */
public class RequestCapacityEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4603L;

    public RequestCapacityEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}
