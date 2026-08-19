package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * How an action or a write back ended. The data is the message shown in the browser, or {@code null} from a
 * server that was not concerned.
 */
public class RespondActionEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 3107L;

    private boolean successful;

    public RespondActionEvent(ListenerAdapter.ServerName receiver, boolean successful, String message,
                              UUID requestId) {
        super(receiver, message, requestId);
        this.successful = successful;
    }

    public RespondActionEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }
}
