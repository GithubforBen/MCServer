package de.hems.communication.events.calendar;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Asks the host to take an event out of the calendar.
 */
public class CancelEventRequest extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 323L;

    private UUID eventId;

    public CancelEventRequest() {
    }

    public CancelEventRequest(ListenerAdapter.ServerName receiver, UUID eventId) {
        super(receiver);
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}
