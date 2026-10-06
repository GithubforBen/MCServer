package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/** The ghost server a run was given - the name of the server, or {@code null} when there was none. */
public class RespondClaimGhostRunEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4348L;

    public RespondClaimGhostRunEvent(ListenerAdapter.ServerName receiver, String serverName, UUID requestId) {
        super(receiver, serverName, requestId);
    }

    public RespondClaimGhostRunEvent() {
    }

    /**
     * @return the server the run now lives on, or {@code null} when no ghost was free
     */
    public String getServerName() {
        return getData() instanceof String name ? name : null;
    }
}
