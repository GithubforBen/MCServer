package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.event.RunData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Every run the launcher knows. */
public class RespondRunsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4312L;

    public RespondRunsEvent(ListenerAdapter.ServerName receiver, ArrayList<RunData> runs, UUID requestId) {
        super(receiver, runs, requestId);
    }

    public RespondRunsEvent() {
    }
}
