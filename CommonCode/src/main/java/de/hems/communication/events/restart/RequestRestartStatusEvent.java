package de.hems.communication.events.restart;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher whether a restart is scheduled. Answered with {@link RespondRestartEvent}. */
public class RequestRestartStatusEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4402L;

    public RequestRestartStatusEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}
