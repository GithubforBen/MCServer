package de.hems.communication.events.money;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every balance it keeps. */
public class RequestBalancesEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4502L;

    public RequestBalancesEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}
