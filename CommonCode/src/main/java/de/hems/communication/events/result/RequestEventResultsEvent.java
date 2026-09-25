package de.hems.communication.events.result;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/** Asks the launcher for the result lines of one event. */
public class RequestEventResultsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4345L;

    private UUID eventUuid;

    public RequestEventResultsEvent(UUID eventUuid) {
        super(ListenerAdapter.ServerName.HOST);
        this.eventUuid = eventUuid;
    }

    public RequestEventResultsEvent() {
    }

    public UUID getEventUuid() {
        return eventUuid;
    }
}
