package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/** How a backpack write ended. The data is the message shown to the player. */
public class RespondBackpackSaveEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4110L;

    private boolean successful;
    private long revision;

    public RespondBackpackSaveEvent(ListenerAdapter.ServerName receiver, boolean successful, long revision,
                                    String message, UUID requestId) {
        super(receiver, message, requestId);
        this.successful = successful;
        this.revision = revision;
    }

    public RespondBackpackSaveEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }

    public long getRevision() {
        return revision;
    }
}
