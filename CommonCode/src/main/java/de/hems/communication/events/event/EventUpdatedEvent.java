package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.event.EventData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Announces that an event changed.
 * <p>
 * Sent by the launcher to the whole network after every write, so the calendar in the lobby, the tab list on
 * survival and the website all follow along without polling.
 */
public class EventUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4306L;

    private UUID eventUuid;
    /** The new state, or {@code null} when the event was deleted. */
    private EventData event;

    public EventUpdatedEvent(UUID eventUuid, EventData event) {
        super(ListenerAdapter.ServerName.ALL);
        this.eventUuid = eventUuid;
        this.event = event;
    }

    public EventUpdatedEvent() {
    }

    public UUID getEventUuid() {
        return eventUuid;
    }

    public EventData getEvent() {
        return event;
    }

    public boolean isDeleted() {
        return event == null;
    }
}
