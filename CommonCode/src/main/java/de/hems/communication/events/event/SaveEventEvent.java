package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.event.EventData;

import java.io.Serializable;

/**
 * Writes an event on the launcher.
 * <p>
 * The event carries the revision it was read at, so a change made in the lobby cannot quietly overwrite one
 * made on the website a second earlier.
 */
public class SaveEventEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4303L;

    private EventData event;
    /** Whether it may be created if the launcher does not know it yet. */
    private boolean createIfMissing;

    public SaveEventEvent(EventData event, boolean createIfMissing) {
        super(ListenerAdapter.ServerName.HOST);
        this.event = event;
        this.createIfMissing = createIfMissing;
    }

    public SaveEventEvent() {
    }

    public EventData getEvent() {
        return event;
    }

    public boolean isCreateIfMissing() {
        return createIfMissing;
    }
}
