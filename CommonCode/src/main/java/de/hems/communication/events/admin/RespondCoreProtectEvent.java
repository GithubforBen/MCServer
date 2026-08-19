package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.admin.CoreProtectEntry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * The rows a CoreProtect lookup found.
 */
public class RespondCoreProtectEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 3109L;

    /** Set when the lookup could not run at all, for example because CoreProtect is not installed. */
    private String error;

    public RespondCoreProtectEvent(ListenerAdapter.ServerName receiver, ArrayList<CoreProtectEntry> entries,
                                   String error, UUID requestId) {
        super(receiver, entries, requestId);
        this.error = error;
    }

    public RespondCoreProtectEvent() {
    }

    public String getError() {
        return error;
    }
}
