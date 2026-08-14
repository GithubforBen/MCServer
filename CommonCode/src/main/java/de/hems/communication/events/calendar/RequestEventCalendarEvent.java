package de.hems.communication.events.calendar;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Asks the host for the whole event calendar.
 */
public class RequestEventCalendarEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 320L;

    public RequestEventCalendarEvent() {
    }

    public RequestEventCalendarEvent(ListenerAdapter.ServerName receiver) {
        super(receiver);
    }
}
