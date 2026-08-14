package de.hems.communication.events.calendar;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.event.ScheduledEvent;

import java.io.Serializable;

/**
 * Tells every server that the calendar changed and what it looks like now. This is what keeps the calendar
 * the same on all servers without anybody having to ask for it.
 */
public class EventCalendarUpdatedEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 324L;

    private ScheduledEvent[] events;

    public EventCalendarUpdatedEvent() {
    }

    public EventCalendarUpdatedEvent(ListenerAdapter.ServerName receiver, ScheduledEvent[] events) {
        super(receiver);
        this.events = events;
    }

    public ScheduledEvent[] getEvents() {
        return events == null ? new ScheduledEvent[0] : events;
    }
}
