package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/** How a stash write ended. The data is the message to show. */
public class RespondStashSaveEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 5005L;

    private boolean successful;
    private long revision;

    public RespondStashSaveEvent(ListenerAdapter.ServerName receiver, boolean successful, long revision,
                                 String message, UUID requestId) {
        super(receiver, message, requestId);
        this.successful = successful;
        this.revision = revision;
    }

    public RespondStashSaveEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }

    public long getRevision() {
        return revision;
    }
}
