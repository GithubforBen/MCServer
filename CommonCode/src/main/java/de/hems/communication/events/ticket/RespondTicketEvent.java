package de.hems.communication.events.ticket;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/** The answer to a {@link TicketRequestEvent}: a ticket or a list of them, or why it was refused. */
public class RespondTicketEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4503L;

    private String error;

    public RespondTicketEvent(ListenerAdapter.ServerName receiver, Object data, String error, UUID requestId) {
        super(receiver, data, requestId);
        this.error = error;
    }

    public RespondTicketEvent() {
    }

    public String getError() {
        return error;
    }
}
