package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/** What the launcher wrote down, or why it wrote nothing. */
public class RespondServerMemoryEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4608L;

    private boolean successful;
    private String message;

    public RespondServerMemoryEvent(ListenerAdapter.ServerName receiver, boolean successful, String message,
                                    Integer memoryMB, UUID requestId) {
        super(receiver, memoryMB, requestId);
        this.successful = successful;
        this.message = message;
    }

    public RespondServerMemoryEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}
