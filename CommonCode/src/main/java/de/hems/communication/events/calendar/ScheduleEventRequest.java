package de.hems.communication.events.calendar;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.event.ScheduledEvent;

import java.io.Serializable;

/**
 * Asks the host to store an event. Events that are already known are replaced, so the same request also
 * saves changes.
 */
public class ScheduleEventRequest extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 322L;

    private ScheduledEvent event;

    public ScheduleEventRequest() {
    }

    public ScheduleEventRequest(ListenerAdapter.ServerName receiver, ScheduledEvent event) {
        super(receiver);
        this.event = event;
    }

    public ScheduledEvent getEvent() {
        return event;
    }
}
