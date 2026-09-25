package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/** Whether a prize was reserved for the server that asked - {@code true} only for the first one. */
public class RespondClaimAwardEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4326L;

    public RespondClaimAwardEvent(ListenerAdapter.ServerName receiver, Boolean claimed, UUID requestId) {
        super(receiver, claimed, requestId);
    }

    public RespondClaimAwardEvent() {
    }
}
