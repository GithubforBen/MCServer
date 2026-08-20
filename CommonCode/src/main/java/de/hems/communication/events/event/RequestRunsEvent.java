package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every run of every event. */
public class RequestRunsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4311L;

    public RequestRunsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}
