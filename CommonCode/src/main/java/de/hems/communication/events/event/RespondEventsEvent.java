package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.event.EventData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Every event the launcher knows. */
public class RespondEventsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4302L;

    public RespondEventsEvent(ListenerAdapter.ServerName receiver, ArrayList<EventData> events, UUID requestId) {
        super(receiver, events, requestId);
    }

    public RespondEventsEvent() {
    }
}
