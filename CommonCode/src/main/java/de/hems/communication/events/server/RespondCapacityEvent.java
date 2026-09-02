package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.server.CapacityData;

import java.io.Serializable;
import java.util.UUID;

/** What the launcher has left, and what it suggests doing about it. */
public class RespondCapacityEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4604L;

    public RespondCapacityEvent(ListenerAdapter.ServerName receiver, CapacityData capacity, UUID requestId) {
        super(receiver, capacity, requestId);
    }

    public RespondCapacityEvent() {
    }
}
