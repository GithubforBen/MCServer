package de.hems.communication.events.calendar;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.event.ScheduledEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * The answer of the host: every event that is in the calendar.
 */
public class RespondEventCalendarEvent extends RespondDataEvent implements Event, Serializable {
    private static final long serialVersionUID = 321L;

    public RespondEventCalendarEvent() {
    }

    public RespondEventCalendarEvent(ListenerAdapter.ServerName receiver, ScheduledEvent[] data, UUID requestId) {
        super(receiver, data, requestId);
    }

    public ScheduledEvent[] getData() {
        if (!(super.getData() instanceof ScheduledEvent[])) {
            throw new ClassCastException("The data of this event is not a list of scheduled events");
        }
        return (ScheduledEvent[]) super.getData();
    }
}
