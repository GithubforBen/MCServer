package de.hems.communication.events.round;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every self started round and for the rules they are started under. */
public class RequestRoundsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4711L;

    public RequestRoundsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}
