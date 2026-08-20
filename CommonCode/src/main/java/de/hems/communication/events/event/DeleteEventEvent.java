package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/** Removes an event on the launcher. */
public class DeleteEventEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4305L;

    private UUID eventUuid;

    public DeleteEventEvent(UUID eventUuid) {
        super(ListenerAdapter.ServerName.HOST);
        this.eventUuid = eventUuid;
    }

    public DeleteEventEvent() {
    }

    public UUID getEventUuid() {
        return eventUuid;
    }
}
