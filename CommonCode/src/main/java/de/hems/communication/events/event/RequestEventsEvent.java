package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every event of the network. */
public class RequestEventsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4301L;

    public RequestEventsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}
